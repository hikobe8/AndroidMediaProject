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
//        mCamera = Camera.open(cameraId)
//        mCamera?.let {
//            mCamera!!.setPreviewTexture(mSurfaceTexture)
//            val params = mCamera!!.parameters.apply {
//                flashMode = Camera.Parameters.FLASH_MODE_OFF
//                previewFormat = ImageFormat.NV21
//                setPreviewSize(supportedPreviewSizes[0].width, supportedPreviewSizes[0].height)
//                setPictureSize(supportedPictureSizes[0].width, supportedPictureSizes[0].height)
//            }
//            mCamera!!.parameters =  params
//            mCamera!!.startPreview()
//        }
        mCamera = Camera.open(cameraId)
        mCamera!!.setPreviewTexture(mSurfaceTexture)
        val parameters = mCamera!!.getParameters()

        parameters.setFlashMode("off")
        parameters.setPreviewFormat(ImageFormat.NV21)

        parameters.setPictureSize(
            parameters.getSupportedPictureSizes().get(0).width,
            parameters.getSupportedPictureSizes().get(0).height
        )
        parameters.setPreviewSize(
            parameters.getSupportedPreviewSizes().get(0).width,
            parameters.getSupportedPreviewSizes().get(0).height
        )

        mCamera!!.setParameters(parameters)
        mCamera!!.startPreview()
    }

    fun stopPreview() {
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
    }

    fun switchCamera() {

    }

}