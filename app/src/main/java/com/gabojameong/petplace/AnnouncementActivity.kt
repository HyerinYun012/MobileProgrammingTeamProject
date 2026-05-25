package com.gabojameong.petplace

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityAnnouncementBinding

class AnnouncementActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAnnouncementBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        val announcement = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("announcement", AnnouncementData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("announcement") as? AnnouncementData
        }

        if (announcement != null) {
            initUI(announcement)
        } else {
            Toast.makeText(applicationContext, "공지사항 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnReturn.setOnClickListener {
            finish()
        }
    }

    private fun initUI(data: AnnouncementData) {
        binding.tvTitle.text = data.title
        binding.tvPoster.text = data.posterName
        binding.tvPostTime.text = data.postTime
        binding.tvContents.text = data.contents

        // 작성자 프로필 이미지 로드
        Glide.with(this)
            .load(data.posterUrl)
            .placeholder(R.drawable.icon_pfp1)
            .error(R.drawable.icon_pfp1)
            .circleCrop()
            .into(binding.ivPoster)

        // 공지 상세 이미지 로드
        if (!data.fullImageUrl.isNullOrEmpty()) {
            binding.ivFullImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(data.fullImageUrl)
                .placeholder(R.drawable.test_announcement_full)
                .error(R.drawable.test_announcement_full)
                .into(binding.ivFullImage)
        } else {
            binding.ivFullImage.visibility = View.GONE
        }
    }
}
