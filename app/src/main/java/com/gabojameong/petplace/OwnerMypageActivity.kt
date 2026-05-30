package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityOwnerMypageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerMypageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOwnerMypageBinding

    /** 내 업장 ID — 리뷰관리·공지관리·업장관리에 필요 */
    private var myRestaurantId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOwnerMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profile = intent.getSerializableExtra("user_profile", UserProfileResponse::class.java)
        if (profile != null) updateUI(profile) else fetchUserProfile()

        // 업장 ID 로드 (리뷰·공지관리 버튼에 필요)
        fetchMyRestaurantId()

        binding.imageView7.setOnClickListener { finish() }

        binding.imageView9.setOnClickListener {
            startActivity(Intent(this, MypageEditActivity::class.java)
                .putExtra("user_profile", profile))
        }

        binding.button4.setOnClickListener {
            startActivity(Intent(this, StoreManageActivity::class.java)
                .apply { if (myRestaurantId != -1L) putExtra("RESTAURANT_ID", myRestaurantId) })
        }

        binding.button6.setOnClickListener {
            if (myRestaurantId == -1L) {
                Toast.makeText(this, "업장 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, OwnerReviewManageActivity::class.java)
                .putExtra("RESTAURANT_ID", myRestaurantId))
        }

        binding.button2.setOnClickListener {
            startActivity(Intent(this, MenuManageActivity::class.java)
                .apply { if (myRestaurantId != -1L) putExtra("restaurantId", myRestaurantId) })
        }

        binding.button9.setOnClickListener {
            if (myRestaurantId == -1L) {
                Toast.makeText(this, "업장 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, NoticeManageActivity::class.java)
                .putExtra("RESTAURANT_ID", myRestaurantId))
        }

        binding.button5.setOnClickListener {
            startActivity(Intent(this, InquiryListActivity::class.java))
        }

        binding.ivLogout.setOnClickListener { RetrofitClient.logout() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchMyRestaurantId() {
        RetrofitClient.apiService.getMyRestaurants()
            .enqueue(object : Callback<ApiResponse<List<OwnerRestaurantSummary>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<OwnerRestaurantSummary>>>,
                    response: Response<ApiResponse<List<OwnerRestaurantSummary>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val first = response.body()?.data?.firstOrNull()
                        if (first != null) {
                            myRestaurantId = first.id
                            Log.d("OwnerMypage", "업장 ID 로드 완료: $myRestaurantId (${first.name})")
                        } else {
                            Log.w("OwnerMypage", "등록된 업장 없음")
                        }
                    } else {
                        Log.e("OwnerMypage", "업장 목록 로드 실패: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, t: Throwable) {
                    Log.e("OwnerMypage", "업장 목록 네트워크 오류", t)
                }
            })
    }

    private fun updateUI(profile: UserProfileResponse) {
        binding.textView4.text = profile.nickname
        if (!profile.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(profile.profileImageUrl)
                .placeholder(R.drawable.union).error(R.drawable.union)
                .circleCrop().into(binding.imageView6)
        }
    }

    private fun fetchUserProfile() {
        RetrofitClient.apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileResponse>>, response: Response<ApiResponse<UserProfileResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { updateUI(it) }
                } else {
                    Toast.makeText(this@OwnerMypageActivity, "프로필 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                Log.e("OwnerMypage", "Profile load failed", t)
            }
        })
    }
}
