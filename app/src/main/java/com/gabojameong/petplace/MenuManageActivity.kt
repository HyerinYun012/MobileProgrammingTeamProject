package com.gabojameong.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityMenuManageBinding
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class MenuManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuManageBinding
    private lateinit var menuAdapter: MenuInputAdapter
    private val currentMenuList = mutableListOf<MenuData>()
    private val deletedMenuIds = mutableListOf<Long>()

    private val apiService = RetrofitClient.apiService
    private val gson = Gson()
    private var restaurantId: Long = 1L

    private var clickedImagePosition: Int = -1

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && clickedImagePosition != -1) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentMenuList[clickedImagePosition].imageUri = uri.toString()
            menuAdapter.notifyItemChanged(clickedImagePosition)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전달받은 식당 ID가 있다면 사용, 없으면 기본값 1L
        restaurantId = intent.getLongExtra("restaurantId", 1L)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        fetchMenus()

        binding.btnAddMenu.setOnClickListener {
            currentMenuList.add(MenuData())
            menuAdapter.notifyItemInserted(currentMenuList.size - 1)
        }

        binding.btnSaveMenus.setOnClickListener {
            saveMenus()
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuInputAdapter(
            menuList = currentMenuList,
            onImageClick = { position ->
                clickedImagePosition = position
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveClick = { position ->
                val item = currentMenuList[position]
                item.id?.let { deletedMenuIds.add(it) }
                currentMenuList.removeAt(position)
                menuAdapter.notifyItemRemoved(position)
            }
        )
        binding.rvMenus.adapter = menuAdapter
    }

    private fun fetchMenus() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getMenus(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<MenuResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MenuResponse>>>,
                response: Response<ApiResponse<PageResponse<MenuResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val menus = response.body()?.data?.content ?: emptyList()
                    currentMenuList.clear()
                    currentMenuList.addAll(menus.map { 
                        MenuData(
                            id = it.id, 
                            name = it.name, 
                            price = it.price, 
                            imageUrl = it.imageUrl, 
                            desc = it.description
                        ) 
                    })
                    if (currentMenuList.isEmpty()) currentMenuList.add(MenuData())
                    menuAdapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<MenuResponse>>>, t: Throwable) {
                Toast.makeText(this@MenuManageActivity, "메뉴 로드 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveMenus() {
        // 1. 삭제 처리
        deletedMenuIds.forEach { menuId ->
            apiService.deleteMenu(restaurantId, menuId).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {}
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
        }
        deletedMenuIds.clear()

        // 2. 추가 및 수정 처리
        val totalTasks = currentMenuList.size
        if (totalTasks == 0) {
            this.showCustomDialog("저장할 메뉴가 없습니다.")
            return
        }

        var completedTasks = 0
        var isAnyFailed = false

        currentMenuList.forEach { menu ->
            val imagePart = if (menu.imageUri.startsWith("content://")) {
                val file = getFileFromUri(Uri.parse(menu.imageUri))
                MultipartBody.Part.createFormData("imageFile", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            } else {
                null
            }

            if (menu.id == null) {
                // 신규 메뉴 등록
                val request = MenuRequest(menu.name, menu.price, menu.desc)
                val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())

                apiService.registerMenu(restaurantId, requestBody, imagePart)
                    .enqueue(object : Callback<ApiResponse<Long>> {
                        override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                            if (!response.isSuccessful) isAnyFailed = true
                            checkAndFinish(++completedTasks, totalTasks, isAnyFailed)
                        }
                        override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                            isAnyFailed = true
                            checkAndFinish(++completedTasks, totalTasks, isAnyFailed)
                        }
                    })
            } else {
                // 기존 메뉴 수정
                val request = MenuRequest(menu.name, menu.price, menu.desc)
                val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
                
                apiService.updateMenu(restaurantId, menu.id!!, requestBody, imagePart)
                    .enqueue(object : Callback<ApiResponse<Any>> {
                        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                            if (!response.isSuccessful) isAnyFailed = true
                            checkAndFinish(++completedTasks, totalTasks, isAnyFailed)
                        }
                        override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                            isAnyFailed = true
                            checkAndFinish(++completedTasks, totalTasks, isAnyFailed)
                        }
                    })
            }
        }
    }

    private fun checkAndFinish(completed: Int, total: Int, failed: Boolean) {
        if (completed == total) {
            if (failed) {
                Toast.makeText(this, "일부 메뉴 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
            this.showCustomDialog("메뉴 정보가 저장되었습니다.") {
                finish()
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_menu_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        return file
    }
}
