package com.hikobe8.androidmediaproject.opengl.egl

import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

class RayEGLFactory(private var mEGLContext: EGLContext? = null) : GLSurfaceView.EGLContextFactory {

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    }

    override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
        return if (mEGLContext != null) {
            mEGLContext!!
        } else {
            val attribList = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            mEGLContext = egl.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT,
                attribList
            )
            return mEGLContext!!
        }

    }

    override fun destroyContext(
        egl: EGL10, display: EGLDisplay,
        context: EGLContext
    ) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:$display context: $context")
        }
    }

    fun getEGLContext(): EGLContext? = mEGLContext

}