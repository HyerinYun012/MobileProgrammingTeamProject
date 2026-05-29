package com.gabojameong.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private val myRestaurants = mutableListOf<OwnerRestaurantSummary>()

    private val apiService = RetrofitClient.apiService
    private val gson = Gson()
    private var selectedRestaurantId: Long = -1L
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

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()

        binding.fabAddMenu.setOnClickListener {
            if (selectedRestaurantId == -1L) {
                Toast.makeText(this, "먼저 업장을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentMenuList.add(MenuData())
            menuAdapter.notifyItemInserted(currentMenuList.size - 1)
            updateMenuCountAndEmptyState()
        }

        binding.btnSaveMenus.setOnClickListener {
            if (selectedRestaurantId == -1L) {
                Toast.makeText(this, "먼저 업장을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMenus()
        }

        fetchMyRestaurants()
    }

    // ─── 업장 선택 Spinner ──────────────────────────────────────────────────────

    private fun fetchMyRestaurants() {
        apiService.getMyRestaurants().enqueue(object : Callback<ApiResponse<List<OwnerRestaurantSummary>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<OwnerRestaurantSummary>>>,
                response: Response<ApiResponse<List<OwnerRestaurantSummary>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.data ?: emptyList()
                    myRestaurants.clear()
                    myRestaurants.addAll(list)
                    setupRestaurantSpinner()
                } else {
                    // 서버에 API 없거나 오류 시 intent로 전달된 ID fallback
                    val fallbackId = intent.getLongExtra("restaurantId", -1L)
                    if (fallbackId != -1L) {
                        selectedRestaurantId = fallbackId
                        fetchMenus()
                    } else {
                        Toast.makeText(this@MenuManageActivity, "업장 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, t: Throwable) {
                val fallbackId = intent.getLongExtra("restaurantId", -1L)
                if (fallbackId != -1L) {
                    selectedRestaurantId = fallbackId
                    fetchMenus()
                } else {
                    Toast.makeText(this@MenuManageActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupRestaurantSpinner() {
        val names = mutableListOf("업장을 선택해주세요")
        names.addAll(myRestaurants.map { "${it.name} (${it.category})" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRestaurant.adapter = adapter

        binding.spinnerRestaurant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedRestaurantId = -1L
                    currentMenuList.clear()
                    menuAdapter.notifyDataSetChanged()
                    updateMenuCountAndEmptyState()
                } else {
                    val restaurant = myRestaurants[position - 1]
                    selectedRestaurantId = restaurant.id
                    deletedMenuIds.clear()
                    fetchMenus()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ─── RecyclerView ───────────────────────────────────────────────────────────

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
                menuAdapter.notifyItemRangeChanged(position, currentMenuList.size)
                updateMenuCountAndEmptyState()
            }
        )
        binding.rvMenus.adapter = menuAdapter
    }

    // ─── 메뉴 불러오기 ───────────────────────────────────────────────────────────

    private fun fetchMenus() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getMenus(selectedRestaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<MenuResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MenuResponse>>>,
                response: Response<ApiResponse<PageResponse<MenuResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val menus = response.body()?.data?.content ?: emptyList()
                    currentMenuList.clear()
                    currentMenuList.addAll(menus.map {
                        MenuData(id = it.id, name = it.name, price = it.price, imageUrl = it.imageUrl, desc = it.description)
                    })
                    menuAdapter.notifyDataSetChanged()
                    updateMenuCountAndEmptyState()
                } else {
                    Toast.makeText(this@MenuManageActivity, "메뉴 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MenuResponse>>>, t: Throwable) {
                Toast.makeText(this@MenuManageActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ─── 빈 상태 / 메뉴 개수 ─────────────────────────────────────────────────────

    private fun updateMenuCountAndEmptyState() {
        val count = currentMenuList.size
        binding.tvMenuCount.text = count.toString()
        if (count == 0 || selectedRestaurantId == -1L) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvMenus.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvMenus.visibility = View.VISIBLE
        }
    }

    // ─── 메뉴 저장 ───────────────────────────────────────────────────────────────

    private fun saveMenus() {
        deletedMenuIds.forEach { menuId ->
            apiService.deleteMenu(selectedRestaurantId, menuId).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {}
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
            })
        }
        deletedMenuIds.clear()

        val validMenus = currentMenuList.filter { it.name.isNotBlank() }
        if (validMenus.isEmpty()) {
            this.showCustomDialog("저장할 메뉴가 없습니다.")
            return
        }

        val totalTasks = validMenus.size
        var completedTasks = 0
        var isAnyFailed = false

        validMenus.forEach { menu ->
            val imagePart: MultipartBody.Part? = if (menu.imageUri.isNotEmpty() && menu.imageUri.startsWith("content://")) {
                val file = getFileFromUri(Uri.parse(menu.imageUri))
                MultipartBody.Part.createFormData("imageFile", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            } else {
                null
            }

            val requestBody = gson.toJson(MenuRequest(menu.name, menu.price, menu.desc))
                .toRequestBody("application/json".toMediaTypeOrNull())

            if (menu.id == null) {
                apiService.registerMenu(selectedRestaurantId, requestBody, imagePart)
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
                apiService.updateMenu(selectedRestaurantId, menu.id!!, requestBody, imagePart)
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
            } else {
                this.showCustomDialog("메뉴 정보가 저장되었습니다.") {
                    fetchMenus()
                }
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
