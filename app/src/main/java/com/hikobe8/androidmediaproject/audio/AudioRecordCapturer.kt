package com.hikobe8.androidmediaproject.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.hikobe8.androidmediaproject.FileUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018/12/9 23:43
 *  description : 使用AudioRerecord实现的简单录音机
 */
class AudioRecordCapturer {

    companion object {
        const val TAG = "AudioRecordCapturer"
        const val DEFAULT_SOURCE = MediaRecorder.AudioSource.MIC
        const val DEFAULT_SAMPLE_SIZE = 44100
        const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val DEFAULT_CHANNEL_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioRecorder: AudioRecord? = null
    private var mMinBufferSize = 0
    private var mLoopStarted = false
    private var mRecordStarted = false
    private var mCaptureThread: Thread? = null
    var mFileName: String? = null
    var mOnRecordCompleteListener: OnRecordCompleteListener? = null

    interface OnRecordCompleteListener {
        fun onRecordCompleted(audioRecordBean: AudioRecordBean?)
    }

    fun startCapture() {
        if (mRecordStarted) {
            Log.e(TAG, "Capture already started!")
            return
        }
        if (TextUtils.isEmpty(mFileName)) {
            mFileName = System.currentTimeMillis().toString() + ".pcm"
        }
        mMinBufferSize =
                AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_SIZE, DEFAULT_CHANNEL_CONFIG, DEFAULT_CHANNEL_FORMAT)
        if (mMinBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "getMinBufferSize error!")
            return
        }
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
        mRecordStarted = true
        mCaptureThread = Thread(AudioCaptureRunnable())
        mCaptureThread?.start()
        mLoopStarted = true
        Log.d(TAG, "capture started")
    }

    fun stopCapture() {
        if (!mRecordStarted) {
            return
        }
        mLoopStarted = false
        try {
            mCaptureThread?.interrupt()
            mCaptureThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (mAudioRecorder?.state == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecorder?.stop()
        }
        mAudioRecorder?.release()
        mRecordStarted = false
        Log.d(TAG, "capture stopped")
    }

    inner class AudioCaptureRunnable : Runnable {
        override fun run() {
            var fos: BufferedOutputStream? = null
            try {
                val file = File(FileUtils.getAudioRecordDir(), mFileName)
                if (!file.exists()) {
                    file.createNewFile()
                }
                fos = BufferedOutputStream(FileOutputStream(file))
                while (mLoopStarted) {
                    val buffer = ByteArray(mMinBufferSize)
                    val ret = mAudioRecorder?.read(buffer, 0, mMinBufferSize)
                    when (ret) {
                        AudioRecord.ERROR_INVALID_OPERATION -> Log.e(TAG, "ERROR_INVALID_OPERATION")
                        AudioRecord.ERROR_BAD_VALUE -> Log.e(TAG, "ERROR_BAD_VALUE")
                        else -> {
                            // write to file
                            fos.write(buffer, 0, ret!!)
                        }
                    }
                    SystemClock.sleep(10)
                }
                fos.flush()
                mOnRecordCompleteListener?.onRecordCompleted(
                    mFileName?.let { AudioRecordBean(it, file.absolutePath, file.length() / (DEFAULT_SAMPLE_SIZE * 2 * 2)) }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }

        }
    }
}