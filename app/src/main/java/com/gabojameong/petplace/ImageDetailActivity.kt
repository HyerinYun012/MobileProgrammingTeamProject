package com.gabojameong.petplace

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gabojameong.petplace.databinding.ActivityImageDetailBinding

class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageList = intent.getStringArrayListExtra("IMAGE_LIST") ?: return

        binding.viewPager.adapter = ImageSliderAdapter(imageList)

       // binding.btnBack.setOnClickListener { finish() }
    }
}
