package com.example.petplace
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.petplace.databinding.ActivityImageDetailBinding

class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageList = intent.getStringArrayListExtra("IMAGE_LIST") ?: return

        // ViewPager2 설정
        binding.viewPager.adapter = ImageSliderAdapter(imageList)

        // 페이지 변경 시 인디케이터 갱신 로직 추가...
    }
}