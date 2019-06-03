package com.hikobe8.audio_extractor

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.lang.Exception
import java.nio.ByteBuffer

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-06-03 14:59
 *  description : 音频解码
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class AudioDecoder {

    companion object {
        const val TAG = "AudioDecoder"
    }

    private var mMediaExtractor: MediaExtractor? = null
    private var mAudioDecoder: MediaCodec? = null

    //初始化解码器
    fun initDecoder() {
        mMediaExtractor = MediaExtractor()
        mMediaExtractor!!.setDataSource("http://mr1.doubanio.com/d0c4f39e600c9832aaba4962e3eb1a31/0/fm/song/p2286961_128k.mp3")
        val trackCount = mMediaExtractor!!.trackCount
        Logger.i("trackCount = $trackCount")
        try {
            for (i in 0 until trackCount) {
                val mediaFormat = mMediaExtractor!!.getTrackFormat(i)
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio")) {
                    mMediaExtractor!!.selectTrack(i)
                    mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm")
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
                    mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
                    mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
                    mAudioDecoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME))
                    mAudioDecoder!!.configure(mediaFormat, null, null, 0)
                    break
                }
                Logger.e("no audio track found!")
            }
        } catch (e: Exception) {
            mMediaExtractor?.release()
            mMediaExtractor = null
            mAudioDecoder = null
        }
        mAudioDecoder?.start()
        decode()
    }

    private fun decode() {
        var finished = false
        val bufferInfo = MediaCodec.BufferInfo()
        while (!finished) {
            val inputIndex = mAudioDecoder!!.dequeueInputBuffer(0)
            if (inputIndex < 0) {
                finished = true
            }
            val inputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mAudioDecoder!!.getOutputBuffer(inputIndex)
            } else {
                mAudioDecoder!!.inputBuffers[inputIndex]
            }
            inputBuffer!!.clear()
            val sampleSize = mMediaExtractor!!.readSampleData(inputBuffer, 0)
            if (sampleSize > 0) {
                mAudioDecoder!!.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0)
                mMediaExtractor!!.advance()
            } else {
                finished = true
            }
            var outputIndex = mAudioDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            var outputBuffer: ByteBuffer? = null
            var chunkPCM: ByteArray? = null
            while (outputIndex >= 0) {
                outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioDecoder!!.getOutputBuffer(outputIndex)
                } else {
                    mAudioDecoder!!.outputBuffers[outputIndex]
                }
                chunkPCM = ByteArray(bufferInfo.size)
                outputBuffer.get(chunkPCM)
                outputBuffer.clear()
                Logger.w("pcm data = ${chunkPCM.size}")
                outputIndex = mAudioDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
    }

    private class Logger {
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