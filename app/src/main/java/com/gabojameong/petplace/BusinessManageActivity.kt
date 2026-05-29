package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
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
    private lateinit var adapter: BusinessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        binding.btnBack.setOnClickListener { finish() }

        loadPendingBusinesses()
    }

    private fun setupRecyclerView() {
        adapter = BusinessAdapter(
            mutableListOf(),
            onAcceptClick = { ownerId -> approveBusiness(ownerId) },
            onRejectClick = { ownerId -> rejectBusiness(ownerId) }
        )
        binding.rvBusinesses.apply {
            layoutManager = LinearLayoutManager(this@BusinessManageActivity)
            adapter = this@BusinessManageActivity.adapter
        }
    }

    private fun loadPendingBusinesses() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getPendingOwners(pageable).enqueue(object : Callback<ApiResponse<PageResponse<PendingOwnerItem>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<PendingOwnerItem>>>,
                response: Response<ApiResponse<PageResponse<PendingOwnerItem>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.content ?: emptyList()
                    if (items.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                        binding.rvBusinesses.visibility = View.GONE
                    } else {
                        binding.tvEmptyMessage.visibility = View.GONE
                        binding.rvBusinesses.visibility = View.VISIBLE
                        adapter.updateData(items)
                    }
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    binding.tvEmptyMessage.text = "목록 로드 실패: $msg"
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<PendingOwnerItem>>>, t: Throwable) {
                Log.e("BusinessManage", "Network error", t)
                Toast.makeText(this@BusinessManageActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun approveBusiness(ownerId: Long) {
        apiService.verifyOwner(ownerId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "사업자 가입이 승인되었습니다.", Toast.LENGTH_SHORT).show()
                    loadPendingBusinesses() // 리스트 갱신
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(applicationContext, "승인 실패: $msg", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(applicationContext, "네트워크 오류 발생", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun rejectBusiness(ownerId: Long) {
        apiService.rejectOwner(ownerId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "사업자 가입이 거절되었습니다.", Toast.LENGTH_SHORT).show()
                    loadPendingBusinesses() // 리스트 갱신
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(applicationContext, "거절 실패: $msg", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(applicationContext, "네트워크 오류 발생", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class BusinessAdapter(
        private val bizList: MutableList<PendingOwnerItem>,
        private val onAcceptClick: (Long) -> Unit,
        private val onRejectClick: (Long) -> Unit
    ) : RecyclerView.Adapter<BusinessAdapter.ViewHolder>() {

        fun updateData(newList: List<PendingOwnerItem>) {
            bizList.clear()
            bizList.addAll(newList)
            notifyDataSetChanged()
        }

        inner class ViewHolder(private val itemBinding: ItemBusinessBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: PendingOwnerItem) {
                itemBinding.tvOwnerInfo.text = "신청자: ${item.name} (${item.loginId})"

                itemBinding.tvStoreName.text = "가게 정보 로딩 중..."
                itemBinding.tvBizNum.text = ""
                itemBinding.tvBizAddress.text = ""
                itemBinding.tvBizCategory.text = ""

                val currentOwnerId = item.ownerId
                itemBinding.root.tag = currentOwnerId

                apiService.getOwnerBusinessInfo(item.ownerId).enqueue(object : Callback<ApiResponse<OwnerBusinessInfoResponse>> {
                    override fun onResponse(
                        call: Call<ApiResponse<OwnerBusinessInfoResponse>>,
                        response: Response<ApiResponse<OwnerBusinessInfoResponse>>
                    ) {
                        if (itemBinding.root.tag != currentOwnerId) return

                        if (response.isSuccessful && response.body()?.success == true) {
                            val business = response.body()?.data?.businesses?.firstOrNull()
                            if (business != null) {
                                itemBinding.tvStoreName.text = business.storeName
                                itemBinding.tvBizNum.text = "사업자 번호: ${business.businessNo}"
                                itemBinding.tvBizAddress.text = "주소: ${business.address}"
                                itemBinding.tvBizCategory.text = "서비스 유형: ${business.category}"
                            } else {
                                itemBinding.tvStoreName.text = "등록된 가게 정보 없음"
                            }
                        } else {
                            val errorMsg = RetrofitClient.parseErrorMessage(response)
                            itemBinding.tvStoreName.text = "정보 불러오기 실패: $errorMsg"
                            Log.e("BusinessManage", "API Error: $errorMsg")
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<OwnerBusinessInfoResponse>>, t: Throwable) {
                        if (itemBinding.root.tag != currentOwnerId) return
                        itemBinding.tvStoreName.text = "데이터 통신 에러"
                        Log.e("BusinessManage", "Network Failure", t)
                    }
                })

                itemBinding.btnAccept.setOnClickListener { onAcceptClick(item.ownerId) }
                itemBinding.btnReject.setOnClickListener { onRejectClick(item.ownerId) }
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