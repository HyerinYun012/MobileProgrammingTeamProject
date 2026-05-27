package com.example.petplace

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityPetManageBinding

class PetManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetManageBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var petAdapter: PetInputAdapter
    private val currentPetList = mutableListOf<PetData>()
    private var clickedImagePosition: Int = -1

    // 사진 1장 선택 런처 (선택 즉시 어댑터로 주소 넘김)
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && clickedImagePosition != -1) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentPetList[clickedImagePosition].imageUri = uri.toString()
            petAdapter.notifyItemChanged(clickedImagePosition)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        binding.btnBack.setOnClickListener { finish() }

        // 기존 데이터 불러오기 (임시 고객 ID 1번 사용)
        val existingData = dbHelper.getPetsByUserId(1)
        currentPetList.addAll(existingData)

        // 아예 비어있으면 기본으로 1개 깔아주기
        if (currentPetList.isEmpty()) {
            currentPetList.add(PetData())
        }

        petAdapter = PetInputAdapter(
            petList = currentPetList,
            onImageClick = { position ->
                clickedImagePosition = position
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveClick = { position ->
                currentPetList.removeAt(position)
                petAdapter.notifyItemRemoved(position)
            }
        )
        binding.rvPets.adapter = petAdapter

        // 반려동물 추가하기 버튼
        binding.btnAddPet.setOnClickListener {
            currentPetList.add(PetData())
            petAdapter.notifyDataSetChanged()

            binding.scrollView.postDelayed({
                // 스크롤 뷰 안에 있는 전체 내용물의 길이를 잰 다음, 거기로 스무스하게 내려가게 함!
                val bottom = binding.scrollView.getChildAt(0).height
                binding.scrollView.smoothScrollTo(0, bottom)
            }, 100)
        }

        // 저장하기 버튼 (유효성 빡세게 검사!)
        binding.btnSavePets.setOnClickListener {
            val finalData = petAdapter.getPetList()

            // 🚨 하나라도 안 적은 칸이 있는지 매의 눈으로 스캔!
            val hasEmptyField = finalData.any {
                it.name.trim().isEmpty() ||
                        it.birthday.trim().isEmpty() ||
                        it.breed.trim().isEmpty()
            }

            if (hasEmptyField) {
                // 빵꾸난 곳이 하나라도 있으면 바로 컷!
                showCustomDialog("반려동물의 이름, 생일, 품종을 모두 입력해주세요.")
                return@setOnClickListener
            }

            // 전부 꽉꽉 채워져 있다면 안전하게 DB 저장!
            dbHelper.saveOrUpdatePets(1, finalData)
            showCustomDialog("반려동물 정보가 성공적으로 저장되었습니다.") {
                finish()
            }
        }
    }
}