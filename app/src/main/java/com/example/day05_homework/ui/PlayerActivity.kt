package com.example.day05_homework.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.day05_homework.R
import com.example.day05_homework.service.MusicService

class PlayerActivity : AppCompatActivity() {

    private var service: MusicService? = null
    private var bound = false

    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var seek: SeekBar
    private lateinit var btnPlay: Button
    private lateinit var btnNext: Button

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val s = service
            if (s != null && bound) {
                val dur = s.duration()
                val pos = s.position()
                seek.max = if (dur > 0) dur else 1
                seek.progress = pos
                tvTitle.text = "${s.currentTrack().title} - ${s.currentTrack().artist}"
                tvTime.text = "${fmt(pos)} / ${fmt(dur)}"
                btnPlay.text = if (s.isPlaying()) "Pause" else "Play"
            }
            handler.postDelayed(this, 500)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as MusicService.MusicBinder).getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        tvTitle = findViewById(R.id.tvTitle)
        tvTime = findViewById(R.id.tvTime)
        seek = findViewById(R.id.seekBar)
        btnPlay = findViewById(R.id.btnPlay)
        btnNext = findViewById(R.id.btnNext)

        // 启动前台服务（保证退后台也播）
        val start = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, start)

        // 绑定 service 用于控制
        bindService(Intent(this, MusicService::class.java), conn, Context.BIND_AUTO_CREATE)

        btnPlay.setOnClickListener { service?.togglePlayPause() }
        btnNext.setOnClickListener { service?.next() }

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                service?.seekTo(seekBar?.progress ?: 0)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        handler.post(tick)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(tick)
    }

    override fun onDestroy() {
        if (bound) unbindService(conn)
        super.onDestroy()
    }

    private fun fmt(ms: Int): String {
        val total = ms / 1000
        val m = total / 60
        val s = total % 60
        return "%d:%02d".format(m, s)
    }
}