package com.hikobe8.androidmediaproject.opengl.egl.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import com.hikobe8.androidmediaproject.opengl.egl.RayEGLSurfaceView

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-27 14:57
 *  description :
 */
class RayCameraView(context: Context?, attrs: AttributeSet?) : RayEGLSurfaceView(context, attrs) {

    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK
    private var mCameraRenderer: RayCameraRenderer
    private var mCamera: RayCamera = RayCamera()

    interface OnTextureCreateListener {
        fun onTextureCreate(textureId: Int, width: Int, height: Int)
    }

    var onTextureCreateListener: OnTextureCreateListener? = null

    init {
        mCameraRenderer = RayCameraRenderer(context!!, mCameraId)
        setRenderer(mCameraRenderer)
        mCameraRenderer.onSurfaceCreateListener = object : RayCameraRenderer.OnSurfaceCreateListener {
            override fun onSurfaceCreate(
                surfaceTexture: SurfaceTexture,
                textureId: Int,
                width: Int,
                height: Int
            ) {
                mCamera.initCamera(surfaceTexture, mCameraId)
                onTextureCreateListener?.onTextureCreate(textureId, width, height)
            }
        }
    }

    fun onDestroy() {
        mCamera.stopPreview()
    }

    fun switch() {
        mCameraId =
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
        mCamera.switchCamera(mCameraId)
        mCameraRenderer.setCameraId(mCameraId)
    }

}