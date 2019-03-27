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

    private val mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK
    private var mCameraRenderer: RayCameraRenderer = RayCameraRenderer(context!!)
    private var mCamera: RayCamera = RayCamera()

    init {
        setRenderer(mCameraRenderer)
        mCameraRenderer.onSurfaceCreateListener = object : RayCameraRenderer.OnSurfaceCreateListener {
            override fun onSurfaceCreate(surfaceTexture: SurfaceTexture) {
                mCamera.initCamera(surfaceTexture, mCameraId)
            }
        }
    }

    fun onDestroy() {
        mCamera.stopPreview()
    }

}