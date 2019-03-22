package com.hikobe8.androidmediaproject.opengl.egl

import android.opengl.GLSurfaceView

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-22 17:55
 *  description :
 */
interface RayRenderer {
    fun onSurfaceCreated()
    fun onSurfaceSizeChanged(width: Int, height: Int)
    fun onDraw()
}

class RayRendererWrappter(private val renderer: GLSurfaceView.Renderer):RayRenderer {
    override fun onSurfaceCreated() {
        renderer.onSurfaceCreated(null, null)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        renderer.onSurfaceChanged(null, width, height)
    }

    override fun onDraw() {
        renderer.onDrawFrame(null)
    }

}