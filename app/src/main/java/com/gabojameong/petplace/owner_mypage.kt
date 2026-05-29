package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class owner_mypage : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var btnEditProfile: ImageView
    private lateinit var btnInquiry: Button

    private var userType: String = "OWNER"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userType = intent.getStringExtra("USER_TYPE") ?: "OWNER"

        if (userType == "OWNER" || userType == "BOSS") {
            setContentView(R.layout.activity_owner_mypage)
        } else {
            setContentView(R.layout.activity_customer_mypage)
        }

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        fetchUserInfoFromServer()
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.imageView6)
        tvName = findViewById(R.id.textView4)
        btnEditProfile = findViewById(R.id.imageView9)
        btnInquiry = findViewById(R.id.button5)
    }

    private fun setupListeners() {
        val goToEditProfile = {
            val intent = Intent(this, owner_mypage_edit::class.java)
            intent.putExtra("USER_TYPE", userType)
            startActivity(intent)
        }
        btnEditProfile.setOnClickListener { goToEditProfile() }
        ivProfile.setOnClickListener { goToEditProfile() }
        tvName.setOnClickListener { goToEditProfile() }
        findViewById<TextView>(R.id.textView5)?.setOnClickListener { goToEditProfile() }
        findViewById<ImageView>(R.id.imageView7)?.setOnClickListener { finish() }

        btnInquiry.setOnClickListener {
            startActivity(Intent(this, activity_inquirelist::class.java))
        }
    }

    private fun fetchUserInfoFromServer() {
        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        val cachedNickname = sharedPref.getString("nickname", null)
        if (!cachedNickname.isNullOrBlank()) {
            tvName.text = cachedNickname
        }

        RetrofitClient.apiService.getProfile()
            .enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserProfileResponse>>,
                    response: Response<ApiResponse<UserProfileResponse>>
                ) {
                    val profile = response.body()?.data
                    if (response.isSuccessful && profile != null) {
                        sharedPref.edit().putString("nickname", profile.nickname).apply()
                        updateProfileUI(profile.nickname, profile.profileImageUrl)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {}
            })
    }

    private fun updateProfileUI(nickname: String, imageUrl: String?) {
        tvName.text = nickname
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).circleCrop()
                .placeholder(R.drawable.union).error(R.drawable.union).into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.union)
        }
    }
}
