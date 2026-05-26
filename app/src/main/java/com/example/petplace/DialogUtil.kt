package com.example.petplace // 본인 패키지명 확인!

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView

// 🌟 어느 화면(Context)에서든 이 함수 하나면 팝업창이 뜸!
fun Context.showCustomDialog(message: String, onConfirm: () -> Unit = {}) {
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_custom) // 방금 만든 예쁜 팝업창 연결

    // 배경을 투명하게 해야 둥근 모서리가 예쁘게 보임
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    val tvMessage = dialog.findViewById<TextView>(R.id.tv_dialog_message)
    val btnConfirm = dialog.findViewById<TextView>(R.id.btn_confirm)

    // 내가 넘겨준 메시지로 글자 쏙 바꾸기!
    tvMessage.text = message

    // 확인 버튼 눌렀을 때
    btnConfirm.setOnClickListener {
        dialog.dismiss() // 팝업창 끄기
        onConfirm()      // 팝업창 끈 다음에 할 일 (새로고침 등) 실행!
    }

    dialog.show()
}