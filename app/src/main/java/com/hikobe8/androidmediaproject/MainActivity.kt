package com.hikobe8.androidmediaproject

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.hikobe8.androidmediaproject.draw.DrawImageActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun go2DrawImageByView(view: View) {
        DrawImageActivity.launch(this)
    }

}
