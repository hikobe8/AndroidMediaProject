package com.hikobe8.androidmediaproject.opengl.camera

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_camera1_gl.*

class CameraGLActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, CameraGLActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_gl)
        btn_switch.setOnClickListener {
            camera_view.switch()
        }
    }

    override fun onResume() {
        super.onResume()
        camera_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        camera_view.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}
