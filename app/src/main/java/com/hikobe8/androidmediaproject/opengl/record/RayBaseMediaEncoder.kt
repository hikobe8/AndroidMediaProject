package com.hikobe8.androidmediaproject.opengl.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import com.hikobe8.androidmediaproject.opengl.egl.EglHelper
import com.hikobe8.androidmediaproject.opengl.egl.RayEGLSurfaceView
import com.hikobe8.androidmediaproject.opengl.egl.RayRenderer
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

/**
 * Author : hikobe8@github.com
 * Time : 2019/4/1 7:17 PM
 * Description : 视频音频编码类
 */
abstract class RayBaseMediaEncoder {

    companion object {
        /**
         * The renderer only renders
         * when the surface is created, or when [.requestRender] is called.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         * @see .requestRender
         */
        const val RENDERMODE_WHEN_DIRTY = 0
        /**
         * The renderer is called
         * continuously to re-render the scene.
         *
         * @see .getRenderMode
         * @see .setRenderMode
         */
        const val RENDERMODE_CONTINUOUSLY = 1

        const val TAG = "RayBaseMediaEncoder"
    }

    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mSurface: Surface? = null
    private var mEglContext: EGLContext? = null
    private var mRenderer: RayRenderer? = null
    private var mRendererMode =
        RayEGLSurfaceView.RENDERMODE_CONTINUOUSLY
    private var mGLMediaThread: RayEGLMediaThread? = null
    private var mVideoEncodecThread: VideoEncodecThread? = null
    private var mAudioEncodecThread: AudioEncodeThread? = null

    private var mVideoEncodec: MediaCodec? = null
    private var mVideoBufferInfo: MediaCodec.BufferInfo? = null
    private var mVideoFormat: MediaFormat? = null

    private var mAudioEncodec: MediaCodec? = null
    private var mAudioBufferInfo: MediaCodec.BufferInfo? = null
    private var mAudioFormat: MediaFormat? = null
    private var mAudioPts: Long = 0
    private var mEncodeStart = false
    private var mAudioExit: Boolean = false
    private var mVideoExit: Boolean = false

    private var mMediaMuxer: MediaMuxer? = null
    var onProgressChangeListener: ProgressChangeListener? = null

    fun setRenderer(rayRenderer: RayRenderer) {
        mRenderer = rayRenderer
    }

    fun setRenderMode(renderMode: Int) {
        if (mRenderer == null) {
            throw RuntimeException("renderer is null")
        }
        mRendererMode = renderMode
    }

    /**
     * 初始化编码器
     */
    fun initEncodec(
        eglContext: EGLContext,
        savePath: String,
        mimeType: String,
        width: Int,
        height: Int,
        sampleRate: Int,
        channelCount: Int
    ) {
        mEglContext = eglContext
        mWidth = width
        mHeight = height
        mMediaMuxer = MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        initVideoEncodec(mimeType, width, height)
        initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
    }

    /**
     * 初始化视频编码器
     */
    private fun initVideoEncodec(mimeType: String, width: Int, height: Int) {
        mVideoBufferInfo = MediaCodec.BufferInfo()
        mVideoFormat = MediaFormat.createVideoFormat(mimeType, width, height)
        mVideoFormat!!.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mVideoFormat!!.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
        mVideoFormat!!.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mVideoFormat!!.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        mVideoEncodec = MediaCodec.createEncoderByType(mimeType)
        mVideoEncodec!!.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mSurface = mVideoEncodec!!.createInputSurface()
    }

