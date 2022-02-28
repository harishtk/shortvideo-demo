package com.example.shortvideodemo.ui.playback

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.example.shortvideodemo.databinding.ActivityPlaybackBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaybackActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val clickedId = intent.getStringExtra("clicked") ?: "-1"

        binding.bindState(clickedId)
    }

    private fun ActivityPlaybackBinding.bindState(
        clickedId: String
    ) {
        val adapter = ViewPagerAdapter(this@PlaybackActivity, clickedId)

        pager.adapter = adapter

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = "VDO $position"
        }.attach()
        tabs.isVisible = false
    }
}