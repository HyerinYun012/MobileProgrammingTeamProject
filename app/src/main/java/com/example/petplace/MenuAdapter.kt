package com.example.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ItemMenuBinding

data class MenuData(
    val name: String,
    val price: Int,
    val imageUrl: String? = null
)

class MenuAdapter(private val menuList: List<MenuData>) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(private val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menu: MenuData) {
            binding.tvMenuName.text = menu.name
            binding.tvMenuPrice.text = "${menu.price} 원"
            
            Glide.with(binding.
            ivMenuImage.context)
                .load(menu.imageUrl)
                .placeholder(R.drawable.test_bread)
                .into(binding.ivMenuImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuList[position])
    }

    override fun getItemCount(): Int = menuList.size
}
