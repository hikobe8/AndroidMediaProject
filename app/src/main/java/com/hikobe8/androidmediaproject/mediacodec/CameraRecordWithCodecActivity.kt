package com.hikobe8.androidmediaproject.mediacodec

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import com.hikobe8.androidmediaproject.*
import kotlinx.android.synthetic.main.activity_camera_record.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class CameraRecordWithCodecActivity : AppCompatActivity(), CameraVideoPreview.OnPreviewStateListener {
    override fun onStartPreview(width: Int, height: Int) {
        initMediaCodec(width, height)
    }

    override fun onStopPreview() {
        stopMediaCodec()
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
            mCamera?.setPreviewCallback { data, _ ->

                var input: ByteArray? = data
                val yuv420sp = ByteArray(mWidth * mHeight * 3 / 2)
                NV21ToNV12(input, yuv420sp, mWidth, mHeight)
                input = yuv420sp
                try {
                    val inputBuffers: Array<ByteBuffer>? = mMediaCodec?.getInputBuffers()//拿到输入缓冲区,用于传送数据进行编码
                    val outputBuffers: Array<ByteBuffer>? = mMediaCodec?.getOutputBuffers()//拿到输出缓冲区,用于取到编码后的数据
                    val inputBufferIndex = mMediaCodec?.dequeueInputBuffer (-1)
                    if (inputBufferIndex!! >= 0) {//当输入缓冲区有效时,就是>=0
                        val inputBuffer = inputBuffers!![inputBufferIndex]
                        inputBuffer.clear()
                        inputBuffer.put(input)//往输入缓冲区写入数据,
                        //                    //五个参数，第一个是输入缓冲区的索引，第二个数据是输入缓冲区起始索引，第三个是放入的数据大小，第四个是时间戳，保证递增就是
                        mMediaCodec?.queueInputBuffer(inputBufferIndex, 0, input.size, System.nanoTime() / 1000, 0)

                    }

                    val bufferInfo = MediaCodec.BufferInfo()
                    var outputBufferIndex = mMediaCodec?.dequeueOutputBuffer (bufferInfo, 0)//拿到输出缓冲区的索引
                    if (outputBufferIndex != null) {
                        while (outputBufferIndex!! >= 0) {
                            val outputBuffer = outputBuffers!![outputBufferIndex]
                            val  outData = ByteArray(bufferInfo.size)
                            outputBuffer.get(outData)
                            //outData就是输出的h264数据
                            outputStream?.write(outData, 0, outData.size)//将输出的h264数据保存为文件，用vlc就可以播放
                            mMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mMediaCodec?.dequeueOutputBuffer(bufferInfo, 0)
                        }
                    }

                } catch (t: Throwable) {
                    t.printStackTrace()
                }

            }
            button_capture.setOnClickListener {
                it as ImageButton
                if (mIsRecording) {
                    tv_duration.text = ""
//                    mMediaRecorder?.stop()
                    mTimeHandler.stop()
//                    releaseMediaRecorder()
                    it.setImageResource(R.drawable.ic_video_call_black_24dp)
                    mIsRecording = false
                    stopMediaCodec()
                    outputStream?.close()
                } else {
                    outputStream = FileOutputStream(getOutputMediaFile())
                    mTimeHandler.start()
//                    if (prepareMediaRecorder()) {
////                        mMediaRecorder?.start()
//                        mTimeHandler.start()
//                        it.setImageResource(R.drawable.ic_stop_48dp)
//                        mIsRecording = true
//                    } else {
//                        releaseMediaRecorder()
//                        mTimeHandler.stop()
//                    }
                }
            }
        }
    }


    private val frameRate: Int = 20

    private var mMediaCodec: MediaCodec? = null

    private var mWidth = 0
    private var mHeight = 0

    private fun initMediaCodec(width: Int, height: Int) {
        mWidth = width
        mHeight = height
        val bitrate = 2 * width * height * frameRate //码率
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc")
            val mediaFormat =
                MediaFormat.createVideoFormat("video/avc", height, width) //height和width一般都是照相机的height和width。
            //描述平均位速率（以位/秒为单位）的键。 关联的值是一个整数
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            //描述视频格式的帧速率（以帧/秒为单位）的键。
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)//帧率，一般在15至30之内，太小容易造成视频卡顿。
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 19)//色彩格式，具体查看相关API，不同设备支持的色彩格式不尽相同
            //关键帧间隔时间，单位是秒
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            mMediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec?.start()//开始编码
        } catch (e: IOException) {
            e.printStackTrace()
        }
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

    private fun getOutputMediaFileUri(): Uri {
        return Uri.fromFile(getOutputMediaFile())
    }

    private fun getOutputMediaFile(): File? {

        return try {
            val videoFile = File(FileUtils.getCameraDir(), "${System.currentTimeMillis()}.mp4")
            if (!videoFile.exists()) {
                videoFile.createNewFile()
            }
            videoFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun NV21ToNV12(nv21: ByteArray?, nv12: ByteArray?, width: Int, height: Int) {
        if (nv21 == null || nv12 == null) return
        val frameSize = width * height
        System.arraycopy(nv21, 0, nv12, 0, frameSize)
        for (i in 0 until frameSize) {
            nv12[i] = nv21[i]
        }
        for (j in 0 until frameSize / 2 step 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize]
        }
        for (j in 0 until frameSize / 2 step 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1]
        }
    }

}
