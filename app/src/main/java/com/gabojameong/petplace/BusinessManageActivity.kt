package com.gabojameong.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ActivityBusinessManageBinding
import com.gabojameong.petplace.databinding.ItemBusinessBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BusinessManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessManageBinding
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvBusinesses.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        loadPendingBusinesses()
    }

    private fun loadPendingBusinesses() {
        // 실제 API가 준비되지 않았을 경우를 대비해 안내 메시지나 빈 리스트 처리
        // 현재 ApiService에 getPendingOwners 등의 리스트 API가 정의되어야 합니다.
        // 임시로 비어있는 상태로 두거나, 기존 로직을 API 구조에 맞춰 변경합니다.
        binding.tvEmptyMessage.visibility = View.VISIBLE
        binding.rvBusinesses.visibility = View.GONE
    }

    private fun approveBusiness(ownerId: Long) {
        apiService.verifyOwner(ownerId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    showCustomDialog("사업자 가입이 승인되었습니다.") {
                        loadPendingBusinesses()
                    }
                } else {
                    Toast.makeText(this@BusinessManageActivity, "승인 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(this@BusinessManageActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class BusinessAdapter(
        private val bizList: List<User>, // User DTO 활용
        private val onAcceptClick: (Long) -> Unit
    ) : RecyclerView.Adapter<BusinessAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemBusinessBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(user: User) {
                itemBinding.tvBizNum.text = "아이디: ${user.nickname}"
                itemBinding.tvBizAddress.text = "연락처: ${user.phone}"
                itemBinding.tvServiceType.text = "역할: ${user.role}"

                itemBinding.btnAccept.setOnClickListener { onAcceptClick(user.id) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemBusinessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(bizList[position])
        }

        override fun getItemCount() = bizList.size
    }
}
