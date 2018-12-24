package com.hikobe8.androidmediaproject

import android.media.*
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Author : hikobe8@github.com
 * Time : 2018/12/21 11:31 AM
 * Description : aac 音频编码器
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AudioEncoder {

    companion object {
        const val TAG = "AudioEncoder"
        const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        const val KEY_CHANNEL_COUNT = 2
        const val KEY_SAMPLE_RATE = 44100
        const val KEY_BIT_RATE = 64000
        const val KEY_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    }

    /**
     * 音频解码器
     */
    private var mEncoder: MediaCodec? = null

    private val mBufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    var mFile:File? = null

    private var mFileOS:FileOutputStream?=null

    /**
     * 创建编码器
     */
    fun createAndStartEncoder(){

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
            val mediaFormat = MediaFormat.createAudioFormat(
                MIME_TYPE,
                KEY_SAMPLE_RATE,
                KEY_CHANNEL_COUNT
            )
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,
                KEY_BIT_RATE
            )
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                KEY_AAC_PROFILE
            )
            mEncoder?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mEncoder?.start()
            mFile = File(FileUtils.getAudioRecordDir(), "${System.currentTimeMillis()}.aac")
            mFileOS = FileOutputStream(mFile)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "创建音频解码器失败 : ${e.message}")
        }

    }

    fun stopAndReleaseEncoder(){
        mEncoder?.stop()
        mEncoder?.release()
        mEncoder = null
        mFileOS?.close()
    }

    /**
     * 编码数据
     */
    fun encode(dataSrc: ByteArray) {
        val inputBufferIndex:Int = mEncoder?.dequeueInputBuffer(-1)!!
        if (inputBufferIndex >= 0) {
            val inputBuffer = mEncoder?.getInputBuffer(inputBufferIndex)!!
            inputBuffer.clear()
            inputBuffer.put(dataSrc)
            mEncoder?.queueInputBuffer(inputBufferIndex, 0, dataSrc.size, System.nanoTime(), 0)
        }

        var outputBufferIndex = mEncoder?.dequeueOutputBuffer(mBufferInfo, 0)!!
        //adts头数据的前7个字节
        while (outputBufferIndex >= 0) {
            var frameBuffer = ByteArray(mBufferInfo.size + 7)
            val outputBuffer = mEncoder?.getOutputBuffer(outputBufferIndex)!!
            addADTStoPacket(frameBuffer, frameBuffer.size)
            outputBuffer.get(frameBuffer, 7, mBufferInfo.size)
            //写入文件
            mFileOS?.write(frameBuffer)
            mEncoder?.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mEncoder?.dequeueOutputBuffer(mBufferInfo, 0)!!
        }
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     * @param packet 要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
        val profile = 2  //AAC LC
        val freqIdx = 4  //44.1KHz
        val chanCfg = 2   //CPE
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = (((profile - 1) shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = (((chanCfg and 3) shl 6) + (packetLen shr 11)).toByte()
        packet[4] = ((packetLen and 0x7FF) shr 3).toByte()
        packet[5] = (((packetLen and 7) shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

}