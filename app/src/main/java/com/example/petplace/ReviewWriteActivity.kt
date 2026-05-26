package com.example.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petplace.databinding.ActivityReviewWriteBinding

class ReviewWriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewWriteBinding
    private lateinit var photoAdapter: ReviewPhotoAdapter
    private val selectedPhotos = mutableListOf<Uri>()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                if (!selectedPhotos.contains(uri) && selectedPhotos.size < 5) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    selectedPhotos.add(uri)
                }
            }
            photoAdapter.setPhotos(selectedPhotos)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // 어댑터 생성자 콜백 내부에 사진 추가 로직 연동
        photoAdapter = ReviewPhotoAdapter(
            onAddClick = {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeleteClick = { uri ->
                selectedPhotos.remove(uri)
                photoAdapter.setPhotos(selectedPhotos)
            }
        )

        binding.rvReviewPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvReviewPhotos.adapter = photoAdapter

        // 🚀 등록 버튼 클릭 이벤트
        binding.btnSubmitReview.setOnClickListener {
            val rating = binding.ratingBar.rating
            // trim()을 붙여서 앞뒤 공백(스페이스바)만 친 경우를 다 날려버림!
            val content = binding.etReviewContent.text.toString().trim()

            // 🔥 1. 필수 입력값 방어막 (유효성 검사)
            if (rating == 0f || content.isEmpty()) {
                // 둘 중 하나라도 안 적혀 있으면 팝업 띄우고
                showCustomDialog("별점과 리뷰 내용을 모두 작성해주세요!")
                // return을 써서 여기서 함수를 강제 종료시켜버림! (밑에 DB 저장 로직 실행 안 됨)
                return@setOnClickListener
            }

            // 🔥 2. 위 방어막을 무사히 통과했다면 DB에 저장!
            // 사진 주소들을 쉼표로 묶어서 하나의 텍스트로 만듦
            val imageUrisString = photoAdapter.getPhotos().joinToString(",") { it.toString() }

            val dbHelper = DatabaseHelper(this)
            dbHelper.insertReview(rating, content, imageUrisString)

            // 성공 팝업 띄우고 목록으로 돌아가기
            showCustomDialog("리뷰가 등록되었습니다!") {
                finish()
            }
        }
    }
}