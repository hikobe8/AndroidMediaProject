package com.hikobe8.androidmediaproject.camera.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.Face
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import com.hikobe8.androidmediaproject.FileUtils
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.inflate
import kotlinx.android.synthetic.main.activity_camera2_basic.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-17 16:44
 *  description :
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2BasicFragment : Fragment() {

    companion object {
        const val TAG = "Camera2BasicFragment"
        const val MAX_PREVIEW_WIDTH = 1920
        const val MAX_PREVIEW_HEIGHT = 1080

        /**
         * 得到
         */
        fun chooseOptimalSize(
            choices: Array<Size>, textureViewWidth: Int,
            textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
        ): Size? {
            val bigEnough = ArrayList<Size>()
            val notBigEnough = ArrayList<Size>()

            val w = aspectRatio.width
            val h = aspectRatio.height

            for (choice in choices) {

                if (choice.width <= maxWidth && choice.height <= maxHeight
                    && (choice.height - choice.width * h / w * 1f) < 0.001f)
                {
                    if (choice.width >= textureViewWidth && choice.height >= textureViewHeight) {
                        bigEnough.add(choice)
                    } else {
                        notBigEnough.add(choice)
                    }
                }

            }

            return when {
                bigEnough.size > 0 -> {
                    Collections.min(bigEnough, CompareSizesByArea())
                }
                notBigEnough.size > 0 -> {
                    Collections.max(notBigEnough, CompareSizesByArea())
                }
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }

        }

        private val ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }

    }

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            openCamera(width, height)
        }

    }

    /**
     * 用于静态图片获取，可以理解为拍照
     */
    private var mImageReader: ImageReader? = null

    private var mFile: File? = null

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener {
        mBackgroundHandler?.post(ImageSaver(it.acquireNextImage(), mFile))
    }

    /**
     * 保存图片的任务
     */
    private class ImageSaver(private val image: Image, private val file: File?) : Runnable {

        override fun run() {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(file)
                output.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                image.close()
                try {
                    output?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    }

    private var mSensorOrientation: Int = 0

    /**
     * 相机预览区域尺寸
     */
    private var mPreviewSize: Size? = null

    private var mCameraId = ""

    /**
     * 是否支持闪光灯
     */
    private var mFlashSupported: Boolean = false

    private var mCameraDevice: CameraDevice? = null

    private val mCameraOpenCloseLock = Semaphore(1)

    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            mCameraOpenCloseLock.release()
            mCameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            camera.close()
            mCameraDevice = null
            activity?.finish()
        }

    }

    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    private var mCaptureSession: CameraCaptureSession? = null

    private var mPreviewRequest: CaptureRequest? = null

    private var mOpenFrontCamera = false //默认打开后置摄像头

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {

            val mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
            val faces: Array<Face>? = result.get(CaptureResult.STATISTICS_FACES)
            if (faces != null && mode != null) {
                if (faces.isNotEmpty()) {
                    Log.i(TAG, "faces : " + faces.size + " , mode : " + mode)
                    Log.e(TAG, "${faces[0].bounds}, score = ${faces[0].score}")
                    val detectionResult = RectF()
                    mFaceDetectionMatrix.mapRect(detectionResult, RectF(faces[0].bounds))
                    face_detection_view.update(detectionResult)
                }
            }

        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            process(result)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
            process(partialResult)
        }
    }

    private lateinit var mFaceDetectionMatrix: Matrix

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.activity_camera2_basic, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener {
            takePicture()
        }
        ib_switch.setOnClickListener {
            mOpenFrontCamera = !mOpenFrontCamera
            stopBackgroundThread()
            closeCamera()
            if (textureview.isAvailable) {
                openCamera(textureview.width, textureview.height)
            } else {
                textureview.surfaceTextureListener = mSurfaceTextureListener
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mFile = File(FileUtils.getCameraDir(), "camera2.jpeg")
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureview.isAvailable) {
            openCamera(textureview.width, textureview.height)
        } else {
            textureview.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession?.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice?.close()
                mCameraDevice = null
            }
            if (null != mImageReader) {
                mImageReader?.close()
                mImageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * 初始化并开启后台线程和对应的Handler
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.let {
            it.start()
            mBackgroundHandler = Handler(it.looper)
        }
    }

    /**
     * 关闭后台线程
     */
    private fun stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread?.quitSafely()
        } else {
            mBackgroundThread?.quit()
        }
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs(width, height)
        val manager: CameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager: CameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics: CameraCharacteristics = manager.getCameraCharacteristics(cameraId)
                //打开前置摄像头
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == if (mOpenFrontCamera) CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map: StreamConfigurationMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                //调整屏幕与图像传感器的角度差
                val displayRotation = activity?.windowManager?.defaultDisplay?.rotation
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                var swappedDimensions = false
                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> {
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true
                        }
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> {
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true
                        }
                    }
                    else ->
                        Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }
                val displaySize = Point()
                //使用屏幕的宽高
                activity?.windowManager?.defaultDisplay?.getSize(displaySize)
                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y

                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                //使用最大的尺寸保证图片清晰度最高，并且拍摄的图片宽高比要和预览一致
                val filter = map.getOutputSizes(ImageFormat.JPEG)
                    .filter {
                        Math.abs((it.width - it.height * rotatedPreviewWidth * 1f / rotatedPreviewHeight)) < 0.1f
                    }
                val largest = if (filter.isEmpty()) map.getOutputSizes(ImageFormat.JPEG)[0]
                    else  Collections.max(filter, CompareSizesByArea())
                mImageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2)
                mImageReader?.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)

                //设置预览尺寸
                mPreviewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest
                )

                val orientationOffset = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val activeArraySizeRect =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

                // Need mirror for front camera.
                mFaceDetectionMatrix = Matrix()
                val mirror = facing == CameraCharacteristics.LENS_FACING_FRONT
                mFaceDetectionMatrix.setRotate(orientationOffset?.toFloat() ?: 0f)

                val s1Left = mPreviewSize?.width?.toFloat() ?: 1f
                val s1Right = activeArraySizeRect?.width()?.toFloat() ?: 1f
                val s1 = s1Left / s1Right

                val s2Left = mPreviewSize?.height?.toFloat() ?: 1f
                val s2Right = activeArraySizeRect?.height()?.toFloat() ?: 1f
                val s2 = s2Left / s2Right
                mFaceDetectionMatrix.postScale(if (mirror) -s1 else s1, s2)
                mFaceDetectionMatrix.postTranslate(mPreviewSize?.height?.toFloat()?:1f, mPreviewSize?.width?.toFloat()?:1f)
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureview.setAspectRatio(mPreviewSize?.width!!, mPreviewSize?.height!!)
                } else {
                    textureview.setAspectRatio(mPreviewSize?.height!!, mPreviewSize?.width!!)
                }

                // Check if the flash is supported.
                val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                mFlashSupported = available ?: false

                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            //设备不支持Camera2 API可能会报空指针
            e.printStackTrace()
        }
    }

    /**
     * 创建预览会话
     */
    private fun createCameraPreviewSession() {
        try {
            val texture = textureview.surfaceTexture!!

            texture.setDefaultBufferSize(mPreviewSize?.width!!, mPreviewSize?.height!!)

            //预览的surface
            val surface = Surface(texture)
            mPreviewRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder?.addTarget(surface)

            mCameraDevice?.createCaptureSession(
                Arrays.asList(surface, mImageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        //show a toast
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        if (null == mCameraDevice) {
                            return
                        }

                        mCaptureSession = session

                        try {
//                            mPreviewRequestBuilder?.set(
//                                CaptureRequest.CONTROL_AF_MODE,
//                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//                            )
                            mPreviewRequestBuilder?.set(
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL
                            )
                            setAutoFlash(mPreviewRequestBuilder)
                            mPreviewRequest = mPreviewRequestBuilder?.build()
                            mPreviewRequest?.let {
                                mCaptureSession?.setRepeatingRequest(it, mCaptureCallback, mBackgroundHandler)
                            }
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }

                    }

                }, null
            )

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder?) {
        if (mFlashSupported) {
            requestBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    class CompareSizesByArea : Comparator<Size> {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun compare(o1: Size?, o2: Size?): Int {
            return o1?.width!! * o1.height - o2?.width!! * o2.height
        }
    }

    private fun takePicture() {
        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(mImageReader?.surface!!)

            // Use the same AE and AF modes as the preview.
            if (captureBuilder?.get(CaptureRequest.CONTROL_AF_MODE) == CaptureRequest.CONTROL_AF_MODE_AUTO)
                captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            //设置拍摄的图片的方向
            captureBuilder?.set(
                CaptureRequest.JPEG_ORIENTATION,
                //当前应用屏幕方向锁死为竖屏， 所以根据传感器方向来确定图片方向
                getOrientation((activity as Camera2BasicActivity).mOritentation)
            )
            setAutoFlash(captureBuilder)
            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    showToast("Saved: $mFile")
                    Log.d(TAG, mFile.toString())
                }
            }
            mCaptureSession?.capture(captureBuilder?.build()!!, captureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Shows a [Toast] on the UI thread.
     *
     * @param text The message to show
     */
    private fun showToast(text: String) {
        val activity = activity
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    private fun getOrientation(orientation: Int): Int {
        return (ORIENTATIONS.get(orientation) + mSensorOrientation + 360) % 360
    }

}