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

    private var mVideoEncodec: MediaCodec? = null
    private var mVideoBufferInfo: MediaCodec.BufferInfo? = null
    private var mVideoFormat: MediaFormat? = null
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
    fun initEncodec(eglContext: EGLContext, savePath: String, mimeType: String, width: Int, height: Int) {
        mEglContext = eglContext
        mWidth = width
        mHeight = height
        mMediaMuxer = MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        initVideoEncodec(mimeType, width, height)
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

    fun startRecord(){
        if (mSurface !=null && mEglContext !=null) {
            mGLMediaThread = RayEGLMediaThread(WeakReference(this))
            mVideoEncodecThread = VideoEncodecThread(WeakReference(this))
            mGLMediaThread!!.mCreated = true
            mGLMediaThread!!.mSizeChanged = true
            mGLMediaThread!!.start()
            mVideoEncodecThread!!.start()
        }
    }

    fun stopRecord(){
        mVideoEncodecThread?.exit()
        mGLMediaThread?.onDestroy()
        mVideoEncodecThread = null
        mGLMediaThread = null
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
        private var mVideoTrackIndex = 0
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

                    mMediaMuxer?.stop()
                    mMediaMuxer?.release()
                    mMediaMuxer = null
                    Log.d(TAG, "录制完成")
                    break
                }

                var outputBufferIndex = mVideoEncodec!!.dequeueOutputBuffer(mVideoBufferInfo!!, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //开始muxer
                    mVideoTrackIndex = mMediaMuxer!!.addTrack(mVideoEncodec!!.outputFormat)
                    mMediaMuxer!!.start()
                } else {
                    while (outputBufferIndex >= 0) {

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

        fun exit() {
            mExit = true
        }

    }

    interface ProgressChangeListener {
        fun onProgressChange(seconds: Int)
    }

}