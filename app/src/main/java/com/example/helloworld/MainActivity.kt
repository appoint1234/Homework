package com.example.helloworld

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()//让界面延伸到屏幕边缘，更全面屏
        setContentView(R.layout.activity_main)//定义布局
        //拿到页面的宽高，实现全屏布局
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var textview =findViewById<TextView>(R.id.text)
        var button =findViewById<Button>(R.id.clickme)
        var ischanged=false
        button.setOnClickListener {
            if(!ischanged){
                textview.text="你点击了按钮"
                ischanged=true
            }else{
                textview.text="hello World"
                ischanged=false
            }

        }
    }
}