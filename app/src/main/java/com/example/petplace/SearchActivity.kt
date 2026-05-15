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
import androidx.fragment.app.Fragment
import com.example.petplace.databinding.ActivitySearchBinding

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySearchBinding.inflate(layoutInflater)
    }

    private val apiService = RetrofitClient.apiService

    private fun insertSortButtonInteraction(button: Button, requestKey: String, key:String, defaultText:Int){
        supportFragmentManager.setFragmentResultListener(requestKey, this) {
                _, bundle ->
            val selectedRegions = bundle.getStringArrayList(key)

            if (!selectedRegions.isNullOrEmpty()) {
                button.text = if (selectedRegions.size > 1) {
                    "${selectedRegions[0]} 외 ${selectedRegions.size - 1}"
                } else {
                    selectedRegions[0]
                }
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

        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.btnSortRegion.setOnClickListener {
            val bottomSheet = RegionBottomSheet()
            bottomSheet.show(supportFragmentManager, "RegionBottomSheet")
        }
        binding.btnSortConvenience.setOnClickListener {
            val bottomSheet = ConvenienceBottomSheet()
            bottomSheet.show(supportFragmentManager, "ConvenienceBottomSheet")
        }
        binding.btnSortPet.setOnClickListener {
            val bottomSheet = PetBottomSheet()
            bottomSheet.show(supportFragmentManager, "PetBottomSheet")
        }
        binding.btnSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun performSearch(query: String) {
        apiService.search(keyword = query, userId = "1").enqueue(object : Callback<List<SearchResponse>> {
            override fun onResponse(call: Call<List<SearchResponse>>, response: Response<List<SearchResponse>>) {
                if (response.isSuccessful) {
                    val searchResults = response.body()
                    Log.d("SearchActivity", "검색 성공: $searchResults")
                    Toast.makeText(this@SearchActivity, "검색 성공: ${searchResults?.size}건", Toast.LENGTH_SHORT).show()
                    // TODO: 검색 결과를 리스트로 보여주는 UI 업데이트 로직을 추가하세요.
                } else {
                    Log.e("SearchActivity", "검색 실패: ${response.code()}")
                    Toast.makeText(this@SearchActivity, "검색에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<SearchResponse>>, t: Throwable) {
                Log.e("SearchActivity", "네트워크 에러: ${t.message}")
                Toast.makeText(this@SearchActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
