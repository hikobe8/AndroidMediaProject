package com.hikobe8.androidmediaproject

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.hikobe8.androidmediaproject.audio.AudioRecordPlayActivity
import com.hikobe8.androidmediaproject.camera.CameraBasicActivity
import com.hikobe8.androidmediaproject.camera.CameraRecordActivity
import com.hikobe8.androidmediaproject.draw.DrawImageActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun go2DrawImageByView(view: View) {
        DrawImageActivity.launch(this)
    }

    fun go2AudioRecordPlay(view: View) {
        AudioRecordPlayActivity.launch(this)
    }

    fun go2CameraCapture(view: View) {
        CameraBasicActivity.launch(this)
    }

    fun go2CameraRecord(view: View) {
        CameraRecordActivity.launch(this)
    }

}
