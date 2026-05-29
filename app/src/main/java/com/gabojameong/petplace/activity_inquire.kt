package com.gabojameong.petplace

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class activity_inquire : AppCompatActivity() {

    private lateinit var tvInquiryCategory: TextView
    private lateinit var tvInquiryType: TextView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var rvSelectedImages: RecyclerView

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImageWriteAdapter

    private var allRestaurants = listOf<RestaurantResponse>()
    private var filteredRestaurants = mutableListOf<RestaurantResponse>()

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                selectedImageUris.addAll(uris)
                imageAdapter.notifyDataSetChanged()
            } else {
                Log.d("PhotoPicker", "사진 선택 취소됨")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inquire)

        tvInquiryCategory = findViewById(R.id.tv_inquiry_category)
        tvInquiryType = findViewById(R.id.tv_inquiry_type)
        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        rvSelectedImages = findViewById(R.id.rv_selected_images)
        val btnAddImage = findViewById<ImageButton>(R.id.btn_add_image)
        val btnSave = findViewById<Button>(R.id.btn_save)

        imageAdapter = ImageWriteAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            imageAdapter.notifyItemRangeChanged(position, selectedImageUris.size)
        }
        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = imageAdapter

        findViewById<ImageView>(R.id.imageView7).setOnClickListener {
            finish()
        }

        tvInquiryCategory.setOnClickListener {
            showInquiryCategoryDialog()
        }

        tvInquiryType.setOnClickListener {
            if (allRestaurants.isEmpty()) {
                loadRestaurantsAndShowDialog()
            } else {
                showBusinessSelectDialog()
            }
        }

        btnAddImage.setOnClickListener {
            Log.d("activity_inquire", "Add image button clicked")
            pickMultipleMedia.launch("image/*")
        }

        btnSave.setOnClickListener {
            submitInquiryToServer()
        }

        loadRestaurants()
    }

    private fun showInquiryCategoryDialog() {
        val categories = arrayOf("일반 문의", "오류 문의")
        AlertDialog.Builder(this)
            .setTitle("문의 유형 선택")
            .setItems(categories) { _, which ->
                val selectedCategory = categories[which]
                if (tvInquiryCategory.text != selectedCategory) {
                    tvInquiryType.text = "사업장 선택"
                }
                tvInquiryCategory.text = selectedCategory
            }
            .show()
    }

    private fun loadRestaurants(onComplete: (() -> Unit)? = null) {
        RetrofitClient.apiService.getAllRestaurants("", 1000).enqueue(object : Callback<ApiResponse<PageResponse<RestaurantResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, response: Response<ApiResponse<PageResponse<RestaurantResponse>>>) {
                if (response.isSuccessful) {
                    val content = response.body()?.data?.content ?: emptyList()
                    allRestaurants = content.filter { it.name != "테스트1" && it.name != "테스트2" }
                    Log.d("activity_inquire", "Restaurants loaded: ${allRestaurants.size}")
                    onComplete?.invoke()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<RestaurantResponse>>>, t: Throwable) {
                Log.e("activity_inquire", "사업장 로드 실패", t)
            }
        })
    }

    private fun loadRestaurantsAndShowDialog() {
        Toast.makeText(this, "사업장 목록을 불러오는 중입니다...", Toast.LENGTH_SHORT).show()
        loadRestaurants {
            if (allRestaurants.isNotEmpty() || tvInquiryCategory.text.toString() == "오류 문의") {
                showBusinessSelectDialog()
            } else {
                Toast.makeText(this@activity_inquire, "등록된 사업장이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBusinessSelectDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_restaurant, null)
        val etSearch = dialogView.findViewById<EditText>(R.id.et_search_restaurant)
        val rvRestaurants = dialogView.findViewById<RecyclerView>(R.id.rv_restaurants)

        val currentCategory = tvInquiryCategory.text.toString()

        val displayList = mutableListOf<RestaurantResponse>()

        if (currentCategory == "오류 문의") {
            val pendingOption = RestaurantResponse(
                id = -1L,
                name = "미정",
                category = "",
                region = "",
                address = "",
                latitude = 0.0,
                longitude = 0.0,
                phone = "",
                operatingHours = null,
                allowSmall = false,
                allowMedium = false,
                allowLarge = false,
                hasFence = false,
                hasArtificialGrass = false,
                hasNaturalGrass = false,
                hasSnack = false,
                hasParking = false,
                hasRestroom = false,
                hasIndoor = false,
                hasOutdoor = false,
                imageUrl = null,
                verified = false,
                bookmarked = false
            )
            displayList.add(pendingOption)
        }
        
        displayList.addAll(allRestaurants)
        filteredRestaurants.clear()
        filteredRestaurants.addAll(displayList)

        val adapter = SimpleRestaurantAdapter(filteredRestaurants) { selected ->
            tvInquiryType.text = selected.name
        }
        rvRestaurants.layoutManager = LinearLayoutManager(this)
        rvRestaurants.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        adapter.onItemClick = {
            dialog.dismiss()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filteredRestaurants.clear()

                val sourceList = if (currentCategory == "오류 문의") {
                    val pendingOption = displayList.first()
                    listOf(pendingOption) + allRestaurants
                } else {
                    allRestaurants
                }

                filteredRestaurants.addAll(sourceList.filter { it.name.lowercase().contains(query) })
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.show()
    }

    private class SimpleRestaurantAdapter(
        val list: List<RestaurantResponse>,
        val onClick: (RestaurantResponse) -> Unit
    ) : RecyclerView.Adapter<SimpleRestaurantAdapter.ViewHolder>() {

        var onItemClick: (() -> Unit)? = null

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_restaurant_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_simple, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.itemView.setOnClickListener {
                onClick(item)
                onItemClick?.invoke()
            }
        }

        override fun getItemCount() = list.size
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("inquiry_img_", ".jpg", cacheDir)
        val outputStream = FileOutputStream(tempFile)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return tempFile
    }

    private fun submitInquiryToServer() {
        val categoryText = tvInquiryCategory.text.toString()
        val businessTypeName = tvInquiryType.text.toString()
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (categoryText == "문의 선택" || businessTypeName == "사업장 선택" || title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "문의 유형, 사업장, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryValue = if (categoryText == "일반 문의") "GENERAL" else "ERROR"

        val selectedRestaurant = allRestaurants.find { it.name == businessTypeName }
        val restaurantId = if (businessTypeName == "미정") null else selectedRestaurant?.id

        Log.d("activity_inquire", "Submitting inquiry: type=$businessTypeName, id=$restaurantId, allSize=${allRestaurants.size}")

        val imageParts: List<MultipartBody.Part> = selectedImageUris.mapIndexedNotNull { index, uri ->
            try {
                val file = getFileFromUri(uri)
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", "image_$index.jpg", requestBody)
            } catch (e: Exception) {
                Log.e("activity_inquire", "이미지 변환 실패 (index=$index)", e)
                null
            }
        }

        val request = InquiryRequest(
            category = categoryValue,
            restaurantId = restaurantId,
            title = title,
            content = content
        )
        val dataJson = com.google.gson.Gson().toJson(request)
            .toRequestBody("application/json".toMediaTypeOrNull())

        RetrofitClient.apiService.submitInquiry(dataJson, imageParts)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@activity_inquire, "성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@activity_inquire, "등록 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@activity_inquire, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}