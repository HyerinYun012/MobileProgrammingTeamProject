package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gabojameong.petplace.databinding.FragmentPlaceInfoMenuBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceInfoMenuFragment : Fragment() {
    private var _binding: FragmentPlaceInfoMenuBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceInfoMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val restaurant = arguments?.getSerializable("restaurant", RestaurantResponse::class.java)
        restaurant?.let {
            fetchMenuData(it.id)
        }
    }

    private fun fetchMenuData(restaurantId: Long) {
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "10"
        )

        apiService.getMenus(restaurantId, pageableMap).enqueue(object : Callback<ApiResponse<PageResponse<MenuResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MenuResponse>>>,
                response: Response<ApiResponse<PageResponse<MenuResponse>>>
            ) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    val menuList = apiResponse.data?.content ?: emptyList()
                    val adapterData = menuList.map { MenuData(id = it.id, name = it.name, price = it.price, imageUrl = it.imageUrl, desc = it.description) }
                    setupRecyclerView(adapterData)
                } else {
                    Toast.makeText(requireContext(), "메뉴 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("PlaceInfoMenu", "Menu load failed: ${apiResponse?.message}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MenuResponse>>>, t: Throwable) {
                Log.e("PlaceInfoMenu", "Menu load error: ${t.message}")
            }
        })
    }

    private fun setupRecyclerView(menuList: List<MenuData>) {
        val adapter = MenuAdapter(menuList)
        binding.rvMenu.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
