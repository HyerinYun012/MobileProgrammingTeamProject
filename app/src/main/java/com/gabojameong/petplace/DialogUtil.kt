package com.gabojameong.petplace

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.gabojameong.petplace.databinding.DialogCustomBinding

fun Context.showCustomDialog(message: String, onConfirm: (() -> Unit)? = null) {
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val binding = DialogCustomBinding.inflate(LayoutInflater.from(this))
    dialog.setContentView(binding.root)

    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    binding.tvDialogMessage.text = message

    binding.btnConfirm.setOnClickListener {
        dialog.dismiss()
        onConfirm?.invoke()
    }

    dialog.show()
}
