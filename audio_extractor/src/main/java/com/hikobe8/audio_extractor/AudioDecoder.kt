package com.hikobe8.audio_extractor

import android.annotation.SuppressLint
import android.media.*
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-06-03 14:59
 *  description : 音频解码
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class AudioDecoder private constructor() {

    companion object {

        fun newInstance() = AudioDecoder()

        const val DEFAULT_SAMPLE_SIZE = 44100
        const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val DEFAULT_CHANNEL_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val TAG = "AudioDecoder"
        //        const val URL = "http://mpge.5nd.com/2015/2015-11-26/69708/1.mp3"
        const val URL = "http://mr1.doubanio.com/d0c4f39e600c9832aaba4962e3eb1a31/0/fm/song/p2286961_128k.mp3"
    }

    private var mMediaExtractor: MediaExtractor? = null
    private var mAudioCodec: MediaCodec? = null
    private var mAudioTrack: AudioTrack? = null
    private var mFinished = true
    private var mPlayThread: Thread? = null
    private var mAudioCallback: AudioInfoCallback? = null
    private var mPCMCallback: AudioPCMInfoCallback? = null
    private var mPreparedListener: DecorderPreparedListener? = null
    private var mClock = 0f
    private var mDuration = 0L
    private var mNeedPlay = true
    private var mStartPosition = 0
    private var mDecodeMaxPosition = -1  //解码最大位置(单位秒)，-1表示解码所有数据
    private var mState = 0 // 0 空闲，解码完成状态 1 解码中状态

    /**
     * 音频信息回调，包含时长，和当前播放长度
     */
    interface AudioInfoCallback {
        fun onGetAudioDuration(duration: Long)
        fun onGetPlayProgress(progress: Long)
    }

    interface AudioPCMInfoCallback {
        fun onGetPCMInfo(sampleRate: Int, bitRate: Int, channelCount: Int)
        fun onGetPCMChunk(pcmBuffer: ByteArray)
        fun onComplete()
    }

    interface DecorderPreparedListener {
        fun onPrepared()
    }

    fun setAudioCallback(audioCallback: AudioInfoCallback) {
        mAudioCallback = audioCallback
    }

    fun setAudioPCMCallback(pcmCallback: AudioPCMInfoCallback) {
        mPCMCallback = pcmCallback
    }

    fun setPreparedListener(preparedListener: DecorderPreparedListener) {
        mPreparedListener = preparedListener
    }

    /**
     * 是否播放
     */
    fun setNeedPlay(needPlay: Boolean) {
        mNeedPlay = needPlay
    }

    /**
     * 设置播放起始时间
     * @param startTimeSecond 起始时间，单位秒
     * @param duration 播放时长，单位秒
     */
    fun setStartPositionAndDuration(startTimeSecond: Int, duration: Int) {
        mStartPosition = startTimeSecond
        mDecodeMaxPosition = mStartPosition + duration
    }

    //初始化解码器
    private fun initDecoder() {
        mMediaExtractor = MediaExtractor()
        mMediaExtractor!!.setDataSource(URL)
        val trackCount = mMediaExtractor!!.trackCount
        Logger.i("trackCount = $trackCount")
        try {
            for (i in 0 until trackCount) {
                val mediaFormat = mMediaExtractor!!.getTrackFormat(i)
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    mDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
                    mAudioCallback?.onGetAudioDuration(mDuration)
                    mMediaExtractor!!.selectTrack(i)
//                    mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm")
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
                    mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
                    mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
                    mAudioCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME))
                    mAudioCodec!!.configure(mediaFormat, null, null, 0)
                    mPCMCallback?.onGetPCMInfo(44100, 96000, 2)
                    break
                }
                Logger.e("no audio track found!")
            }
        } catch (e: Exception) {
            mMediaExtractor?.release()
            mMediaExtractor = null
            mAudioCodec = null
        }
        mAudioCodec?.start()
        val bufferSize =
            AudioRecord.getMinBufferSize(
                DEFAULT_SAMPLE_SIZE,
                DEFAULT_CHANNEL_CONFIG,
                DEFAULT_CHANNEL_FORMAT
            )
        if (mNeedPlay) {
            mAudioTrack =
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    DEFAULT_SAMPLE_SIZE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            mAudioTrack!!.play()
        }
    }

    fun prepare() {
        if (mState == 1) {
            return
        }
        thread {
                initDecoder()
                mPreparedListener?.onPrepared()
        }
    }

    fun start() {
        if (mState == 1) {
            return
        }
        mClock = 0f
        mFinished = false
        if (mPlayThread == null) {
            mPlayThread = thread {
                decode()
            }
        }
        mState = 1
    }

    fun release() {
        if (mState == 1) {
            return
        }
        mClock = 0f
        mFinished = true
        mPlayThread = null
        mState = 0
    }

    private fun decode() {
        val bufferInfo = MediaCodec.BufferInfo()
        var lastTime: Float = mClock
        while (!mFinished && if (mDecodeMaxPosition < 0) true else mClock < mDecodeMaxPosition) {
            val inputIndex = mAudioCodec!!.dequeueInputBuffer(0)
            if (inputIndex >= 0) {
                val inputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioCodec!!.getInputBuffer(inputIndex)
                } else {
                    mAudioCodec!!.inputBuffers[inputIndex]
                }
                inputBuffer!!.clear()
                val sampleSize = mMediaExtractor!!.readSampleData(inputBuffer, 0)
                if (sampleSize > 0) {
                    mAudioCodec!!.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0)
                    mMediaExtractor!!.advance()
                } else {
                    release()
                }
            }
            var outputIndex = mAudioCodec!!.dequeueOutputBuffer(bufferInfo, 0)
            var outputBuffer: ByteBuffer?
            var chunkPCM: ByteArray?
            while (outputIndex >= 0 && !mFinished && if (mDecodeMaxPosition < 0) true else mClock < mDecodeMaxPosition) {
                outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioCodec!!.getOutputBuffer(outputIndex)
                } else {
                    mAudioCodec!!.outputBuffers[outputIndex]
                }
                chunkPCM = ByteArray(bufferInfo.size)
                outputBuffer.get(chunkPCM)
                outputBuffer.clear()
                Logger.w("pcm data = ${chunkPCM.size}")
                //176400 = 44100 * 2 * 2 双声道 16bit 采样
                mClock += chunkPCM.size / 176400f
                if (mClock - lastTime > 0.3f && mClock * 1000000 < mDuration) {
                    mAudioCallback?.onGetPlayProgress(mClock.toLong())
                    lastTime = mClock
                }
                if (chunkPCM.isNotEmpty())
                    mPCMCallback?.onGetPCMChunk(chunkPCM)
                mAudioTrack?.write(chunkPCM, 0, bufferInfo.size)
                mAudioCodec!!.releaseOutputBuffer(outputIndex, false)
                outputIndex = mAudioCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            }
        }
        mMediaExtractor?.release()
        mMediaExtractor = null
        if (mAudioTrack?.playState != AudioTrack.PLAYSTATE_STOPPED)
            mAudioTrack?.stop()
        mAudioTrack?.release()
        mAudioTrack = null
        mAudioCodec?.stop()
        mAudioCodec?.release()
        mAudioCodec = null
        mPCMCallback?.onComplete()
        Logger.w("decoder released!")
    }

    @SuppressLint("WrongConstant")
    fun seek(seconds: Int) {
        mMediaExtractor?.seekTo((seconds * 1000000).toLong(), 2)
        mClock = seconds.toFloat()
        mAudioCallback?.onGetPlayProgress(mClock.toLong())
    }

    class Logger {
        companion object {

            val LOG_ENABLE = BuildConfig.DEBUG

            fun i(msg: String) {
                if (LOG_ENABLE)
                    Log.i(TAG, msg)
            }

            fun w(msg: String) {
                if (LOG_ENABLE)
                    Log.w(TAG, msg)
            }

            fun e(msg: String) {
                if (LOG_ENABLE)
                    Log.e(TAG, msg)
            }
        }
    }

}