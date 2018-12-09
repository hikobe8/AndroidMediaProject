package com.hikobe8.androidmediaproject.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018/12/9 23:43
 *  description : 使用AudioRerecord实现的简单录音机
 */
class AudioRecordCapturer {

    companion object {
        val TAG = "AudioRecordCapturer"
        const val DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC
        const val DEFAULT_SAMPLE_SIZE = 44100
        const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val DEFAULT_CHANNEL_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioRecorder: AudioRecord? = null
    private var mMinBufferSize = 0
   private  var mLoopStarted = false

    fun startCapture() {
        mMinBufferSize =
                AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_SIZE, DEFAULT_CHANNEL_CONFIG, DEFAULT_CHANNEL_FORMAT)
        Log.d(TAG, "getMinBufferSize = $mMinBufferSize")
        mAudioRecorder = AudioRecord(
            DEFAULT_SOURCE,
            DEFAULT_SAMPLE_SIZE,
            DEFAULT_CHANNEL_CONFIG,
            DEFAULT_CHANNEL_FORMAT,
            mMinBufferSize
        )
        if (mAudioRecorder?.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            return
        }
        mAudioRecorder?.startRecording()
        mLoopStarted = true
        Thread(AudioCaptureRunnable()).start()
        Log.d(TAG, "capture started")
    }

    fun stopCapture() {
        mLoopStarted = false
        if (mAudioRecorder?.state == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecorder?.stop()
        }
        mAudioRecorder?.release()
        Log.d(TAG, "capture stopped")
    }

    inner class AudioCaptureRunnable : Runnable {
        override fun run() {
            while (mLoopStarted) {
                val buffer:ByteArray = ByteArray(mMinBufferSize)
                val ret = mAudioRecorder?.read(buffer, 0, mMinBufferSize)
                Log.d(TAG, "get "+ret+"bytes pcm BufferSize")
                SystemClock.sleep(10)
            }
        }
    }

}