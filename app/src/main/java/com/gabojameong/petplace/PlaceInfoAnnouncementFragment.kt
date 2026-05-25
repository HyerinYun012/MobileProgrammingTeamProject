package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gabojameong.petplace.databinding.FragmentPlaceInfoAnnouncementBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceInfoAnnouncementFragment : Fragment() {
    private var _binding: FragmentPlaceInfoAnnouncementBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceInfoAnnouncementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val restaurant = arguments?.getSerializable("restaurant", RestaurantResponse::class.java)
        restaurant?.let {
            fetchAnnouncements(it.id)
        }
    }

    private fun fetchAnnouncements(restaurantId: Long) {
        val pageable = mapOf("page" to "0", "size" to "10")
        
        apiService.getNotices(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<NoticeResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<NoticeResponse>>>,
                response: Response<ApiResponse<PageResponse<NoticeResponse>>>
            ) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    val noticeList = apiResponse.data?.content ?: emptyList()
                    val announcementDataList = noticeList.map { notice ->
                        AnnouncementData(
                            title = notice.title,
                            contents = notice.content,
                            postTime = notice.createdAt//,
                            //fullImageUrl = notice.imageUrl, // 상세 이미지 추가
                           // previewUrl = notice.thumbnailUrl // 썸네일 이미지 추가
                        )
                    }
                    setupRecyclerView(announcementDataList)
                } else {
                    Toast.makeText(requireContext().applicationContext, "공지사항 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("AnnouncementFragment", "Notice load failed: ${apiResponse?.message}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, t: Throwable) {
                Log.e("AnnouncementFragment", "Notice load error: ${t.message}")
            }
        })
    }

    private fun setupRecyclerView(dataList: List<AnnouncementData>) {
        val adapter = AnnouncementAdapter(dataList)
        binding.rvAnnouncement.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
