package com.example.shortvideodemo.ui.playback

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

const val NUM_PAGES = 1

class ViewPagerAdapter(fa: FragmentActivity, private val clickedId: String) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    override fun createFragment(position: Int): Fragment {
        return VideoFragment.newInstance("VDO $position",
            if (position == 0) clickedId else "-1")
    }

}