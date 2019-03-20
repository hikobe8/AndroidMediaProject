package com.hikobe8.androidmediaproject.opengl.egl

import android.content.Context
import android.opengl.GLES20
import android.util.AttributeSet

class RayGLSurfaceView(context: Context?, attrs: AttributeSet?) : RayEGLSurfaceView(context, attrs) {
    init {
        setRenderer(MyRenderer())
        setRenderMode(RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }
}

class MyRenderer : RayEGLSurfaceView.RayRenderer {
    override fun onSurfaceCreated() {
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 1f, 0f, 1f)
    }

}