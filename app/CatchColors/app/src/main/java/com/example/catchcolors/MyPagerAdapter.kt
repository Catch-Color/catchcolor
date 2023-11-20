package com.example.catchcolors

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
    private val NUM_PAGES = 4

    // 텝에 맞춰 프래그먼트 연결시킴
    override fun getItemCount(): Int = NUM_PAGES
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> { FragmentA.newInstance("Page 1","")}
            1 -> { FragmentB.newInstance("Page 2","")}
            2 -> { FragmentC.newInstance("Page 3","")}
            else -> { FragmentD.newInstance("Page 4","")}
        }
    }
}