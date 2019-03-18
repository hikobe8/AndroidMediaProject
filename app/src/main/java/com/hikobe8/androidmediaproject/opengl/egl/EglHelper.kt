package com.hikobe8.androidmediaproject.opengl.egl

import android.util.Log
import android.view.Surface
import javax.microedition.khronos.egl.*

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-15 17:31
 *  description :
 */
class EglHelper {

    private var mEgl: EGL10? = null
    private var mEglDisplay: EGLDisplay? = null
    private var mEglContext: EGLContext? = null
    private var mEglSurface: EGLSurface? = null

    fun start(surface: Surface, eglContext: EGLContext?) {
        /*
         * Get an EGL instance
         */
        mEgl = EGLContext.getEGL() as EGL10
        /*
         * Get to the default display.
         */
        mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }
        /*
         * We can now initialize EGL for that display
         */
        val version = IntArray(2)
        if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
            throw RuntimeException("eglInitialize failed")
        }

        val attributes = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 8,
            EGL10.EGL_STENCIL_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, 4, // OpenglES 2.0 默认 EGL14.EGL_OPENGL_ES2_BIT
            EGL10.EGL_NONE
        )
        val num_config = IntArray(1)
        if (!mEgl!!.eglChooseConfig(
                mEglDisplay, attributes, null, 0,
                num_config
            )
        ) {
            throw IllegalArgumentException("eglChooseConfig failed")
        }
        val numConfigs = num_config[0]

        if (numConfigs <= 0) {
            throw IllegalArgumentException(
                "No configs match configSpec"
            )
        }
        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        if (!mEgl!!.eglChooseConfig(
                mEglDisplay, attributes, configs, numConfigs,
                num_config
            )
        ) {
            throw IllegalArgumentException("eglChooseConfig#2 failed")
        }

        mEglContext = if (eglContext == null) {
            mEgl!!.eglCreateContext(
                mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT,
                null
            )
        } else {
            mEgl!!.eglCreateContext(
                mEglDisplay, configs[0], eglContext,
                null
            )
        }
        //create surface
        mEglSurface = mEgl!!.eglCreateWindowSurface(mEglDisplay, configs[0], surface, null)

        if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
            Log.e("EGLHelper", "eglMakeCurrent " + mEgl!!.eglGetError())
        }

    }

    fun swapBuffers(): Boolean {
        return mEgl?.eglSwapBuffers(mEglDisplay, mEglSurface) ?: throw java.lang.RuntimeException("egl is null")
    }

    fun getContext(): EGLContext? = mEglContext

    fun destroy(){
        mEgl?.eglMakeCurrent(
            mEglDisplay, EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        mEgl?.eglDestroySurface(mEglDisplay, mEglSurface)
        mEglSurface = null
        if (mEgl?.eglDestroyContext(mEglDisplay, mEglContext) == true) {
            Log.e("DefaultContextFactory", "display:$mEglDisplay context: $mEglContext")
        }
        mEglContext = null
        mEgl?.eglTerminate(mEglDisplay)
        mEglDisplay = null
    }

}