package com.example.petplace

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.example.petplace.databinding.ActivitySearchBinding
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

    fun performSearch(query: String) {
        // 7-7: 명세의 pageable (Object) 대응을 위한 QueryMap 구성
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "10"
        )

        // 필터 로직 구성
        val call = if (hasAnyFilter()) {
            val filterMap = mutableMapOf<String, String>()
            if (query.isNotEmpty()) filterMap["keyword"] = query

            // 7-7: 15개 지역 Enum 전체 매핑 적용
            selectedRegions?.firstOrNull()?.let {
                filterMap["region"] = when {
                    it.contains("거북섬") -> "GEOBUKSEOM"
                    it.contains("과림") -> "GWARIM"
                    it.contains("군자") -> "GUNJA"
                    it.contains("능곡") -> "NEUNGGOK"
                    it.contains("대야") -> "DAEYA"
                    it.contains("매화") -> "MAEHWA"
                    it.contains("목감") -> "MOKGAM"
                    it.contains("배곧") -> "BAEGON"
                    it.contains("신천") -> "SINCHEON"
                    it.contains("신현") -> "SINHYEON"
                    it.contains("연성") -> "YEONSEONG"
                    it.contains("월곶") -> "WOLGOT"
                    it.contains("은행") -> "EUNHAENG"
                    it.contains("장곡") -> "JANGGOK"
                    it.contains("정왕") -> "JEONGWANG"
                    else -> it
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

            apiService.filterRestaurants(filterMap, pageableMap)
        } else {
            if (query.isEmpty()) {
                Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            apiService.search(query, pageableMap)
        }

        call.enqueue(object : Callback<ApiResponse<PageResponse<RestaurantResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, response: Response<ApiResponse<PageResponse<RestaurantResponse>>>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    val searchResults = response.body()?.data?.content ?: emptyList()
                    if (searchResults.isEmpty()) {
                        Toast.makeText(applicationContext, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
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
                    Toast.makeText(applicationContext, "검색 중 오류가 발생했습니다 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(applicationContext, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
