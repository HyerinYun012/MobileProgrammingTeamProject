package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityAdminMyPageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminMyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMyPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        // 인텐트로 전달된 프로필 정보 확인 (중복 호출 방지)
        val profile = intent.getSerializableExtra("user_profile", UserProfileResponse::class.java)
        if (profile != null) {
            updateUI(profile)
        } else {
            fetchUserProfile()
        }

        binding.layoutMenuReport.setOnClickListener {
            val intent = Intent(this, ReportManageActivity::class.java)
            startActivity(intent)
        }

        binding.layoutMenuBusiness.setOnClickListener {
            val intent = Intent(this, BusinessManageActivity::class.java)
            startActivity(intent)
        }

        binding.layoutMenuInquiry.setOnClickListener {
            val intent = Intent(this, InquiryManageActivity::class.java)
            startActivity(intent)
        }
        
        binding.ivLogout.setOnClickListener {
            RetrofitClient.logout()
        }
    }

    private fun updateUI(profile: UserProfileResponse) {
        binding.tvNickname.text = profile.nickname
        
        if (!profile.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.profileImageUrl)
                .placeholder(R.drawable.icon_pfp1)
                .error(R.drawable.icon_pfp1)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    private fun fetchUserProfile() {
        RetrofitClient.apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileResponse>>,
                response: Response<ApiResponse<UserProfileResponse>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val profile = response.body()?.data
                    profile?.let { updateUI(it) }
                } else {
                    val errorMsg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(this@AdminMyPageActivity, "프로필 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                Log.e("AdminMyPage", "Profile load failed", t)
                Toast.makeText(this@AdminMyPageActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
