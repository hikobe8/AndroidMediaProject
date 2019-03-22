package com.hikobe8.androidmediaproject.opengl.egl

import android.content.Context
import android.util.AttributeSet
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.texture.BasicTextureRenderer

class RayGLSurfaceView(context: Context?, attrs: AttributeSet?) : RayEGLSurfaceView(context, attrs) {
    init {
        setRenderer(MyRenderer(context!!))
        setRenderMode(RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }
}

class MyRenderer(context: Context) : RayEGLSurfaceView.RayRenderer {

    private var mRenderer = BasicTextureRenderer(context, R.drawable.batman)

    override fun onSurfaceCreated() {
        mRenderer.onSurfaceCreated(null, null)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        mRenderer.onSurfaceChanged(null, width, height)
    }

    override fun onDraw() {
        mRenderer.onDrawFrame(null)
    }

}