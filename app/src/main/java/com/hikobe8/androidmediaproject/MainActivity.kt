package com.hikobe8.androidmediaproject

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
