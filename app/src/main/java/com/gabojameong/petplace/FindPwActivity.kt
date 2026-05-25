package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityFindPwBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FindPwActivity : AppCompatActivity() {
    private val binding by lazy { ActivityFindPwBinding.inflate(layoutInflater) }
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnFindPw.setOnClickListener {
            val loginId = binding.etId.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val newPassword = binding.etNewPw.text.toString().trim()
            val newPassword2 = binding.etNewPw2.text.toString().trim()

            if (loginId.isEmpty() || phone.isEmpty()) {
                Toast.makeText(applicationContext, "아이디와 전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.isEmpty() || newPassword2.isEmpty()) {
                Toast.makeText(applicationContext, "새 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != newPassword2) {
                Toast.makeText(applicationContext, "새비밀번호가 새비밀번호 확인과 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ResetPasswordRequest(
                loginId = loginId,
                phone = phone,
                newPassword = newPassword
            )
            
            apiService.resetPassword(request).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse?.success == true) {
                        Toast.makeText(applicationContext, "비밀번호가 안전하게 변경되었습니다.\n새 비밀번호: $newPassword", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = apiResponse?.message ?: "정보가 일치하지 않거나 요청에 실패했습니다."
                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Log.e("FindPw", "Error: ${t.message}")
                    Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
