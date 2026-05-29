package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class InquiryUiModel(
    val id: String,
    val title: String,
    val isReplied: Boolean
)

class InquiryListActivity : AppCompatActivity() {

    private var userRole: String = "CUSTOMER"
    private lateinit var rvInquiryList: RecyclerView

    private val startWriteInquiry = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadInquiryDataFromServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inquirelist)

        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        userRole = sharedPref.getString("userRole", "CUSTOMER") ?: "CUSTOMER"

        rvInquiryList = findViewById(R.id.rv_inquiry_list)
        val btnWriteInquiry = findViewById<ImageButton>(R.id.btn_write_inquiry)
        val tvWriteInquiryLabel = findViewById<TextView>(R.id.tv_write_inquiry_label)
        val btnBack = findViewById<ImageView>(R.id.imageView7)

        if (userRole == "OWNER") {
            btnWriteInquiry.visibility = View.GONE
            tvWriteInquiryLabel.visibility = View.GONE
        } else {
            btnWriteInquiry.visibility = View.VISIBLE
            tvWriteInquiryLabel.visibility = View.VISIBLE
            val writeClick = View.OnClickListener {
                startWriteInquiry.launch(Intent(this, InquiryWriteActivity::class.java))
            }
            btnWriteInquiry.setOnClickListener(writeClick)
            tvWriteInquiryLabel.setOnClickListener(writeClick)
        }

        btnBack.setOnClickListener {
            val dest = if (userRole == "OWNER") OwnerMypageActivity::class.java else CustomerMypageActivity::class.java
            startActivity(Intent(this, dest).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
            finish()
        }

        rvInquiryList.layoutManager = LinearLayoutManager(this)
        loadInquiryDataFromServer()
    }

    override fun onResume() {
        super.onResume()
        loadInquiryDataFromServer()
    }

    private fun loadInquiryDataFromServer() {
        val pageable = mapOf("page" to "0", "size" to "20")
        val call = if (userRole == "OWNER") {
            RetrofitClient.apiService.getGeneralInquiries(pageable)
        } else {
            RetrofitClient.apiService.getMyInquiries(pageable)
        }

        call.enqueue(object : Callback<ApiResponse<PageResponse<InquiryResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<InquiryResponse>>>, response: Response<ApiResponse<PageResponse<InquiryResponse>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val uiData = (response.body()?.data?.content ?: emptyList()).map { res ->
                        InquiryUiModel(
                            id = res.id.toString(),
                            title = res.title ?: "(제목 없음)",
                            isReplied = res.status == "COMPLETED"
                        )
                    }
                    setupAdapter(uiData)
                } else {
                    Log.e("InquiryList", "Error: ${response.code()}")
                    Toast.makeText(this@InquiryListActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<InquiryResponse>>>, t: Throwable) {
                Log.e("InquiryList", "Failure: ${t.message}")
                Toast.makeText(this@InquiryListActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAdapter(dataList: List<InquiryUiModel>) {
        val dest = if (userRole == "OWNER") OwnerInquiryAnswerActivity::class.java else CustomerInquiryAnswerActivity::class.java
        val adapter = InquiryAdapter(dataList) { item ->
            startActivity(Intent(this, dest).apply {
                putExtra("INQUIRY_TITLE", item.title)
                putExtra("INQUIRY_ID", item.id)
            })
        }
        rvInquiryList.adapter = adapter
    }
}
