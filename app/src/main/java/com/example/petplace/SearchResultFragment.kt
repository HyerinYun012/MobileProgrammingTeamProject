package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.petplace.databinding.FragmentSearchResultBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

class SearchResultFragment : Fragment() {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // arguments에서 데이터 추출 시 타입 캐스팅 안정성 확보
        @Suppress("UNCHECKED_CAST")
        val results = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("results", ArrayList::class.java) as? List<RestaurantResponse>
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("results") as? List<RestaurantResponse>
        } ?: emptyList()

        val adapter = SearchResultAdapter(
            results,
            onFavoriteClick = { restaurant, callback ->
                // 즐겨찾기 토글 API 호출
                apiService.toggleBookmark(restaurant.id).enqueue(object : Callback<ApiResponse<Boolean>> {
                    override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                        val apiResponse = response.body()
                        if (response.isSuccessful && apiResponse?.success == true) {
                            val isNowBookmarked = apiResponse.data ?: false
                            callback(isNowBookmarked)
                            val message = if (isNowBookmarked) "북마크에 추가되었습니다." else "북마크가 취소되었습니다."
                            Toast.makeText(requireContext().applicationContext, message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext().applicationContext, "요청 실패: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                        Toast.makeText(requireContext().applicationContext, "네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            onItemClick = { restaurant ->
                val intent = Intent(requireContext(), PlaceInfoActivity::class.java).apply {
                    putExtra("restaurant", restaurant as Serializable)
                }
                startActivity(intent)
            }
        )

        binding.rvSearchResult.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSearchResult.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
