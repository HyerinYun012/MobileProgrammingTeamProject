package com.gabojameong.petplace

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

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnMypage).setOnClickListener {
            val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
            val token = sharedPref.getString("jwt_token", null)
            val role = sharedPref.getString("userRole", null)

            if (token != null) {
                if (role == "OWNER") {
                    val intent = Intent(this, owner_mypage::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, customer_mypage::class.java)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(applicationContext, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        findViewById<ImageButton>(R.id.btnTestLogout).setOnClickListener {
            Toast.makeText(applicationContext, "로그아웃 테스트를 진행합니다.", Toast.LENGTH_SHORT).show()
            RetrofitClient.logout()
        }

        findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
            val intent = Intent(this, activity_community_list::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(this, activity_map::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}