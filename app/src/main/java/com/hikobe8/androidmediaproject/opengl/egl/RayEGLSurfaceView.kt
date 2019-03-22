package com.hikobe8.androidmediaproject.opengl.egl

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

abstract class RayEGLSurfaceView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {

    companion object {
        /**
         * The renderer only renders
         * when the surface is created, or when [.requestRender] is called.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         * @see .requestRender
         */
        const val RENDERMODE_WHEN_DIRTY = 0
        /**
         * The renderer is called
         * continuously to re-render the scene.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         */
        const val RENDERMODE_CONTINUOUSLY = 1

        const val TAG = "RayGLThread"
    }

    constructor(context: Context?) : this(context, null)

    private var mGLThread: GLThread? = null
    private var mSurface: Surface? = null
    private var mEglContext: EGLContext? = null

    init {
        holder.addCallback(this@RayEGLSurfaceView)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mSurface = holder?.surface
        mGLThread = GLThread(WeakReference(this))
        mGLThread?.mCreated = true
        mGLThread!!.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mGLThread?.mWidth = width
        mGLThread?.mHeight = height
        mGLThread?.mSizeChanged = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mSurface = null
        mGLThread?.onDestroy()
    }

    private var mRenderer: RayRenderer? = null
    private var mRendererMode =
        RENDERMODE_CONTINUOUSLY

    fun setSurfaceAndEGLContext(surface: Surface?, eglContext: EGLContext?) {
        mSurface = surface
        mEglContext = eglContext
    }

    fun setRenderer(renderer: RayRenderer) {
        if (mRenderer != null) {
            throw RuntimeException("setRenderer can't called more than once")
        }
        mRenderer = renderer

    }

    fun setRenderMode(renderMode: Int) {
        if (mRenderer == null) {
            throw RuntimeException("renderer is null")
        }
        mRendererMode = renderMode
    }

    fun requestRender() {
        mGLThread?.requestRender()
    }

    fun getEGLContext() = mGLThread?.getEGLContext()

    class GLThread(private val mSurfaceViewWeakRef: WeakReference<RayEGLSurfaceView>) : Thread() {

        private var mExit = false
        var mCreated = false
        var mSizeChanged = false
        private var mEglHelper: EglHelper? = null
        private val mLock = Object()
        var mWidth: Int = 0
        var mHeight: Int = 0
        private var mIsStart = false

        override fun run() {

            mIsStart = false
            mEglHelper = EglHelper()
            mSurfaceViewWeakRef.get()!!.mSurface?.let {
                Log.d(TAG, "Thread name = $name : EGLHelper created")
                mEglHelper!!.start(mSurfaceViewWeakRef.get()!!.mSurface!!, mSurfaceViewWeakRef.get()!!.mEglContext)
            }

            while (true) {

                if (mExit) {
                    release()
                    return
                }

                if (mIsStart) {
                    when {
                        mSurfaceViewWeakRef.get()!!.mRendererMode == RENDERMODE_WHEN_DIRTY -> synchronized(mLock) {
                            mLock.wait()
                        }
                        mSurfaceViewWeakRef.get()!!.mRendererMode == RENDERMODE_CONTINUOUSLY -> Thread.sleep(1000 / 60)
                        else -> throw RuntimeException("renderMode = ${mSurfaceViewWeakRef.get()!!.mRendererMode} is not supported!")
                    }
                }

                onCreate()
                onSizeChanged(mWidth, mHeight)
                onDraw()

                mIsStart = true
            }
        }

        fun requestRender() {
            if (mSurfaceViewWeakRef.get()!!.mRendererMode == RENDERMODE_WHEN_DIRTY) {
                synchronized(mLock) {
                    mLock.notifyAll()
                }
            }
        }

        private fun onCreate() {
            if (mCreated && mSurfaceViewWeakRef.get() != null) {
                Log.d(TAG, "onCreate")
                mCreated = false
                mSurfaceViewWeakRef.get()!!.mRenderer?.onSurfaceCreated()
            }
        }

        private fun onSizeChanged(width: Int, height: Int) {
            if (mSizeChanged && mSurfaceViewWeakRef.get() != null) {
                Log.d(TAG, "onSizeChanged width = $width, height = $height")
                mSizeChanged = false
                mSurfaceViewWeakRef.get()!!.mRenderer?.onSurfaceSizeChanged(width, height)
            }
        }

        private fun onDraw() {
            Log.d(TAG, "onDraw")
            mSurfaceViewWeakRef.get()!!.mRenderer?.onDraw()
            if (!mIsStart) {
                mSurfaceViewWeakRef.get()?.mRenderer?.onDraw()
            }
            mEglHelper?.swapBuffers()
        }

        fun getEGLContext() = mEglHelper?.getContext()

        fun onDestroy() {
            mExit = true
            requestRender()
        }

        private fun release() {
            mEglHelper?.destroy()
            mEglHelper = null
            mSurfaceViewWeakRef.clear()
        }

    }

}