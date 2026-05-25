package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.gabojameong.petplace.databinding.ActivitySearchBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySearchBinding.inflate(layoutInflater)
    }
    private val apiService = RetrofitClient.apiService

    // 필터 상태 저장
    private var selectedRegions: ArrayList<String>? = null
    private var selectedConveniences: ArrayList<String>? = null
    private var selectedPets: ArrayList<String>? = null

    private fun insertSortButtonInteraction(button: Button, requestKey: String, key:String, defaultText:Int){
        supportFragmentManager.setFragmentResultListener(requestKey, this) {
                _, bundle ->
            val data = bundle.getStringArrayList(key)

            when (requestKey) {
                "region_selection" -> selectedRegions = data
                "convenience_selection" -> selectedConveniences = data
                "pet_selection" -> selectedPets = data
            }

            if (!data.isNullOrEmpty()) {
                button.text = if (data.size > 1) "${data[0]} 외 ${data.size - 1}" else data[0]
                button.setBackgroundResource(R.drawable.bg_round_orange)
                button.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                button.setText(defaultText)
                button.setBackgroundResource(R.drawable.edge_round_orange)
                button.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SearchFragment())
                .commit()
        }

        insertSortButtonInteraction(binding.btnSortRegion, "region_selection", "selected_regions", R.string.region_all)
        insertSortButtonInteraction(binding.btnSortConvenience,"convenience_selection", "selected_conveniences", R.string.convenience)
        insertSortButtonInteraction(binding.btnSortPet,"pet_selection", "selected_pets", R.string.pet)

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSortRegion.setOnClickListener {
            RegionBottomSheet().show(supportFragmentManager, "RegionBottomSheet")
        }
        binding.btnSortConvenience.setOnClickListener {
            ConvenienceBottomSheet().show(supportFragmentManager, "ConvenienceBottomSheet")
        }
        binding.btnSortPet.setOnClickListener {
            PetBottomSheet().show(supportFragmentManager, "PetBottomSheet")
        }
        binding.btnSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            performSearch(query)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun setSearchText(text: String) {
        binding.editTextSearch.setText(text)
    }

    private fun hasAnyFilter(): Boolean {
        return !selectedRegions.isNullOrEmpty() || !selectedConveniences.isNullOrEmpty() || !selectedPets.isNullOrEmpty()
    }

    private fun mapRegionToEnum(region: String): String {
        return when {
            region.contains("거북섬") -> "GEOBUKSEOM"
            region.contains("과림") -> "GWARIM"
            region.contains("군자") -> "GUNJA"
            region.contains("능곡") -> "NEUNGGOK"
            region.contains("대야") -> "DAEYA"

            region.contains("매화") -> "MAEHWA"
            region.contains("목감") -> "MOKGAM"
            region.contains("배곧") -> "BAEGON"
            region.contains("신천") -> "SINCHEON"
            region.contains("신현") -> "SINHYEON"

            region.contains("연성") -> "YEONSEONG"
            region.contains("월곶") -> "WOLGOT"
            region.contains("은행") -> "EUNHAENG"
            region.contains("장곡") -> "JANGGOK"
            region.contains("정왕") -> "JEONGWANG"
            else -> region
        }
    }

    fun performSearch(query: String) {
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "10"
        )

        // 필터 로직 구성
        val call = if (hasAnyFilter()) {
            val filterMap = mutableMapOf<String, Any>()
            if (query.isNotEmpty()) filterMap["keyword"] = query

            // 15개 지역 Enum 전체 매핑 적용 - 리스트로 전달
            selectedRegions?.let { regions ->
                val mappedRegions = regions.map { mapRegionToEnum(it) }
                if (mappedRegions.isNotEmpty()) {
                    // List를 그대로 Map에 넣으면 [VALUE] 형태로 인코딩되므로 쉼표로 연결된 문자열로 변환
                    filterMap["regions"] = mappedRegions.joinToString(",")
                }
            }

            // 견종/시설 매핑
            selectedPets?.forEach {
                if (it.contains("소형")) filterMap["allowSmall"] = "true"
                if (it.contains("중형")) filterMap["allowMedium"] = "true"
                if (it.contains("대형")) filterMap["allowLarge"] = "true"
            }
            selectedConveniences?.forEach {
                if (it.contains("울타리")) filterMap["hasFence"] = "true"
                if (it.contains("주차")) filterMap["hasParking"] = "true"
                if (it.contains("실내")) filterMap["hasIndoor"] = "true"
                if (it.contains("실외")) filterMap["hasOutdoor"] = "true"
                if (it.contains("인조잔디")) filterMap["hasArtificialGrass"] = "true"
                if (it.contains("천연잔디")) filterMap["hasNaturalGrass"] = "true"
                if (it.contains("간식")) filterMap["hasSnack"] = "true"
                if (it.contains("화장실")) filterMap["hasRestroom"] = "true"
            }
            Log.d("SearchActivity", "Filter Map: $filterMap, Pageable Map: $pageableMap")
            apiService.filterRestaurants(filterMap, pageableMap)
        } else {
            if (query.isEmpty()) {
                Toast.makeText(applicationContext, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            apiService.search(query, pageableMap)
        }

        // Request URL 로그 추가
        Log.d("SearchActivity", "Request URL: ${call.request().url}")

        call.enqueue(object : Callback<ApiResponse<PageResponse<RestaurantResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, response: Response<ApiResponse<PageResponse<RestaurantResponse>>>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    val searchResults = response.body()?.data?.content ?: emptyList()
                    if (searchResults.isEmpty()) {
                        Toast.makeText(applicationContext, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("SearchActivity", "Search Results: $searchResults")
                        val fragment = SearchResultFragment().apply {
                            arguments = Bundle().apply { putSerializable("results", ArrayList(searchResults)) }
                        }
                        if (!supportFragmentManager.isStateSaved) {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, fragment)
                                .addToBackStack(null)
                                .commitAllowingStateLoss()
                        }
                    }
                } else {
                    Log.e("SearchActivity", "API Error: ${response.code()}")
                    Toast.makeText(applicationContext, "검색 중 오류가 발생했습니다 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Log.e("SearchActivity", "Network Error: ${t.message}")
                Toast.makeText(applicationContext, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
