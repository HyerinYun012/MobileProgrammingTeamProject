package com.example.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petplace.databinding.ActivityBusinessManageBinding
import com.example.petplace.databinding.ItemBusinessBinding

class BusinessManageActivity : AppCompatActivity() {

    // ViewBinding 객체 선언
    private lateinit var binding: ActivityBusinessManageBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityBusinessManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // RecyclerView 레이아웃 매니저 설정
        binding.rvBusinesses.layoutManager = LinearLayoutManager(this)

        // 뒤로가기 버튼 이벤트 처리
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadPendingBusinesses()
    }

    // 대기 중인 사업자 목록 로드 및 UI 업데이트
    private fun loadPendingBusinesses() {
        val bizList = dbHelper.getPendingBusinesses()

        if (bizList.isEmpty()) {
            // 데이터가 없을 경우 안내 메시지 표시 및 리스트 숨김 처리
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.rvBusinesses.visibility = View.GONE
        } else {
            // 데이터가 존재할 경우 안내 메시지 숨김 및 리스트 표시
            binding.tvEmptyMessage.visibility = View.GONE
            binding.rvBusinesses.visibility = View.VISIBLE

            // 어댑터 초기화 및 리스트 연결
            binding.rvBusinesses.adapter = BusinessAdapter(bizList) { bizId ->
                dbHelper.approveBusiness(bizId)

                // 승인 완료 후 다이얼로그 호출 및 목록 새로고침
                showCustomDialog("사업자 가입이 승인되었습니다.") {
                    loadPendingBusinesses()
                }
            }
        }
    }

    // 사업자 목록 RecyclerView 어댑터 (ViewBinding 적용)
    inner class BusinessAdapter(
        private val bizList: List<BusinessData>,
        private val onAcceptClick: (Int) -> Unit
    ) : RecyclerView.Adapter<BusinessAdapter.ViewHolder>() {

        // ViewHolder 정의 (ItemBusinessBinding 사용)
        inner class ViewHolder(private val itemBinding: ItemBusinessBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(biz: BusinessData) {
                // 데이터 바인딩
                itemBinding.tvBizNum.text = "사업자 등록번호: ${biz.bizNum}"
                itemBinding.tvBizAddress.text = "영업장 주소: ${biz.address}"
                itemBinding.tvServiceType.text = "서비스 유형: ${biz.serviceType}"

                // 가입 수락 버튼 클릭 이벤트 할당
                itemBinding.btnAccept.setOnClickListener { onAcceptClick(biz.id) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 아이템 뷰 바인딩 인플레이트
            val binding = ItemBusinessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(bizList[position])
        }

        override fun getItemCount() = bizList.size
    }
}