package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gabojameong.petplace.databinding.FragmentSearchBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchRecentSearches()
        fetchPopularKeywords()
    }

    private fun fetchRecentSearches() {
        apiService.getRecentSearches().enqueue(object : Callback<ApiResponse<List<String>>> {
            override fun onResponse(call: Call<ApiResponse<List<String>>>, response: Response<ApiResponse<List<String>>>) {
                if (_binding == null || !isAdded) return
                
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    populateRecentLayout(apiResponse.data ?: emptyList())
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<String>>>, t: Throwable) {
                Log.e("SearchFragment", "Recent search error: ${t.message}")
            }
        })
    }

    private fun fetchPopularKeywords() {
        apiService.getPopularKeywords().enqueue(object : Callback<ApiResponse<List<String>>> {
            override fun onResponse(call: Call<ApiResponse<List<String>>>, response: Response<ApiResponse<List<String>>>) {
                if (_binding == null || !isAdded) return

                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    populateRecommendLayout(apiResponse.data ?: emptyList())
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<String>>>, t: Throwable) {
                Log.e("SearchFragment", "Popular search error: ${t.message}")
            }
        })
    }

    private fun populateRecentLayout(keywords: List<String>) {
        binding.recentLayout.removeAllViews()
        keywords.forEach { keyword ->
            val button = createSearchButton(keyword, isRecommend = false)
            binding.recentLayout.addView(button)
        }
    }

    private fun populateRecommendLayout(keywords: List<String>) {
        binding.recommendLayout.removeAllViews()
        keywords.forEach { keyword ->
            val button = createSearchButton(keyword, isRecommend = true)
            binding.recommendLayout.addView(button)
        }
    }

    private fun createSearchButton(keyword: String, isRecommend: Boolean): Button {
        return Button(requireContext()).apply {
            text = keyword
            textSize = 14f
            typeface = try { resources.getFont(R.font.nanum_gothic_bold) } catch (e: Exception) { null }
            setPadding(35, 0, 35, 0)
            includeFontPadding = false

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (10 * resources.displayMetrics.density).toInt()
                if (isRecommend) bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
            layoutParams = params

            if (isRecommend) {
                setBackgroundResource(R.drawable.edge_round_orange)
                setTextColor(ContextCompat.getColor(context, R.color.orange))
            } else {
                setBackgroundResource(R.drawable.edge_round_gray)
                setTextColor(ContextCompat.getColor(context, R.color.gray))
            }

            setOnClickListener {
                (activity as? SearchActivity)?.apply {
                    setSearchText(keyword)
                    performSearch(keyword)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
