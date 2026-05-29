package com.gabojameong.petplace

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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

class owner_mypage_edit : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etLoginId: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnAuth: Button
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null
    private var userType: String = "OWNER"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                ivProfile.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_mypage_edit)

        userType = intent.getStringExtra("USER_TYPE") ?: "OWNER"

        initViews()
        setupListeners()
        loadProfileFromServer()
    }

    private fun initViews() {
        btnBack   = findViewById(R.id.imageView7)
        ivProfile = findViewById(R.id.imageView6)
        etName     = findViewById(R.id.et_owner_name)
        etNickname = findViewById(R.id.et_owner_nickname)
        etLoginId  = findViewById(R.id.et_owner_login_id)
        etPassword = findViewById(R.id.et_owner_password)
        etEmail    = findViewById(R.id.et_owner_email)
        etPhone    = findViewById(R.id.et_owner_phone)
        btnAuth    = findViewById(R.id.button7)
        btnSave    = findViewById(R.id.button8)

        // 아이디는 변경 불가
        etLoginId.isEnabled = false
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        ivProfile.setOnClickListener { pickImageLauncher.launch("image/*") }

        // 더미 인증 버튼 — Firebase 제거, 안내 Toast만 표시
        btnAuth.setOnClickListener {
            Toast.makeText(this, "인증 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadProfileFromServer() {
        RetrofitClient.apiService.getProfile()
            .enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserProfileResponse>>,
                    response: Response<ApiResponse<UserProfileResponse>>
                ) {
                    val profile = response.body()?.data ?: return
                    if (response.isSuccessful) {
                        etName.setText(profile.name ?: "")
                        etNickname.setText(profile.nickname)
                        etLoginId.setText(profile.loginId ?: "")
                        etEmail.setText(profile.email ?: "")
                        etPhone.setText(profile.phone ?: "")
                        // 비밀번호는 보안상 빈칸으로 둠 (변경 시에만 입력)

                        if (!profile.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@owner_mypage_edit)
                                .load(profile.profileImageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.union)
                                .error(R.drawable.union)
                                .into(ivProfile)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                    Toast.makeText(
                        this@owner_mypage_edit,
                        "프로필 정보를 불러오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveProfile() {
        val name     = etName.text.toString().trim()
        val nickname = etNickname.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val phone    = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임은 필수 입력 항목입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestData = UpdateProfileRequestData(
            name      = name,
            nickname  = nickname,
            email     = email,
            phone     = phone,
            newPassword = if (password.isNotEmpty()) password else null
        )

        val json = Gson().toJson(requestData)
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        // 프로필 이미지 파트 (선택한 경우에만)
        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            uriToFile(this, uri)?.let { file ->
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("profileImage", file.name, reqFile)
            }
        }

        RetrofitClient.apiService.updateProfile(requestBody, imagePart)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(
                    call: Call<ApiResponse<Any>>,
                    response: Response<ApiResponse<Any>>
                ) {
                    if (response.isSuccessful) {
                        // SharedPreferences 닉네임 최신화
                        getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                            .edit().putString("nickname", nickname).apply()
                        Toast.makeText(
                            this@owner_mypage_edit, "저장되었습니다.", Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@owner_mypage_edit, "저장에 실패했습니다.", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(
                        this@owner_mypage_edit, "서버 연결 오류: ${t.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "temp_profile_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out -> inputStream.use { it.copyTo(out) } }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
