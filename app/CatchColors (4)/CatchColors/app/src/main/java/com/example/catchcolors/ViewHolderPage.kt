package com.example.catchcolors

import androidx.recyclerview.widget.RecyclerView
import com.example.catchcolors.databinding.ItemViewpagerBinding

class ViewHolderPage(private val binding: ItemViewpagerBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(data: DataPage) {
        binding.tvTitle.text = data.title
        binding.rlLayout.setBackgroundResource(data.color)
    }
}