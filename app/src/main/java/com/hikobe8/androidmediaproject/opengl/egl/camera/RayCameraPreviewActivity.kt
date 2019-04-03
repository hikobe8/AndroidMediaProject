package com.hikobe8.androidmediaproject.opengl.egl.camera

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_ray_camera_encode.*

class RayCameraPreviewActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, RayCameraPreviewActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ray_camera_encode)
        iv_switch.setOnClickListener {
            camera_view.switch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camera_view.onDestroy()
    }

}
