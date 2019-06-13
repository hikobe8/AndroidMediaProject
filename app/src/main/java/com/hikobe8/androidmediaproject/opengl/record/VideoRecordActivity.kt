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
import com.hikobe8.audio_extractor.AudioDecoder
import kotlinx.android.synthetic.main.activity_video_record.*
import java.io.File

class VideoRecordActivity : AppCompatActivity() {

    companion object {
        const val RECORD_DURATION = 15
        fun launch(context: AppCompatActivity) {
            context.startActivity(Intent(context, VideoRecordActivity::class.java))
        }
    }

    private var mVideoEncodec: RayMediaEncodec? = null
    private var mState = -1 // -1 未就绪 0 准备就绪，等待开始 1 正在录制
    private var mSampleRate = -1
    private var mChannelCount = -1
    private var mTextureId = -1
    private var mSurfaceWidth = -1
    private var mSurfaceHeight = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        camera_view.onTextureCreateListener = object : RayCameraView.OnTextureCreateListener {
            override fun onTextureCreate(textureId: Int, width: Int, height: Int) {
                mTextureId = textureId
                mSurfaceWidth = width
                mSurfaceHeight = height
                if (mSampleRate > 0 && mChannelCount > 0 && mVideoEncodec == null) {
                    initMediaCodec()
                }
            }

        }
        btn_record.setOnClickListener {
            if (mState < 0) {
                AudioDecoder
                    .newInstance().apply {
                        setNeedPlay(false)
                        setPreparedListener(object : AudioDecoder.DecoderPreparedListener {
                            override fun onPrepared() {
                                start()
                            }
                        })
                        setStartPositionAndDuration(20, RECORD_DURATION)
                        setAudioPCMCallback(object : AudioDecoder.AudioPCMInfoCallback {
                            override fun onGetPCMInfo(sampleRate: Int, bitRate: Int, channelCount: Int) {
                                mSampleRate = sampleRate
                                mChannelCount = channelCount
                                if (mSurfaceWidth > 0 && mSurfaceHeight > 0 && mTextureId > 0 && mVideoEncodec == null) {
                                    initMediaCodec()
                                }
                                mState = 1
                                runOnUiThread {
                                    btn_record.text = "正在录制"
                                }
                                mVideoEncodec?.startRecord()
                            }

                            override fun onComplete() {

                            }

                            override fun onGetPCMChunk(pcmBuffer: ByteArray) {
                                mVideoEncodec?.putPCMData(pcmBuffer, pcmBuffer.size)
                            }

                        })
                    }.prepare()
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
                    "${Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4"} 录制完成".show(
                        this
                    )
                }
            }
        }
    }

    private fun initMediaCodec() {
        mVideoEncodec = RayMediaEncodec(this@VideoRecordActivity, mTextureId)
        mVideoEncodec!!.initEncodec(
            camera_view.getEGLContext()!!,
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4",
            MediaFormat.MIMETYPE_VIDEO_AVC, mSurfaceWidth, mSurfaceHeight, mSampleRate, mChannelCount
        )
        mState = 0 // 视频录制准备工作就绪
        mVideoEncodec!!.onProgressChangeListener = object : RayBaseMediaEncoder.ProgressChangeListener {
            override fun onProgressChange(seconds: Int) {
                Log.d("录制中", "$seconds 秒")
                if (seconds == RECORD_DURATION) {
                    runOnUiThread {
                        btn_record.performClick()
                    }
                    return
                }
                runOnUiThread {
                    btn_record.text = "正在录制: $seconds 秒"
                }
            }

        }
    }
}
