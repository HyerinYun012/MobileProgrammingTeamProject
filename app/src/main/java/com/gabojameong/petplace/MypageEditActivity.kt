package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityMypageEditBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MypageEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMypageEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMypageEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageView7.setOnClickListener {
            finish()
        }

        val profile = intent.getSerializableExtra("user_profile", UserProfileResponse::class.java)
        if (profile != null) {
            updateUI(profile)
        } else {
            fetchUserProfile()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUI(profile: UserProfileResponse) {
        // activity_owner_mypage_edit.xml uses phoneEditText for the name/nickname field
        binding.phoneEditText.setText(profile.nickname)
        
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
                    Toast.makeText(this@MypageEditActivity, "프로필 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                Log.e("OwnerMypage", "Profile load failed", t)
                Toast.makeText(this@MypageEditActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
