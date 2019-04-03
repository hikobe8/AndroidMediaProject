package com.hikobe8.androidmediaproject.opengl.record

import android.content.Intent
import android.media.MediaFormat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.egl.camera.RayCameraView
import com.hikobe8.androidmediaproject.show
import kotlinx.android.synthetic.main.activity_video_record.*
import java.io.File

class VideoRecordActivity : AppCompatActivity() {

    companion object {
        fun launch(context: AppCompatActivity) {
            context.startActivity(Intent(context, VideoRecordActivity::class.java))
        }
    }

    private var mVideoEncodec: RayMediaEncodec? = null
    private var mState = -1 // -1 未就绪 0 准备就绪，等待开始 1 正在录制


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        camera_view.onTextureCreateListener = object : RayCameraView.OnTextureCreateListener {
            override fun onTextureCreate(textureId: Int, width: Int, height: Int) {
                mVideoEncodec = RayMediaEncodec(this@VideoRecordActivity, textureId)
                mVideoEncodec!!.initEncodec(camera_view.getEGLContext()!!,
                    Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4",
                    MediaFormat.MIMETYPE_VIDEO_AVC, width, height
                )
                mState = 0 // 视频录制准备工作就绪
                mVideoEncodec!!.onProgressChangeListener = object :RayBaseMediaEncoder.ProgressChangeListener{
                    override fun onProgressChange(seconds: Int) {
                        Log.d("录制中", "$seconds 秒")
                        runOnUiThread {
                            btn_record.text = "正在录制: $seconds 秒"
                        }
                    }

                }
            }
        }
        btn_record.setOnClickListener {
            if (mState < 0) {
                "尚未就绪".show(this)
            } else {
                if (mState == 0) {
                    mState = 1
                    btn_record.text = "正在录制"
                    mVideoEncodec?.startRecord()
                } else {
                    mState = 0
                    mVideoEncodec!!.stopRecord()
                    mVideoEncodec = null
                    btn_record.text = "开始"
                    "${Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4"} 录制完成".show(this)
                }
            }
        }
    }
}
