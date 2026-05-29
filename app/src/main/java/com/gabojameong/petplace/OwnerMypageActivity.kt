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
import com.gabojameong.petplace.databinding.ActivityOwnerMypageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerMypageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOwnerMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOwnerMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profile = intent.getSerializableExtra("user_profile", UserProfileResponse::class.java)
        if (profile != null) {
            updateUI(profile)
        } else {
            fetchUserProfile()
        }

        binding.imageView7.setOnClickListener {
            finish()
        }
        binding.imageView9.setOnClickListener {
            val intent = Intent(this, MypageEditActivity::class.java)
            intent.putExtra("user_profile", profile)
            startActivity(intent)
        }
        binding.button4.setOnClickListener {
            //업장관리 액티비티로 이동 (storemanageactivity
            val intent = Intent(this, StoreManageActivity::class.java)
            startActivity(intent)
        }
        binding.button6.setOnClickListener{
            val intent = Intent(this, OwnerReviewManageActivity::class.java)
            startActivity(intent)
            //리뷰관리 액티비티로 이동 (OwnerReviewManageActivity)
        }
        binding.button2.setOnClickListener {
            val intent = Intent(this, MenuManageActivity::class.java)
            startActivity(intent)
            //메뉴관리 액티비티로 이동
        }
        binding.button9.setOnClickListener {
            val intent = Intent(this, NoticeManageActivity::class.java)
            startActivity(intent)
        }
        binding.button5.setOnClickListener {
            startActivity(Intent(this, InquiryListActivity::class.java))
        }
        binding.ivLogout.setOnClickListener {
            RetrofitClient.logout()
        }

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
                    Toast.makeText(this@OwnerMypageActivity, "프로필 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                Log.e("OwnerMypage", "Profile load failed", t)
                Toast.makeText(this@OwnerMypageActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
