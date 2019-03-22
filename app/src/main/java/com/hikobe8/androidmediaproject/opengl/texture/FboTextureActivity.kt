package com.hikobe8.androidmediaproject.opengl.texture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import kotlinx.android.synthetic.main.activity_fbo_texture.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FboTextureActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, FboTextureActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fbo_texture)
        gl_content.setEGLContextClientVersion(2)
        gl_content.setRenderer(FboRenderer(this))
        gl_content.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
}