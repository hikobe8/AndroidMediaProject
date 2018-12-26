package com.hikobe8.androidmediaproject.opengl.basic

import android.content.Context
import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_open_glbasic_draw.*

/***
 *  Author : yurui@palmax.cn
 *  Create at 2018/12/26 16:28
 *  description : OpenGLES 的基本图形绘制
 */
class OpenGLBasicDrawActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, OpenGLBasicDrawActivity::class.java))
        }
    }

    private lateinit var mGLSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_glbasic_draw)
        mGLSurfaceView = GLSurfaceView(this)
        mGLSurfaceView.setEGLContextClientVersion(2)
        mGLSurfaceView.setRenderer(TriangleRenderer(this))
        mGLSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        content.addView(mGLSurfaceView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_basic_opengl_draw, menu)
        return true
    }

    val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_triangle -> {
                content.removeAllViews()
                mGLSurfaceView = GLSurfaceView(this)
                mGLSurfaceView.setEGLContextClientVersion(2)
                mGLSurfaceView.setRenderer(TriangleRenderer(this))
                mGLSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                content.addView(mGLSurfaceView)
            }
            R.id.menu_rect -> {
                content.removeAllViews()
                mGLSurfaceView = GLSurfaceView(this)
                mGLSurfaceView.setEGLContextClientVersion(2)
                mGLSurfaceView.setRenderer(RectRenderer(this))
                mGLSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                content.addView(mGLSurfaceView)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mGLSurfaceView.onPause()
    }

}
