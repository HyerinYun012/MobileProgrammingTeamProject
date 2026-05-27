package com.example.petplace

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityMenuManageBinding

class MenuManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuManageBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var menuAdapter: MenuInputAdapter
    private val currentMenuList = mutableListOf<MenuData>()

    // 현재 사진을 등록하려고 클릭한 메뉴의 순서(position) 저장
    private var clickedImagePosition: Int = -1

    // 사진 1장 선택 런처
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && clickedImagePosition != -1) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // 해당 위치의 메뉴 데이터에 사진 주소 저장하고 갱신!
            currentMenuList[clickedImagePosition].imageUri = uri.toString()
            menuAdapter.notifyItemChanged(clickedImagePosition)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        binding.btnBack.setOnClickListener { finish() }

        // 1. 기존에 저장된 메뉴 데이터 불러오기 (데이터 유지 기능)
        val existingData = dbHelper.getMenusByStoreId(1) // 임시 스토어 ID 1 사용
        currentMenuList.addAll(existingData)

        // 데이터가 아예 없으면 빈 칸 하나 띄워주기
        if (currentMenuList.isEmpty()) {
            currentMenuList.add(MenuData())
        }

        // 2. 어댑터 연결
        menuAdapter = MenuInputAdapter(
            menuList = currentMenuList,
            onImageClick = { position ->
                clickedImagePosition = position
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveClick = { position ->
                currentMenuList.removeAt(position)
                menuAdapter.notifyItemRemoved(position)
            }
        )
        binding.rvMenus.adapter = menuAdapter

        // 3. '메뉴 추가 +' 버튼 클릭 시 빈 칸 새로 생성
        binding.btnAddMenu.setOnClickListener {
            currentMenuList.add(MenuData()) // 새 빈 데이터 추가
            menuAdapter.notifyItemInserted(currentMenuList.size - 1)
        }

        // 🔥 4. '저장하기' 버튼 클릭 시 작동할 진짜 저장 로직 (이쪽으로 이사 완료!)
        binding.btnSaveMenus.setOnClickListener {
            val finalData = menuAdapter.getMenuList()

            // DB에 통째로 덮어쓰기 업데이트
            dbHelper.saveOrUpdateMenus(1, finalData) // 임시 스토어 ID 1 사용

            // 저장 성공 팝업 띄우고 이전 화면으로 나가기
            showCustomDialog("메뉴가 성공적으로 저장되었습니다!") {
                finish()
            }
        }
    }
}