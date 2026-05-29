package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityCustomerMypageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerMypageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCustomerMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 인텐트로 전달된 프로필 정보 확인 (중복 호출 방지)
        val profile = intent.getSerializableExtra("user_profile", UserProfileResponse::class.java)
        if (profile != null) {
            updateUI(profile)
        } else {
            fetchUserProfile()
        }

        binding.imageView7.setOnClickListener {
            finish()
        }
        binding.button3.setOnClickListener {
            // 관심목록으로 이동
        }
        binding.button4.setOnClickListener {
            // 최근 본 가게로 이동
        }
        binding.button6.setOnClickListener {
            // 내 리뷰로 이동
        }
        binding.imageView9.setOnClickListener {
            val intent = Intent(this, MypageEditActivity::class.java)
            intent.putExtra("user_profile", profile)
            startActivity(intent)
        }
        binding.ivLogout.setOnClickListener {
            RetrofitClient.logout()
        }
//        binding.petButton.setOnClickListener {
//            // 펫 등록으로 이동
//        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUI(profile: UserProfileResponse) {
        binding.textView4.text = profile.nickname
        
        if (!profile.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.profileImageUrl)
                .placeholder(R.drawable.union)
                .error(R.drawable.union)
                .circleCrop()
                .into(binding.imageView6)
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
                    Toast.makeText(applicationContext, "프로필 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                Log.e("CustomerMypage", "Profile load failed", t)
                Toast.makeText(applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
