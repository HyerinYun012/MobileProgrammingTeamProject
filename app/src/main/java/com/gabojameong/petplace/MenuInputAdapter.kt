package com.gabojameong.petplace

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gabojameong.petplace.databinding.ItemMenuInputBinding

class MenuInputAdapter(
    private val menuList: MutableList<MenuData>,
    private val onImageClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<MenuInputAdapter.ViewHolder>() {

    fun getMenuList(): List<MenuData> = menuList

    inner class ViewHolder(private val binding: ItemMenuInputBinding) : RecyclerView.ViewHolder(binding.root) {

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = menuList[position]
                    // 입력값 실시간 저장
                    item.name = binding.etMenuName.text.toString()
                    item.price = binding.etMenuPrice.text.toString().toIntOrNull() ?: 0
                    item.desc = binding.etMenuDesc.text.toString()
                }
            }
        }

        fun bind(menu: MenuData) {
            // 재사용 시 중복 리스너 방지를 위해 먼저 제거
            binding.etMenuName.removeTextChangedListener(textWatcher)
            binding.etMenuPrice.removeTextChangedListener(textWatcher)
            binding.etMenuDesc.removeTextChangedListener(textWatcher)

            binding.etMenuName.setText(menu.name)
            binding.etMenuPrice.setText(if (menu.price == 0) "" else menu.price.toString())
            binding.etMenuDesc.setText(menu.desc ?: "")

            // 다시 리스너 등록
            binding.etMenuName.addTextChangedListener(textWatcher)
            binding.etMenuPrice.addTextChangedListener(textWatcher)
            binding.etMenuDesc.addTextChangedListener(textWatcher)

            if (menu.imageUri.isNotEmpty()) {
                Glide.with(binding.ivMenuPhoto.context)
                    .load(Uri.parse(menu.imageUri))
                    .transform(RoundedCorners(20))
                    .into(binding.ivMenuPhoto)
            } else {
                binding.ivMenuPhoto.setImageResource(R.drawable.ic_add_photo)
            }

            binding.ivMenuPhoto.setOnClickListener { 
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onImageClick(bindingAdapterPosition)
                }
            }
            binding.btnRemoveItem.setOnClickListener { 
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(bindingAdapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuList[position])
    }

    override fun getItemCount() = menuList.size
}
