package com.hikobe8.androidmediaproject

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hikobe8.androidmediaproject.audio.AudioRecordPlayActivity
import com.hikobe8.androidmediaproject.camera.CameraBasicActivity
import com.hikobe8.androidmediaproject.camera.CameraRecordActivity
import com.hikobe8.androidmediaproject.camera.camera2.Camera2BasicActivity
import com.hikobe8.androidmediaproject.draw.DrawImageActivity
import com.hikobe8.androidmediaproject.draw.PorterDuffDemoActivity
import com.hikobe8.androidmediaproject.media.ExtractMuteVideoActivity
import com.hikobe8.androidmediaproject.mediacodec.AudioRecordPlayWithCodecActivity
import com.hikobe8.androidmediaproject.mediacodec.CameraRecordWithCodecActivity
import com.hikobe8.androidmediaproject.mediacodec.EncodeImages2VideoActivity
import com.hikobe8.androidmediaproject.mediacodec.MediaCodecDecodeActivity
import com.hikobe8.androidmediaproject.opengl.OpenGLMainActivity

class MainActivity : AppCompatActivity() {

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
                PermissionUtils.requestPermission(this, permissionArray.toArray(arrayOf()))
            }
        } else {
            initViews()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtils.isAllPermissionsGranted(requestCode, grantResults)) {
            initViews()
        } else {
            "权限获取失败".show(this)
            finish()
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_main)
    }

    fun go2DrawImageByView(view: View) {
        DrawImageActivity.launch(this)
    }

    fun go2PorterDuff(view: View) {
        PorterDuffDemoActivity.launch(this)
    }

    fun go2AudioRecordPlay(view: View) {
        AudioRecordPlayActivity.launch(this)
    }

    fun go2CameraCapture(view: View) {
        CameraBasicActivity.launch(this)
    }

    fun go2CameraRecord(view: View) {
        CameraRecordActivity.launch(this)
    }

    fun go2MP4Extraction(view: View) {
        ExtractMuteVideoActivity.launch(this)
    }

    fun go2Camera2(view: View) {
        Camera2BasicActivity.launch(this)
    }

    fun go2AudioRecordPlayAAC(view: View) {
        AudioRecordPlayWithCodecActivity.launch(this)
    }

    fun go2RecordH264(view: View) {
        CameraRecordWithCodecActivity.launch(this)
    }

    fun go2OpenGL(view: View) {
        OpenGLMainActivity.launch(this)
    }

    fun go2DecodeByCodec(view: View) {
        MediaCodecDecodeActivity.launch(this)
    }

    fun go2EncodeImagesToVideo(view: View) {
        EncodeImages2VideoActivity.launch(this)
    }

}
