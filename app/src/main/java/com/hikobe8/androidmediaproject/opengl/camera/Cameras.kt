package com.hikobe8.androidmediaproject.opengl.camera

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import java.util.*

/***
 *  Author : yurui@palmax.cn
 *  Create at 2019/1/2 10:15
 *  description : 相机接口，包含Camera, Camera2的通用方法
 */
interface ICamera {
    /**
     * 打开相机
     */
    fun openCamera()

    /**
     * 开始预览
     */
    fun startPreview()

    fun stopPreview()

    /**
     * 设置纹理数据
     */
    fun setTexture(surfaceTexture: SurfaceTexture?)

    fun closeCamera()

    /**
     * 切换摄像头
     */
    fun switchCamera()

    /**
     * 设置相机预览的宽高
     * @param width 宽度像素值
     * @param height 高度像素值
     */
    fun setPreviewSize(width: Int, height: Int)

    /**
     * 设置拍摄图片的宽高
     * @param width 宽度像素值
     * @param height 高度像素值
     */
    fun setOutPutSize(width: Int, height: Int)

    fun isFrontCamera():Boolean

    val DEFAULT_OUTPUT_ASPECT_RATIO: Float get() = 1.78f
}

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-29 14:21
 *  description : 使用camera API实现的相机辅助类
 */
class ClassicCamera : ICamera {

    private var mCamera: Camera? = null
    private var mCameraId = 0
    val mPreviewSize = Point()
    val mOutPutSize = Point()
    private var mAspectRatio = DEFAULT_OUTPUT_ASPECT_RATIO
    private var mSurfaceTexture: SurfaceTexture? = null

    override fun setPreviewSize(width: Int, height: Int) {
        mPreviewSize.x = width
        mPreviewSize.y = height
        val params = mCamera?.parameters
        val bestPreviewSize = getBestSize(params?.supportedPreviewSizes!!, mPreviewSize.y, mAspectRatio)
        mPreviewSize.x = bestPreviewSize.width
        mPreviewSize.y = bestPreviewSize.height
        mCamera?.parameters = params
    }

    override fun setOutPutSize(width: Int, height: Int) {
        mOutPutSize.x = width
        mOutPutSize.y = height
        mAspectRatio = height.toFloat() / width
    }

    override fun openCamera() {
        mCamera = Camera.open(mCameraId) //默认打开后置摄像头
        val params = mCamera?.parameters
        val bestPreviewSize = getBestSize(params?.supportedPreviewSizes!!, mPreviewSize.y, mAspectRatio)
        mPreviewSize.x = bestPreviewSize.height
        mPreviewSize.y = bestPreviewSize.width
        params.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height)
        val bestOutputSize = getBestSize(params.supportedPictureSizes, mOutPutSize.y, mAspectRatio)
        mOutPutSize.x = bestOutputSize.height
        mOutPutSize.y = bestOutputSize.width
        params.setPictureSize(bestOutputSize.width, bestOutputSize.height)
        val focusModes = params.supportedFocusModes
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        mCamera?.parameters = params
    }

    override fun startPreview() {
        mCamera?.startPreview()
    }

    override fun stopPreview() {
        mCamera?.stopPreview()
    }

    override fun setTexture(surfaceTexture: SurfaceTexture?) {
        mCamera?.setPreviewTexture(surfaceTexture)
        mSurfaceTexture = surfaceTexture
    }

    override fun closeCamera() {
        try {
            mCamera?.stopPreview()
            mCamera?.release()
        } catch (e: RuntimeException) {

        }
    }

    override fun switchCamera() {
        mCameraId = (mCameraId + 1) % 2
    }

    /**
     * 获取比例一致或者最接近的相机支持的图片的宽高
     * @param supportedPictureSizes 相机支持的图片保存宽高列表
     * @param minOutputWidth 最小保存的图片的宽度，默认为0像素，即取出相机支持的最大宽度，保证图片清晰度
     * @param aspectRatio 图片的宽高比
     */
    private fun getBestSize(
        supportedPictureSizes: MutableList<Camera.Size>,
        minOutputWidth: Int,
        aspectRatio: Float
    ): Camera.Size {
        supportedPictureSizes.apply {
            sortWith(SizeComparator())
            filter { it.width > minOutputWidth && (it.width.toFloat() / it.height - aspectRatio < 0.001) }
        }
        return supportedPictureSizes[0]
    }

    override fun isFrontCamera():Boolean {
        val info:Camera.CameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
    }

    private class SizeComparator : Comparator<Camera.Size> {
        override fun compare(o1: Camera.Size?, o2: Camera.Size?): Int {
            return (o2?.width ?: 0) - (o1?.width ?: 0)
        }

    }

}