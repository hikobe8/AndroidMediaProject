package com.hikobe8.androidmediaproject.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ImageButton
import com.hikobe8.androidmediaproject.*
import com.hikobe8.androidmediaproject.audio.AudioRecordPlayActivity
import kotlinx.android.synthetic.main.activity_camera_record.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.getCameraInfo
import android.view.OrientationEventListener



class CameraRecordActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CameraRecordActivity"
        fun launch(context: Context) {
            context.startActivity(Intent(context, CameraRecordActivity::class.java))
        }
    }

    class TimerHandler(activity: CameraRecordActivity) : Handler() {

        private var mWeakReference: WeakReference<CameraRecordActivity> = WeakReference(activity)

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
                activity.tv_duration.text = activity.resources.getString(R.string.text_recording_video, mSeconds.formatTime())
                sendMsg()
            }
        }
    }

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var mMediaRecorder: MediaRecorder? = null
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

    private fun startCameraPreview() {
        if (!CameraUtils.checkCameraHardware(this)) {
            finish()
        } else {
            //Create an instance of Camera
            mCamera = CameraUtils.getCameraInstance()
            mPreview = mCamera?.let {
                CameraUtils.setCameraDisplayOrientation(this, 0, it)
                CameraPreview(this, it)
            }
            mPreview?.also {
                camera_preview.addView(it)
            }
            button_capture.setOnClickListener {
                it as ImageButton
                if (mIsRecording) {
                    tv_duration.text = ""
                    mMediaRecorder?.stop()
                    mTimeHandler.stop()
                    releaseMediaRecorder()
                    mCamera?.lock() // take camera access back from MediaRecorder
                    it.setImageResource(R.drawable.ic_video_call_black_24dp)
                    mIsRecording = false
                } else {
                    if (prepareMediaRecorder()) {
                        mMediaRecorder?.start()
                        mTimeHandler.start()
                        it.setImageResource(R.drawable.ic_stop_48dp)
                        mIsRecording = true
                    } else {
                        releaseMediaRecorder()
                        mTimeHandler.stop()
                    }
                }
            }
        }
    }

    private fun prepareMediaRecorder(): Boolean {
        mMediaRecorder = MediaRecorder()

        mCamera?.let {
            // Step 1: Unlock and set camera to MediaRecorder
            it.unlock()

            mMediaRecorder?.run {

                setCamera(mCamera)
                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
                // Step 4: Set output file
                setOutputFile(getOutputMediaFile()?.absolutePath)
                // Step 5: Set the preview output
                setPreviewDisplay(mPreview?.holder?.surface)
                // Step 6: Prepare configured MediaRecorder
                setVideoSize(1920, 1080)
                // See android.hardware.Camera.Parameters.setRotation for
                // documentation.
                // Note that mOrientation here is the device orientation, which is the opposite of
                // what activity.getWindowManager().getDefaultDisplay().getRotation() would return,
                // which is the orientation the graphics need to rotate in order to render correctly.
                val mOrientation = windowManager.defaultDisplay
                    .rotation
                var rotation = 0
                val info = android.hardware.Camera.CameraInfo()
                val mCameraId = 0
                android.hardware.Camera.getCameraInfo(mCameraId, info)
                if (mOrientation !== OrientationEventListener.ORIENTATION_UNKNOWN) {
                    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                        rotation = (info.orientation - mOrientation + 360) % 360
                    } else {  // back-facing camera
                        rotation = (info.orientation + mOrientation) % 360
                    }
                } else {
                    //Get the right original orientation
                    rotation = info.orientation
                }
                //        mMediaRecorder.setOrientationHint(rotation);

                if (mCameraId === CameraInfo.CAMERA_FACING_FRONT) {
                    if (rotation == 270 || rotation == 90 || rotation == 180) {
                        setOrientationHint(180)
                    } else {
                        setOrientationHint(0)
                    }
                } else {
                    if (rotation == 180) {
                        setOrientationHint(180)
                    } else {
                        setOrientationHint(0)
                    }
                }


                return try {
                    prepare()
                    true
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "IllegalStateException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                } catch (e: IOException) {
                    Log.d(TAG, "IOException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                }
            }
        }

        return false
    }

    private fun releaseMediaRecorder() {
        mMediaRecorder?.reset()
        mMediaRecorder?.release()
        mMediaRecorder = null
        mCamera?.lock()
    }

    private fun releaseCamera(){
        mCamera?.release()
        mCamera = null
    }

    override fun onPause() {
        super.onPause()
        releaseMediaRecorder()
        releaseCamera()
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

}
