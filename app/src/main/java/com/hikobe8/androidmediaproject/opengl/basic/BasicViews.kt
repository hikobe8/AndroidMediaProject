package com.hikobe8.androidmediaproject.opengl.basic

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BasicShapeGLSurfaceView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {

    constructor(context: Context?):this(context, null)

    private val mRenderer:BasicRenderer

    init {
        setEGLContextClientVersion(2)
        mRenderer = BasicRenderer(TriangleShape(context))
        setRenderer(mRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setShape(shape: Shape){
        mRenderer.setShape(shape)
    }

}

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-27 14:03
 *  description :
 */
class BasicRenderer(shape: Shape):GLSurfaceView.Renderer{

    private var mShape = shape

    override fun onDrawFrame(gl: GL10?) {
        mShape.onDrawFrame(gl)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mShape.onSurfaceChanged(gl,width,height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mShape.onSurfaceCreated(gl,config)
    }

    fun setShape(shape: Shape) {
        mShape = shape
    }

}