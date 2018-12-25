package com.hikobe8.androidmediaproject.mediacodec

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaCodec
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import com.hikobe8.androidmediaproject.CameraUtils
import com.hikobe8.androidmediaproject.PermissionUtils
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.formatTime
import kotlinx.android.synthetic.main.activity_camera_record.*
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraRecordWithCodecActivity : AppCompatActivity(), CameraVideoPreview.OnPreviewStateListener {

    private var mCodecInited = false

    override fun onStartPreview(width: Int, height: Int) {
        if (width == 0 || height == 0 || mCodecInited)
            return
        initMediaCodec(width, height)
        mCodecInited = true
    }

    override fun onStopPreview() {
//        stopMediaCodec()
    }

    private fun stopMediaCodec() {
        mMediaCodec?.stop()
        mMediaCodec?.release()
        mMediaCodec = null
    }

    companion object {
        const val TAG = "CameraRecordActivity"
        fun launch(context: Context) {
            context.startActivity(Intent(context, CameraRecordWithCodecActivity::class.java))
        }
    }

    class TimerHandler(activity: CameraRecordWithCodecActivity) : Handler() {

        private var mWeakReference: WeakReference<CameraRecordWithCodecActivity> = WeakReference(activity)

        private var mSeconds = 0L

        fun start() {
            sendMsg()
        }

        fun stop() {
            mSeconds = 0
            removeMessages(0)
        }

        private fun sendMsg() {
            mSeconds++
            sendEmptyMessageDelayed(0, 1000L)
        }

        @SuppressLint("StringFormatMatches")
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val activity = mWeakReference.get()
            if (activity != null) {
                activity.tv_duration.text =
                        activity.resources.getString(R.string.text_recording_video, mSeconds.formatTime())
                sendMsg()
            }
        }
    }

    private var mCamera: Camera? = null
    private var mPreview: CameraVideoPreview? = null
    private val mTimeHandler: TimerHandler by lazy {
        TimerHandler(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionArray: ArrayList<String> = ArrayList()
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.CAMERA)) {
                permissionArray.add(Manifest.permission.CAMERA)
            }
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
                permissionArray.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissionArray.size < 1) {
                initViews()
            } else {
                PermissionUtils.requestPermission(this, permissionArray.toArray(arrayOf()), 10)
            }
        } else {
            initViews()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.size > 1) {
            initViews()
        } else {
            onBackPressed()
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_camera_record)
        startCameraPreview()
    }

    private var mIsRecording = false

    private var outputStream: FileOutputStream?=null

    private fun startCameraPreview() {
        if (!CameraUtils.checkCameraHardware(this)) {
            finish()
        } else {
            //Create an instance of Camera
            mCamera = CameraUtils.getCameraInstance()
            mPreview = mCamera?.let {
                CameraUtils.setCameraDisplayOrientation(this, 0, it)
                CameraVideoPreview(this, it)
            }
            mPreview?.also {
                camera_preview.addView(it)
            }
            mPreview?.mOnPreviewStateListener = this
            button_capture.setOnClickListener {
                it as ImageButton
                if (mIsRecording) {
                    tv_duration.text = ""
                    mTimeHandler.stop()
                    it.setImageResource(R.drawable.ic_video_call_black_24dp)
                    mIsRecording = false
                    outputStream?.close()
                } else {
                    mTimeHandler.start()
                    mIsRecording = true
                    it.setImageResource(R.drawable.ic_stop_48dp)
                }
            }
        }
    }


    private val frameRate: Int = 20

    private var mMediaCodec: MediaCodec? = null

    private var mWidth = 0
    private var mHeight = 0

    private var avcCodec: AvcEncoder? = null

    var YUVQueue = ArrayBlockingQueue<ByteArray>(10)

    private fun initMediaCodec(width: Int, height: Int) {
        avcCodec = AvcEncoder(YUVQueue, width, height, 30, 8500 * 1000)
        avcCodec?.StartEncoderThread()
        mCamera?.setPreviewCallback { data, _ ->

            if (mIsRecording) {
                putYUVData(data, data.size)
            }

        }

    }

    fun putYUVData(buffer: ByteArray, length: Int) {
        if (YUVQueue.size >= 10) {
            YUVQueue.poll()
        }
        YUVQueue.add(buffer)
    }

    private fun releaseCamera() {
        mCamera?.release()
        mCamera = null
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
        stopMediaCodec()
    }

}
