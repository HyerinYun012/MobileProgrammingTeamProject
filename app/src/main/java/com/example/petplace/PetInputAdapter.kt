package com.example.petplace

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ItemPetInputBinding

class PetInputAdapter(
    private val petList: MutableList<PetData>,
    private val onImageClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PetInputAdapter.ViewHolder>() {

    fun getPetList(): List<PetData> = petList

    inner class ViewHolder(private val binding: ItemPetInputBinding) : RecyclerView.ViewHolder(binding.root) {
        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    petList[position].name = binding.etPetName.text.toString()
                    petList[position].birthday = binding.etPetBirthday.text.toString()
                    petList[position].breed = binding.etPetBreed.text.toString()
                }
            }
        }

        fun bind(pet: PetData, position: Int) {

            binding.etPetName.removeTextChangedListener(textWatcher)
            binding.etPetBirthday.removeTextChangedListener(textWatcher)
            binding.etPetBreed.removeTextChangedListener(textWatcher)

            binding.etPetName.setText(pet.name)
            binding.etPetBirthday.setText(pet.birthday)
            binding.etPetBreed.setText(pet.breed)

            binding.etPetName.addTextChangedListener(textWatcher)
            binding.etPetBirthday.addTextChangedListener(textWatcher)
            binding.etPetBreed.addTextChangedListener(textWatcher)

            // 프사 처리 (Glide circleCrop)
            if (pet.imageUri.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(Uri.parse(pet.imageUri))
                    .circleCrop()
                    .into(binding.ivPetProfile)
            } else {
                binding.ivPetProfile.setImageResource(R.drawable.ic_profile_dog)
            }

            binding.ivPetProfile.setOnClickListener { onImageClick(position) }
            binding.btnRemovePet.setOnClickListener { onRemoveClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemPetInputBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(petList[position], position)
    override fun getItemCount() = petList.size
}