package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petplace.databinding.ActivityInquiryManageBinding
import com.example.petplace.databinding.ItemInquiryBinding

class InquiryManageActivity : AppCompatActivity() {

    // ViewBinding 및 헬퍼 객체 선언
    private lateinit var binding: ActivityInquiryManageBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityInquiryManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // RecyclerView 레이아웃 매니저 설정
        binding.rvInquiries.layoutManager = LinearLayoutManager(this)

        // 뒤로가기 버튼 이벤트 처리
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        // DB에서 전체 문의 내역 조회
        val inquiryList = dbHelper.getAllInquiries()

        // 어댑터 연결 및 클릭 이벤트 정의
        binding.rvInquiries.adapter = InquiryAdapter(inquiryList) { inquiry ->
            // 상태 업데이트 (읽음 처리)
            dbHelper.checkInquiry(inquiry.id)

            // 상세 페이지 이동 및 데이터 전달
            val intent = Intent(this, InquiryDetailActivity::class.java).apply {
                putExtra("CATEGORY", inquiry.category)
                putExtra("EMAIL", inquiry.email)
                putExtra("CONTENT", inquiry.content)
            }
            startActivity(intent)
        }
    }

    // 1:1 문의 목록 RecyclerView 어댑터 (ViewBinding 적용)
    inner class InquiryAdapter(
        private val list: List<InquiryData>,
        private val onItemClick: (InquiryData) -> Unit
    ) : RecyclerView.Adapter<InquiryAdapter.ViewHolder>() {

        // ViewHolder 정의 (ItemInquiryBinding 사용)
        inner class ViewHolder(private val itemBinding: ItemInquiryBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(inquiry: InquiryData) {
                // 데이터 바인딩
                itemBinding.tvCategory.text = inquiry.category
                itemBinding.tvContentPreview.text = inquiry.content

                // 확인 여부에 따른 배경 리소스 분기 처리
                if (inquiry.isChecked) {
                    itemBinding.root.setBackgroundResource(R.drawable.bg_border_gray)
                } else {
                    itemBinding.root.setBackgroundResource(R.drawable.bg_border_orange)
                }

                // 항목 클릭 이벤트 할당
                itemBinding.root.setOnClickListener { onItemClick(inquiry) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 아이템 뷰 바인딩 인플레이트
            val binding = ItemInquiryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size
    }
}