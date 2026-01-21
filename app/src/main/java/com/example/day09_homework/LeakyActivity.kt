package com.example.day09_homework

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LeakyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.R.layout.simple_list_item_1)

        // 故意泄漏：把 Activity 存到单例
        LeakyHolder.activity = this
    }
}
