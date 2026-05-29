package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnMypage).setOnClickListener {
            val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
            val token = sharedPref.getString("jwt_token", null)

            if (token != null) {
                // 로그인이 되어있으니 프로필 정보를 가져와서 역할에 따라 분기
                RetrofitClient.apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<UserProfileResponse>>,
                        response: Response<ApiResponse<UserProfileResponse>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val profile = response.body()?.data
                            val role = profile?.role?.uppercase()

                            Log.d("MainActivity", "User role: $role")

                            val intent = when (role) {
                                "CUSTOMER" -> Intent(this@MainActivity, CustomerMypageActivity::class.java)
                                "OWNER" -> Intent(this@MainActivity, OwnerMypageActivity::class.java)
                                "ADMIN" -> Intent(this@MainActivity, AdminMyPageActivity::class.java)
                                else -> {
                                    Toast.makeText(this@MainActivity, "알 수 없는 사용자 권한입니다: $role", Toast.LENGTH_SHORT).show()
                                    null
                                }
                            }
                            
                            intent?.let {
                                // 프로필 정보를 인텐트에 담아서 전달 (중복 호출 방지)
                                it.putExtra("user_profile", profile)
                                startActivity(it)
                            }
                        } else {
                            val errorMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@MainActivity, "세션 만료 또는 오류: $errorMsg", Toast.LENGTH_SHORT).show()
                            RetrofitClient.logout()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                        Log.e("MainActivity", "Profile load failed", t)
                        Toast.makeText(this@MainActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                // 로그인이 안 되어 있는 경우 -> 로그인 화면 유도
                Toast.makeText(applicationContext, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        findViewById<Button>(R.id.btnMap).setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnCheck).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java).apply {
                putExtra("SEARCH_ALL_REGIONS", true)
            }
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
