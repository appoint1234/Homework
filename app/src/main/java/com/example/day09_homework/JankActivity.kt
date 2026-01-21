package com.example.day09_homework

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class JankActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val list = ArrayList<ByteArray>() // 用来制造内存压力
    private var running = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return

            // 1) 主线程重计算（制造卡顿）
            var sum = 0L
            for (i in 0 until 20_000_000) { // 数字可调
                sum += i
            }

            // 2) 频繁分配内存（制造 GC 抖动）
            list.add(ByteArray(2 * 1024 * 1024)) // 每次 2MB
            if (list.size > 10) list.removeAt(0)

            handler.postDelayed(this, 16) // 尝试 60fps 跑，会严重掉帧
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.simple_list_item_1)
    }

    override fun onResume() {
        super.onResume()
        running = true
        handler.post(ticker)
    }

    override fun onPause() {
        running = false
        handler.removeCallbacks(ticker)
        super.onPause()
    }
}
