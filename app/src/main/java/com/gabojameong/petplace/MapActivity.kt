package com.gabojameong.petplace

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gabojameong.petplace.databinding.ActivityMapBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var locationSource: FusedLocationSource
    private var naverMap: NaverMap? = null
    
    private val markers = mutableListOf<Marker>()
    private val addedRestaurantIds = mutableSetOf<Long>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        mapFragment.getMapAsync(this)

        binding.btnReturn.setOnClickListener { finish() }
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.btnMypage.setOnClickListener {
            val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
            val token = sharedPref.getString("jwt_token", null)
            if (token != null) {
                RetrofitClient.apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<UserProfileResponse>>,
                        response: Response<ApiResponse<UserProfileResponse>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val profile = response.body()?.data
                            val role = profile?.role?.uppercase()

                            Log.d("MainActivity", "User role: $role")

                            val intent = when (role) {
                                "CUSTOMER" -> Intent(applicationContext, CustomerMypageActivity::class.java)
                                "OWNER" -> Intent(applicationContext, OwnerMypageActivity::class.java)
                                "ADMIN" -> Intent(applicationContext, AdminMyPageActivity::class.java)
                                else -> {
                                    Toast.makeText(applicationContext, "알 수 없는 사용자 권한입니다: $role", Toast.LENGTH_SHORT).show()
                                    null
                                }
                            }

                            intent?.let {
                                // 프로필 정보를 인텐트에 담아서 전달 (중복 호출 방지)
                                it.putExtra("user_profile", profile)
                                startActivity(it)
                            }
                        } else {
                            val errorMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(applicationContext, "$errorMsg", Toast.LENGTH_SHORT).show()
                            RetrofitClient.logout()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                        Log.e("MainActivity", "Profile load failed", t)
                        Toast.makeText(applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(applicationContext, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        binding.btnLocation.setOnClickListener {
            naverMap?.let {
                it.locationTrackingMode = if (it.locationTrackingMode == LocationTrackingMode.None) {
                    binding.btnLocation.setBackgroundResource(R.drawable.bg_round_white)
                    LocationTrackingMode.Follow
                } else {
                    binding.btnLocation.setBackgroundResource(R.drawable.bg_round_gray)
                    LocationTrackingMode.None
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource

        // intent로 특정 좌표가 전달된 경우 해당 위치로 이동, 아니면 시흥 기본값
        val targetLat = intent.getDoubleExtra("target_lat", 0.0)
        val targetLng = intent.getDoubleExtra("target_lng", 0.0)
        val targetName = intent.getStringExtra("target_name")

        if (targetLat != 0.0 && targetLng != 0.0) {
            // 카메라만 이동 (마커는 fetchNearbyRestaurants가 자동으로 추가)
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(LatLng(targetLat, targetLng), 16.0))
        } else {
            val siheung = LatLng(37.3801, 126.8030)
            naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(siheung, 13.0))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            naverMap.locationTrackingMode = LocationTrackingMode.NoFollow
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }

        naverMap.addOnCameraIdleListener {
            val center = naverMap.cameraPosition.target
            Log.d("MapActivity", "카메라 이동 멈춤: ${center.latitude}, ${center.longitude}")
            fetchNearbyRestaurants(center.latitude, center.longitude)
        }

        /*
        val testRestaurant = RestaurantResponse(
            id = -1L, // 중복 방지를 위해 음수 ID 사용
            name = "테스트 애견카페",
            category = "카페",
            region = "시흥",
            address = "경기도 시흥시 시청로 20",
            latitude = 37.3801,
            longitude = 126.8030,
            phone = "010-9999-9999",
            operatingHours = null,
            allowSmall = true,
            allowMedium = true,
            allowLarge = false,
            hasFence = true,
            hasArtificialGrass = true,
            hasNaturalGrass = false,
            hasSnack = true,
            hasParking = true,
            hasRestroom = true,
            hasIndoor = true,
            hasOutdoor = true,
            imageUrls = null,
            verified = true,
            bookmarked = false
        )
        addMarker(testRestaurant)
        */
    }

    private fun fetchNearbyRestaurants(lat: Double, lng: Double) {
        val pageable = mapOf("page" to "0", "size" to "100")
        RetrofitClient.apiService.getNearbyRestaurants(lat, lng, 10.0, pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<RestaurantResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<RestaurantResponse>>>,
                    response: Response<ApiResponse<PageResponse<RestaurantResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val restaurants = response.body()?.data?.content ?: emptyList()
                        Log.d("MapActivity", "서버 데이터 로드 성공: ${restaurants.size}개")
                        restaurants.forEach { restaurant ->
                            addMarker(restaurant)
                        }
                    } else {
                        Log.e("MapActivity", "서버 응답 에러: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, t: Throwable) {
                    Log.e("MapActivity", "서버 데이터 로드 실패", t)
                    if (!isFinishing) {
                        Toast.makeText(applicationContext, "서버 연결이 원활하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun addMarker(restaurant: RestaurantResponse) {
        if (addedRestaurantIds.contains(restaurant.id)) return
        val marker = Marker()
        marker.position = LatLng(restaurant.latitude, restaurant.longitude)
        marker.captionText = restaurant.name
        marker.captionTextSize = 16f
        marker.captionColor = ContextCompat.getColor(this, R.color.black)
        marker.captionHaloColor = ContextCompat.getColor(this, R.color.white)
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.icon_map_marker)
        marker.width = 70
        marker.height = 70
        marker.anchor = PointF(0.5f, 0.5f)
        marker.isIconPerspectiveEnabled = true
        marker.onClickListener = Overlay.OnClickListener {
            Log.d("MapActivity", "마커 클릭: ${restaurant.name}")
            val intent = Intent(this, PlaceInfoActivity::class.java).apply {
                putExtra("restaurant", restaurant)
                // REORDER_TO_FRONT: 스택에 PlaceInfoActivity가 이미 있으면 앞으로 가져옴
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
            true
        }
        
        markers.add(marker)
        addedRestaurantIds.add(restaurant.id)
    }

    /*
    private fun clearMarkers() {
        markers.forEach { it.map = null }
        markers.clear()
        addedRestaurantIds.clear()
    }
    */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) {
                naverMap?.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
