package com.example.petplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.petplace.databinding.FragmentPlaceInfoAnnouncementBinding
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
        } ?: loadMockData()
    }

    private fun fetchAnnouncements(restaurantId: Long) {
        // 7-7: 명세의 pageable (Object) 대응을 위한 QueryMap 구성
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
                            postTime = notice.createdAt
                        )
                    }
                    setupRecyclerView(announcementDataList)
                } else {
                    loadMockData()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, t: Throwable) {
                Log.e("AnnouncementFragment", "Notice load error: ${t.message}")
                loadMockData()
            }
        })
    }

    private fun setupRecyclerView(dataList: List<AnnouncementData>) {
        val adapter = AnnouncementAdapter(dataList)
        binding.rvAnnouncement.adapter = adapter
    }

    private fun loadMockData() {
        val mockAnnouncementList = listOf(
            AnnouncementData(
                title = "3월 방문 감사 이벤트 안내",
                contents = "안녕하세요~! 항상 찾아주시는 손님 여러분들께 감사의 의미로 이벤트를 준비했습니다. 아래 사진을 참고해주세요 감사합니다^^",
                postTime = "5일전"
            ),
            AnnouncementData(
                title = "신메뉴 출시 기념 할인 쿠폰 증정",
                contents = "카페 펫플레이스에서 신메뉴가 출시되었습니다! 지금 방문하시면 할인 쿠폰을 드려요.",
                postTime = "2일전"
            ),
            AnnouncementData(
                title = "반려견 동반 에티켓 안내 캠페인",
                contents = "우리 모두를 위한 펫티켓! 실내 이용 시 주의사항을 꼭 확인해 주세요.",
                postTime = "1일전"
            ),
            AnnouncementData(
                title = "주말 영업 시간 연장 알림",
                contents = "이번 주부터 토/일요일은 저녁 10시까지 영업합니다.",
                postTime = "방금 전"
            )
        )
        setupRecyclerView(mockAnnouncementList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
