package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityTempTestBinding
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TempTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTempTestBinding
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTempTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 리뷰 읽기 화면 테스트 (임시 식당 ID 1번 전달)
        binding.btnTestReviewRead.setOnClickListener {
            val intent = Intent(this, ReviewReadActivity::class.java).apply {
                putExtra("RESTAURANT_ID", 1L)
            }
            startActivity(intent)
        }

        // 2. 사장님 리뷰 관리 테스트
        binding.btnTestOwner.setOnClickListener {
            val intent = Intent(this, OwnerReviewManageActivity::class.java).apply {
                putExtra("RESTAURANT_ID", 1L)
            }
            startActivity(intent)
        }

        // 3. 내 리뷰 관리 테스트
        binding.btnTestCustomer.setOnClickListener {
            val intent = Intent(this, MyReviewActivity::class.java)
            startActivity(intent)
        }

        // 4. 관리자 마이페이지 테스트
        binding.btnTestAdmin.setOnClickListener {
            val intent = Intent(this, AdminMyPageActivity::class.java)
            startActivity(intent)
        }

        // 5. 서버에 테스트용 사업자(식당) 데이터 추가
        binding.btnTestAddBiz.setOnClickListener {
            val randomNum = (100..999).random()
            val request = RestaurantRequest(
                name = "테스트 가게 $randomNum",
                address = "시흥시 정왕동 $randomNum",
                phone = "010-1234-$randomNum",
                businessNo = "123-45-$randomNum",
                category = "CAFE",
                region = "시흥",
                latitude = 37.3801,
                longitude = 126.8030
            )

            val json = gson.toJson(request)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

            apiService.registerRestaurant(requestBody, null).enqueue(object : Callback<ApiResponse<Long>> {
                override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                    if (response.isSuccessful) {
                        showCustomDialog("서버에 사업자 데이터가 추가되었습니다.\nID: ${response.body()?.data}")
                    } else {
                        Toast.makeText(this@TempTestActivity, "추가 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                    Toast.makeText(this@TempTestActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 6. 서버에 테스트용 1:1 문의 데이터 추가
        binding.btnTestAddInquiry.setOnClickListener {
            val categories = listOf("일반 문의", "오류 문의", "신고 문의")
            val request = InquiryRequest(
                category = categories.random(),
                email = "user_test@naver.com",
                content = "ApiService를 통한 1:1 문의 서버 전송 테스트입니다."
            )

            apiService.submitInquiry(request).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        showCustomDialog("서버에 문의 데이터가 성공적으로 전송되었습니다.")
                    } else {
                        Toast.makeText(this@TempTestActivity, "전송 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@TempTestActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 7. 업장 등록/관리 화면 이동
        binding.btnTestStoreManage.setOnClickListener {
            val intent = Intent(this, StoreManageActivity::class.java)
            startActivity(intent)
        }
    }
}