package com.example.petplace

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.petplace.databinding.ItemMenuInputBinding

class MenuInputAdapter(
    private val menuList: MutableList<MenuData>,
    private val onImageClick: (Int) -> Unit, // 사진 클릭 시 Activity에 위치(position) 전달
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<MenuInputAdapter.ViewHolder>() {

    fun getMenuList(): List<MenuData> = menuList

    inner class ViewHolder(private val binding: ItemMenuInputBinding) : RecyclerView.ViewHolder(binding.root) {

        // 텍스트가 바뀔 때마다 즉시 리스트에 저장해주는 Watcher
        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    menuList[position].name = binding.etMenuName.text.toString()
                    menuList[position].price = binding.etMenuPrice.text.toString()
                    menuList[position].desc = binding.etMenuDesc.text.toString()
                }
            }
        }

        fun bind(menu: MenuData, position: Int) {
            // 기존 텍스트워처 해제 후 값 셋팅 (스크롤 시 꼬임 방지)
            binding.etMenuName.removeTextChangedListener(textWatcher)
            binding.etMenuPrice.removeTextChangedListener(textWatcher)
            binding.etMenuDesc.removeTextChangedListener(textWatcher)

            binding.etMenuName.setText(menu.name)
            binding.etMenuPrice.setText(menu.price)
            binding.etMenuDesc.setText(menu.desc)

            binding.etMenuName.addTextChangedListener(textWatcher)
            binding.etMenuPrice.addTextChangedListener(textWatcher)
            binding.etMenuDesc.addTextChangedListener(textWatcher)

            // 🔥 사진 바인딩
            if (menu.imageUri.isNotEmpty()) {
                // 진짜 사진이 들어올 땐 Glide로 띄우기
                Glide.with(itemView.context)
                    .load(Uri.parse(menu.imageUri))
                    .transform(RoundedCorners(20))
                    .into(binding.ivMenuPhoto)
            } else {
                binding.ivMenuPhoto.setImageResource(R.drawable.ic_add_photo)
            }

            // 클릭 이벤트들
            binding.ivMenuPhoto.setOnClickListener { onImageClick(position) }
            binding.btnRemoveItem.setOnClickListener { onRemoveClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuList[position], position)
    }

    override fun getItemCount() = menuList.size
}