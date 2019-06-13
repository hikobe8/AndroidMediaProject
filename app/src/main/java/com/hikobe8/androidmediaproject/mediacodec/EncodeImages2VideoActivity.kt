package com.hikobe8.androidmediaproject.mediacodec

import android.content.Context
import android.content.Intent
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.texture.FboRenderer
import kotlinx.android.synthetic.main.activity_encode_images2_video.*
import kotlin.concurrent.thread

/***
 *  Author : hikobe8@github.com
 *  Create at 19-6-13 下午6:37
 *  description : 
 */
class EncodeImages2VideoActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, EncodeImages2VideoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encode_images2_video)
        gl_content.setEGLContextClientVersion(2)
        val renderer = FboRenderer(this)
        gl_content.setRenderer(renderer)
        gl_content.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        thread {
            for (i in 0 .. 134){
                val id = resources.getIdentifier("slice$i", "drawable", packageName)
                renderer.setImageResId(id)
                gl_content.requestRender()
                Thread.sleep(60)
            }

        }
    }
}