    /**
     * 初始化音频编码器
     */
    private fun initAudioEncodec(mimeType: String, sampleRate: Int, channelCount: Int) {
        mAudioSampleRate = sampleRate
        mAudioBufferInfo = MediaCodec.BufferInfo()
        mAudioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount)
        mAudioFormat!!.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
        mAudioFormat!!.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mAudioFormat!!.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)

        mAudioEncodec = MediaCodec.createEncoderByType(mimeType)
        mAudioEncodec!!.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    fun startRecord() {
        if (mSurface != null && mEglContext != null) {
            mGLMediaThread = RayEGLMediaThread(WeakReference(this))
            mVideoEncodecThread = VideoEncodecThread(WeakReference(this))
            mAudioEncodecThread = AudioEncodeThread(WeakReference(this))
            mGLMediaThread!!.mCreated = true
            mGLMediaThread!!.mSizeChanged = true
            mGLMediaThread!!.start()
            mVideoEncodecThread!!.start()
            mAudioEncodecThread?.start()
        }
    }

    fun stopRecord() {
        mAudioEncodecThread?.exit()
        mVideoEncodecThread?.exit()
        mGLMediaThread?.onDestroy()
        mVideoEncodecThread = null
        mGLMediaThread = null
    }

    private var mAudioSampleRate: Int = 0

    fun putPCMData(buffer: ByteArray, size: Int) {
        if (mAudioEncodecThread != null && mAudioEncodecThread?.mExit == false && size > 0 && mEncodeStart) {
            val inputBufferIndex = mAudioEncodec!!.dequeueInputBuffer(0)
            if (inputBufferIndex >= 0) {
                val byteBuffer = mAudioEncodec!!.inputBuffers[inputBufferIndex]
                byteBuffer.clear()
                byteBuffer.put(buffer)
                val pts = getAudioPts(size, mAudioSampleRate)
                mAudioEncodec!!.queueInputBuffer(inputBufferIndex, 0, size, pts, 0)
            }
        }
    }

    private fun getAudioPts(size: Int, sampleRate: Int): Long {
        mAudioPts += (size.toDouble() / (sampleRate * 2 * 2) * 1000000.0).toLong()
        return mAudioPts
    }

    class RayEGLMediaThread(private val mEncoderWeakRef: WeakReference<RayBaseMediaEncoder>) : Thread() {

        private var mExit = false
        var mCreated = false
        var mSizeChanged = false
        private var mEglHelper: EglHelper? = null
        private val mLock = Object()
        private var mIsStart = false

        override fun run() {
            mIsStart = false
            mEglHelper = EglHelper()
            mEncoderWeakRef.get()!!.mSurface?.let {
                Log.d(RayEGLSurfaceView.TAG, "Thread name = $name : EGLHelper created")
                mEglHelper!!.start(mEncoderWeakRef.get()!!.mSurface!!, mEncoderWeakRef.get()!!.mEglContext)
            }
            while (true) {

                if (mExit) {
                    release()
                    return
                }

                if (mIsStart) {
                    when {
                        mEncoderWeakRef.get()!!.mRendererMode == RENDERMODE_WHEN_DIRTY -> synchronized(mLock) {
                            mLock.wait()
                        }
                        mEncoderWeakRef.get()!!.mRendererMode == RENDERMODE_CONTINUOUSLY -> Thread.sleep(1000 / 60)
                        else -> throw RuntimeException("renderMode = ${mEncoderWeakRef.get()!!.mRendererMode} is not supported!")
                    }
                }

                onCreate()
                onSizeChanged(mEncoderWeakRef.get()!!.mWidth, mEncoderWeakRef.get()!!.mHeight)
                onDraw()

                mIsStart = true
            }
        }

        private fun onCreate() {
            if (mCreated && mEncoderWeakRef.get() != null) {
                Log.d(RayEGLSurfaceView.TAG, "onCreate")
                mCreated = false
                mEncoderWeakRef.get()!!.mRenderer?.onSurfaceCreated()
            }
        }

        private fun onSizeChanged(width: Int, height: Int) {
            if (mSizeChanged && mEncoderWeakRef.get() != null) {
                Log.d(RayEGLSurfaceView.TAG, "onSizeChanged width = $width, height = $height")
                mSizeChanged = false
                mEncoderWeakRef.get()!!.mRenderer?.onSurfaceSizeChanged(width, height)
            }
        }

        private fun onDraw() {
            Log.d(RayEGLSurfaceView.TAG, "onDraw")
            mEncoderWeakRef.get()!!.mRenderer?.onDraw()
            if (!mIsStart) {
                mEncoderWeakRef.get()?.mRenderer?.onDraw()
            }
            mEglHelper?.swapBuffers()
        }

        fun onDestroy() {
            mExit = true
            requestRender()
        }

        private fun release() {
            mEglHelper?.destroy()
            mEglHelper = null
            mEncoderWeakRef.clear()
        }

        private fun requestRender() {
            if (mEncoderWeakRef.get()!!.mRendererMode == RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                synchronized(mLock) {
                    mLock.notifyAll()
                }
            }
        }

    }

    class VideoEncodecThread(private val mEncoderWeakRef: WeakReference<RayBaseMediaEncoder>) : Thread() {

        private var mExit = false
        private var mVideoEncodec = mEncoderWeakRef.get()?.mVideoEncodec
        private var mVideoBufferInfo = mEncoderWeakRef.get()?.mVideoBufferInfo
        private var mMediaMuxer = mEncoderWeakRef.get()?.mMediaMuxer
        var mVideoTrackIndex = 0
        private var mPts = 0L

        override fun run() {

            mPts = 0L
            mVideoTrackIndex = -1
            mExit = false
            mVideoEncodec?.start()
            while (true) {

                if (mExit) {
                    mVideoEncodec?.stop()
                    mVideoEncodec?.release()
                    mVideoEncodec = null
                    mEncoderWeakRef.get()?.mVideoExit = true
                    if (mEncoderWeakRef.get()?.mAudioExit == true) {
                        mMediaMuxer?.stop()
                        mMediaMuxer?.release()
                        mMediaMuxer = null
                    }

                    Log.d(TAG, "录制完成")
                    break
                }

                var outputBufferIndex = mVideoEncodec!!.dequeueOutputBuffer(mVideoBufferInfo!!, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //开始muxer
                    mVideoTrackIndex = mMediaMuxer!!.addTrack(mVideoEncodec!!.outputFormat)
                    if (mEncoderWeakRef.get()?.mAudioEncodecThread?.mAudioTrackIndex != -1) {
                        mMediaMuxer?.start()
                        mEncoderWeakRef.get()?.mEncodeStart = true
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (mEncoderWeakRef.get()?.mEncodeStart == true) {
                            val outputBuffer = mVideoEncodec!!.outputBuffers[outputBufferIndex]
                            outputBuffer.position(mVideoBufferInfo!!.offset)
                            outputBuffer.limit(mVideoBufferInfo!!.offset + mVideoBufferInfo!!.size)

                            if (mPts == 0L) {
                                mPts = mVideoBufferInfo!!.presentationTimeUs
                            }

                            mVideoBufferInfo!!.presentationTimeUs = mVideoBufferInfo!!.presentationTimeUs - mPts

                            mMediaMuxer!!.writeSampleData(mVideoTrackIndex, outputBuffer, mVideoBufferInfo!!)

                            mEncoderWeakRef.get()?.let {
                                it.onProgressChangeListener?.onProgressChange(
                                    (mVideoBufferInfo!!.presentationTimeUs / 1000000).toInt()
                                )
                            }

                            mVideoEncodec!!.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mVideoEncodec!!.dequeueOutputBuffer(mVideoBufferInfo!!, 0L)
                        }
                    }
                }

            }

        }

        fun exit() {
            mExit = true
        }

    }

    class AudioEncodeThread(private val mEncoderWeakRef: WeakReference<RayBaseMediaEncoder>) : Thread() {

        var mExit = false
        private var mAudioEncodec = mEncoderWeakRef.get()?.mAudioEncodec
        private var mAudioBufferInfo = mEncoderWeakRef.get()?.mAudioBufferInfo
        private var mMediaMuxer = mEncoderWeakRef.get()?.mMediaMuxer
        var mAudioTrackIndex = -1
        private var mPts = 0L

        fun exit() {
            mExit = true
        }

        override fun run() {
            mPts = 0L
            mAudioTrackIndex = -1
            mExit = false
            mAudioEncodec?.start()
            while (true) {

                if (mExit) {
                    //退出音频编码
                    mAudioEncodec?.stop()
                    mAudioEncodec?.release()
                    mAudioEncodec = null
                    mEncoderWeakRef.get()?.mAudioExit = true
                    if (mEncoderWeakRef.get()?.mVideoExit == true) {
                        mMediaMuxer?.stop()
                        mMediaMuxer?.release()
                        mMediaMuxer = null
                    }

                    break
                }

                var outputBufferIndex = mAudioEncodec!!.dequeueOutputBuffer(mAudioBufferInfo!!, 0)

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mAudioTrackIndex = mMediaMuxer!!.addTrack(mAudioEncodec!!.outputFormat)
                    if (mEncoderWeakRef.get()?.mVideoEncodecThread?.mVideoTrackIndex != -1) {
                        mMediaMuxer?.start()
                        mEncoderWeakRef.get()?.mEncodeStart = true
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (mEncoderWeakRef.get()?.mEncodeStart == true) {
                            val outputBuffer = mAudioEncodec!!.outputBuffers[outputBufferIndex]
                            outputBuffer.position(mAudioBufferInfo!!.offset)
                            outputBuffer.limit(mAudioBufferInfo!!.offset + mAudioBufferInfo!!.size)
                            if (mPts == 0L) {
                                mPts = mAudioBufferInfo!!.presentationTimeUs
                            }

                            mAudioBufferInfo!!.presentationTimeUs = mAudioBufferInfo!!.presentationTimeUs - mPts

                            mMediaMuxer!!.writeSampleData(mAudioTrackIndex, outputBuffer, mAudioBufferInfo!!)
                            mAudioEncodec?.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mAudioEncodec!!.dequeueOutputBuffer(mAudioBufferInfo!!, 0)
                        }
                    }
                }
            }

        }

    }

    interface ProgressChangeListener {
        fun onProgressChange(seconds: Int)
    }

}