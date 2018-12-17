package com.hikobe8.androidmediaproject.camera

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException
import java.lang.Exception

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-12 10:28
 *  description : 使用Camera实现的相机预览view
 */
class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        const val TAG = "CameraPreview"
    }

    private val mHolder = holder.apply {
        addCallback(this@CameraPreview)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mCamera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
                this@CameraPreview.startFaceDetection()
            } catch (e: IOException) {
                Log.d(TAG, "Error setting camera preview: ${e.message}")
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (mHolder.surface == null) {
            return
        }
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        val parameters = mCamera.parameters
        val optimalSize = getOptimalSize(mCamera.parameters.supportedPreviewSizes, width, height)
        parameters?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        optimalSize?.let { parameters.setPreviewSize(it.width, it.height) }
        mCamera.parameters = parameters
        // start preview with new settings
        mCamera.apply {
            try {
                setPreviewDisplay(mHolder)
                startPreview()
                this@CameraPreview.startFaceDetection()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {

    }

    private fun startFaceDetection(){
        val params = mCamera.parameters

        params.apply {
            if (maxNumDetectedFaces > 0) {
                mCamera.startFaceDetection()
            }
        }
    }

    private fun getOptimalSize(sizes:List<Camera.Size>, w:Int, h:Int):Camera.Size? {
        val aspectTolerance = 0.1
        val targetRatio =  h.toDouble() / w
        var optimalSize:Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }

        return optimalSize
    }

}