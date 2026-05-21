package com.example.petplace

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityFindIdBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FindIdActivity : AppCompatActivity() {
    private val binding by lazy { ActivityFindIdBinding.inflate(layoutInflater) }
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnFindId.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "이름과 전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = FindIdRequest(name, phone)
            apiService.findId(request).enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse?.success == true) {
                        val foundId = apiResponse.data

                        binding.etResultId.setText(foundId)
                        binding.tvResultLabel.visibility = View.VISIBLE
                        binding.etResultId.visibility = View.VISIBLE
                        
                        Toast.makeText(this@FindIdActivity, "아이디를 찾았습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = apiResponse?.message ?: "아이디를 찾을 수 없습니다."
                        Toast.makeText(this@FindIdActivity, errorMsg, Toast.LENGTH_SHORT).show()

                        binding.tvResultLabel.visibility = View.GONE
                        binding.etResultId.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e("FindId", "Error: ${t.message}")
                    Toast.makeText(this@FindIdActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()

                    binding.tvResultLabel.visibility = View.GONE
                    binding.etResultId.visibility = View.GONE
                }
            })
        }
    }
}
