package com.example.catchcolors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catchcolors.databinding.ItemViewpagerBinding

class ViewPagerAdapter(private val listData: ArrayList<DataPage>) :
    RecyclerView.Adapter<ViewHolderPage>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPage {
        val binding =
            ItemViewpagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolderPage(binding)
    }

    override fun onBindViewHolder(holder: ViewHolderPage, position: Int) {
        holder.bind(listData[position % listData.size])
    }

    override fun getItemCount(): Int = Int.MAX_VALUE
}