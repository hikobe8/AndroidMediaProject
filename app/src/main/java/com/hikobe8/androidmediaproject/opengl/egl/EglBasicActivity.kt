package com.hikobe8.androidmediaproject.opengl.egl

import android.content.Context
import android.content.Intent
import android.opengl.GLES20
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_egl_basic.*

class EglBasicActivity : AppCompatActivity(), SurfaceHolder.Callback {

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Thread(
            Runnable {
                mEglHelper.start(holder!!.surface, null)
                while (true) {
                    GLES20.glViewport(0, 0, width, height)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    GLES20.glClearColor(1f, 1f, 0f, 1f)
                    mEglHelper.swapBuffers()
                    Thread.sleep(16)
                }
            }
        ).start()

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mEglHelper.destroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, EglBasicActivity::class.java))
        }
    }

    private var mEglHelper = EglHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egl_basic)
        sv.holder.addCallback(this)
    }

}
