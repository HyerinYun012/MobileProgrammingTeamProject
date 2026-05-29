package com.gabojameong.petplace

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: run { finish(); return }

        val ivFullscreen = findViewById<ImageView>(R.id.iv_fullscreen)
        val btnClose = findViewById<ImageView>(R.id.btn_close)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.icon_nothing)
            .error(R.drawable.icon_nothing)
            .into(ivFullscreen)

        btnClose.setOnClickListener { finish() }
        ivFullscreen.setOnClickListener { finish() }
    }
}
