package com.hikobe8.androidmediaproject.opengl.yuv

import android.content.Context
import android.util.AttributeSet
import com.hikobe8.androidmediaproject.opengl.egl.RayEGLSurfaceView

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-06-24 18:08
 *  description :
 */
class YUVView(context: Context?, attrs: AttributeSet?) : RayEGLSurfaceView(context, attrs) {

    private var yuvRender = YUVRender(context!!)

    init {
        setRenderer(yuvRender)
        setRenderMode(RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }

    fun setFrameData(w:Int, h:Int, by:ByteArray, bu:ByteArray, bv:ByteArray) {
        yuvRender.setFrameData(w, h, by, bu, bv)
        requestRender()
    }

}