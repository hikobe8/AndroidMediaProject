package com.hikobe8.androidmediaproject.mediacodec

import android.content.Context
import android.content.Intent
import android.media.MediaFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.egl.RayEGLFactory
import com.hikobe8.androidmediaproject.opengl.record.RayBaseMediaEncoder
import com.hikobe8.androidmediaproject.opengl.record.RayMediaEncodec
import com.hikobe8.androidmediaproject.opengl.texture.FboRenderer
import com.hikobe8.androidmediaproject.show
import com.hikobe8.audio_extractor.AudioDecoder
import kotlinx.android.synthetic.main.activity_encode_images2_video.*
import java.io.File
import javax.microedition.khronos.egl.EGLContext
import kotlin.concurrent.thread

/***
 *  Author : hikobe8@github.com
 *  Create at 19-6-13 下午6:37
 *  description :
 */
class EncodeImages2VideoActivity : AppCompatActivity() {

    companion object {
        const val RECORD_DURATION = 10
        fun launch(context: Context) {
            context.startActivity(Intent(context, EncodeImages2VideoActivity::class.java))
        }
    }

    private var mVideoEncodec: RayMediaEncodec? = null
    private var mSampleRate = -1
    private var mChannelCount = -1
    private var mTextureId = -1
    private var mSurfaceWidth = -1
    private var mSurfaceHeight = -1
    private var mEGLContext: EGLContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encode_images2_video)
        val rayEGLFactory = RayEGLFactory(mEGLContext)
        gl_content.setEGLContextFactory(rayEGLFactory)
        gl_content.setEGLContextClientVersion(2)
        val renderer = FboRenderer(this).apply {
            onTextureCreateListener = object : FboRenderer.OnTextureCreateListener {
                override fun onTextureCreate(textureId: Int, width: Int, height: Int) {
                    mTextureId = textureId
                    mSurfaceWidth = width
                    mSurfaceHeight = height
                    mEGLContext = rayEGLFactory.getEGLContext()
                    if (mSampleRate > 0 && mChannelCount > 0 && mVideoEncodec == null && mEGLContext != null) {
                        initMediaCodec()
                    }
                }
            }
        }
        gl_content.setRenderer(renderer)
        gl_content.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        thread {
            for (i in 0..700) {
                val id = resources.getIdentifier("slice_${i % 7}", "drawable", packageName)
                renderer.setImageResId(id)
                gl_content.requestRender()
                Thread.sleep(60)
            }

        }
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
                        if (mSurfaceWidth > 0 && mSurfaceHeight > 0 && mTextureId > 0 && mVideoEncodec == null && mEGLContext != null) {
                            initMediaCodec()
                        }
                        mVideoEncodec?.startRecord()
                        runOnUiThread {
                            "开始录制".show(context = this@EncodeImages2VideoActivity)
                        }
                    }

                    override fun onComplete() {

                    }

                    override fun onGetPCMChunk(pcmBuffer: ByteArray) {
                        mVideoEncodec?.putPCMData(pcmBuffer, pcmBuffer.size)
                    }

                })
            }.prepare()
    }

    private fun initMediaCodec() {
        mVideoEncodec = RayMediaEncodec(this@EncodeImages2VideoActivity, mTextureId)
        mVideoEncodec!!.initEncodec(
            mEGLContext!!,
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4",
            MediaFormat.MIMETYPE_VIDEO_AVC, mSurfaceWidth, mSurfaceHeight, mSampleRate, mChannelCount
        )
        mVideoEncodec!!.onProgressChangeListener = object : RayBaseMediaEncoder.ProgressChangeListener {
            override fun onProgressChange(seconds: Int) {
                Log.d("录制中", "$seconds 秒")
                if (seconds == RECORD_DURATION) {
                    mVideoEncodec!!.stopRecord()
                    mVideoEncodec = null
                    runOnUiThread {
                        "${Environment.getExternalStorageDirectory().absolutePath + File.separator + "test_record.mp4"} 录制完成".show(
                            this@EncodeImages2VideoActivity
                        )
                    }
                    return
                }
            }

        }
    }

}
