package com.hikobe8.androidmediaproject.opengl.egl

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R

class CustomizedGLSurfaceActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, CustomizedGLSurfaceActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customized_glsurface)
    }
}
