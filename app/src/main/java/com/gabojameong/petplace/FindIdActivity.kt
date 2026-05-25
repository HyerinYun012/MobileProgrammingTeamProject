package com.gabojameong.petplace

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityFindIdBinding
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

        // 전화번호 자동 포맷팅 (010-xxxx-xxxx 고정)
        binding.etPhone.setText("010-")
        binding.etPhone.setSelection(4)
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                var input = s.toString().replace("-", "")
                if (!input.startsWith("010")) {
                    input = "010" + input
                }

                val formatted = StringBuilder()
                for (i in input.indices) {
                    formatted.append(input[i])
                    if ((i == 2 || i == 6) && i != input.length - 1) {
                        formatted.append("-")
                    }
                    if (i == 10) break
                }

                val finalStr = formatted.toString()
                if (s.toString() != finalStr) {
                    s?.replace(0, s.length, finalStr)
                    binding.etPhone.setSelection(binding.etPhone.length())
                }
                isFormatting = false
            }
        })

        binding.btnFindId.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || phone == "010-") {
                Toast.makeText(applicationContext, "이름과 전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
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
                        
                        Toast.makeText(applicationContext, "아이디를 찾았습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = apiResponse?.message ?: "아이디를 찾을 수 없습니다."
                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT).show()

                        binding.tvResultLabel.visibility = View.GONE
                        binding.etResultId.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e("FindId", "Error: ${t.message}")
                    Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()

                    binding.tvResultLabel.visibility = View.GONE
                    binding.etResultId.visibility = View.GONE
                }
            })
        }
    }
}
