package com.hikobe8.androidmediaproject.opengl.egl.camera

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-27 15:05
 *  description : Camera API 实现，已标记过时的API
 */
class RayCamera {

    private var mCamera: Camera? = null

    private var mSurfaceTexture: SurfaceTexture? = null

    fun initCamera(surfaceTexture: SurfaceTexture, cameraId: Int) {
        mSurfaceTexture = surfaceTexture
        startPreview(cameraId)
    }

    private fun startPreview(cameraId: Int) {
        mCamera = Camera.open(cameraId)
        mCamera?.let {
            mCamera!!.setPreviewTexture(mSurfaceTexture)
            val params = mCamera!!.parameters.apply {
                flashMode = Camera.Parameters.FLASH_MODE_OFF
                previewFormat = ImageFormat.NV21
                setPreviewSize(supportedPreviewSizes[0].width, supportedPreviewSizes[0].height)
                setPictureSize(supportedPictureSizes[0].width, supportedPictureSizes[0].height)
                val focusModes = supportedFocusModes
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                }
            }
            mCamera!!.parameters =  params
            mCamera!!.startPreview()
        }
    }

    fun stopPreview() {
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
    }

    fun switchCamera(cameraId: Int) {
        stopPreview()
        startPreview(cameraId)
    }

}