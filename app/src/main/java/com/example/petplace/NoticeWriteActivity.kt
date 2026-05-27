package com.example.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityNoticeWriteBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

class NoticeWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoticeWriteBinding
    private lateinit var dbHelper: DatabaseHelper
    private var noticeId: Int = -1

    private var bannerImageUri: Uri? = null
    private var originalImageUri: Uri? = null
    private var tempOriginalPickUri: Uri? = null // 갤러리에서 고른 원본 기억용

    // 1. 갤러리에서 사진 고르기 -> 2. 배너용(3:1) 크롭 실행
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            tempOriginalPickUri = uri
            startCropForBanner(uri)
        }
    }

    // 3. 배너 크롭 결과 받기 -> 4. 본문용(자유) 크롭 실행
    private val bannerCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            bannerImageUri = UCrop.getOutput(result.data!!)
            // 배너 크롭 끝났으니 바로 본문용 크롭 띄우기
            tempOriginalPickUri?.let { startCropForOriginal(it) }
        }
    }

    // 5. 본문용 크롭 결과 받기 -> 6. 화면에 적용
    private val originalCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            originalImageUri = UCrop.getOutput(result.data!!)

            // 화면엔 목록용 배너를 미리보기로 띄움
            Glide.with(this).load(bannerImageUri).into(binding.ivAttachedPhoto)
            binding.btnDeletePhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }

        // 기존 공지 수정인 경우 데이터 불러오기
        noticeId = intent.getIntExtra("NOTICE_ID", -1)
        if (noticeId != -1) {
            val notice = dbHelper.getNoticesByStoreId(1).find { it.id == noticeId }
            if (notice != null) {
                binding.etNoticeTitle.setText(notice.title)
                binding.etNoticeContent.setText(notice.content)
                if (notice.bannerUri.isNotEmpty()) {
                    bannerImageUri = Uri.parse(notice.bannerUri)
                    originalImageUri = Uri.parse(notice.originalUri)
                    binding.ivAttachedPhoto.setPadding(0,0,0,0)
                    Glide.with(this).load(bannerImageUri).into(binding.ivAttachedPhoto)
                    binding.btnDeletePhoto.visibility = View.VISIBLE
                }
            }
        }

        // 사진 추가 버튼 클릭
        binding.ivAttachedPhoto.setOnClickListener {
            if (bannerImageUri == null) {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        // 사진 삭제 버튼 클릭
        binding.btnDeletePhoto.setOnClickListener {
            bannerImageUri = null
            originalImageUri = null
            binding.ivAttachedPhoto.setImageResource(R.drawable.ic_add_photo)
            binding.btnDeletePhoto.visibility = View.GONE
        }

        // 저장하기
        binding.btnSaveNotice.setOnClickListener {
            val title = binding.etNoticeTitle.text.toString().trim()
            val content = binding.etNoticeContent.text.toString().trim()
            val bannerStr = bannerImageUri?.toString() ?: ""
            val originalStr = originalImageUri?.toString() ?: ""

            if (title.isEmpty() || content.isEmpty()) {
                showCustomDialog("제목과 내용을 모두 입력해주세요.")
                return@setOnClickListener
            }

            if (noticeId == -1) {
                dbHelper.insertNotice(1, title, content, bannerStr, originalStr)
                showCustomDialog("공지가 등록되었습니다.") { finish() }
            } else {
                dbHelper.updateNotice(noticeId, title, content, bannerStr, originalStr)
                showCustomDialog("공지가 수정되었습니다.") { finish() }
            }
        }
    }

    // ✂️ 가로 배너용 자르기 (가로:세로 = 3:1 강제)
    private fun startCropForBanner(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "banner_${UUID.randomUUID()}.jpg"))

        // 🔥 uCrop 디자인 완벽 통제 옵션!
        val options = UCrop.Options().apply {
            setToolbarTitle("목록 배너용으로 자르기")

            // 1. 상태바(알림창) 색상을 하얀색으로 덮어서 안 가려지게 만듦
            setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"))

            // 2. 상단 툴바 배경도 하얀색으로 세팅
            setToolbarColor(android.graphics.Color.parseColor("#FFFFFF"))

            // 3. 상단 툴바 글자 & 뒤로가기 버튼은 우리 앱 포인트 컬러인 주황색!
            setToolbarWidgetColor(android.graphics.Color.parseColor("#FF8A4C"))

            // 4. 밑에 자르기 조절 버튼(활성화된 탭)도 주황색!
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF8A4C"))
        }

        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(3f, 1f) // 비율 고정
            .withOptions(options) // 🔥 옵션 적용!
            .getIntent(this)

        bannerCropLauncher.launch(uCropIntent)
    }

    // 본문용 원본 자르기 (비율 자유)
    // ✂️ 본문용 원본 자르기 (비율 자유)
    private fun startCropForOriginal(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "original_${UUID.randomUUID()}.jpg"))

        // 🔥 여기도 똑같이 디자인 통제 옵션 적용!
        val options = UCrop.Options().apply {
            setToolbarTitle("본문 상세용으로 자르기")

            setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"))
            setToolbarColor(android.graphics.Color.parseColor("#FFFFFF"))
            setToolbarWidgetColor(android.graphics.Color.parseColor("#FF8A4C"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF8A4C"))
        }

        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .useSourceImageAspectRatio() // 원본 비율 허용 (자유형)
            .withOptions(options) // 🔥 옵션 적용!
            .getIntent(this)

        originalCropLauncher.launch(uCropIntent)
    }
}