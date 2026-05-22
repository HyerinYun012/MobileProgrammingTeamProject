package com.example.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. 검색 버튼
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // 2. 마이페이지 버튼 (로그인 체크)
        findViewById<ImageButton>(R.id.btnMypage).setOnClickListener {
            val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
            val token = sharedPref.getString("jwt_token", null)

            if (token != null) {
                // 로그인이 되어 있는 경우 -> 마이페이지로 이동 (추후 Activity 연결)
                Toast.makeText(applicationContext, "마이페이지로 이동합니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 로그인이 안 되어 있는 경우 -> 로그인 화면 유도
                Toast.makeText(applicationContext, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        // 3. 테스트 로그아웃 버튼 (클릭 시 세션 정보를 파기하고 로그인 화면으로 이동합니다)
        findViewById<ImageButton>(R.id.btnTestLogout).setOnClickListener {
            Toast.makeText(applicationContext, "로그아웃 테스트를 진행합니다.", Toast.LENGTH_SHORT).show()
            RetrofitClient.logout()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
