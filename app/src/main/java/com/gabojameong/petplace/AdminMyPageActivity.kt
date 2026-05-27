package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityAdminMyPageBinding

class AdminMyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMyPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutMenuReport.setOnClickListener {
            val intent = Intent(this, ReportManageActivity::class.java)
            startActivity(intent)
        }

        binding.layoutMenuBusiness.setOnClickListener {
            val intent = Intent(this, BusinessManageActivity::class.java)
            startActivity(intent)
        }

        binding.layoutMenuInquiry.setOnClickListener {
            val intent = Intent(this, InquiryManageActivity::class.java)
            startActivity(intent)
        }
    }
}
