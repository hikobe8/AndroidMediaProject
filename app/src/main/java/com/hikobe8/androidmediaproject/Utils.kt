package com.hikobe8.androidmediaproject

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresApi
import java.io.File
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.view.Surface
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
import android.view.Surface.ROTATION_0



/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 11:08
 *  description : 工具类集合
 */

class PermissionUtils {

    companion object {

        @RequiresApi(Build.VERSION_CODES.M)
        fun checkPermissionGranted(context: Context, permission:String): Boolean {
                val checkSelfPermission = context.checkSelfPermission(permission)
                return checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun requestPermission(activity:Activity, permissions:Array<String>, reqCode:Int) {
            activity.requestPermissions(permissions, reqCode)
        }
    }

}

class FileUtils {
    companion object {
        fun getStorageDir(): String {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath, BuildConfig.APPLICATION_ID)
            if (!dir.exists())
                dir.mkdirs()
            return dir.absolutePath
        }

        fun getAudioRecordDir():String = getStorageDir().let {
            val dir = File(it, "record")
            if (!dir.exists())
                dir.mkdir()
            dir.absolutePath
        }

        fun getCameraDir():String = getStorageDir().let {
            val dir = File(it, "camera")
            if (!dir.exists())
                dir.mkdir()
            dir.absolutePath
        }

    }
}

object PcmToWavUtil {

    /**
     * @param pcmData pcm原始数据
     * @param numChannels 声道设置, mono = 1, stereo = 2
     * @param sampleRate 采样频率
     * @param bitPerSample 单次数据长度, 例如8bits
     * @return wav数据
     */
    fun pcmToWav(pcmData: ByteArray, numChannels: Int, sampleRate: Int, bitPerSample: Int): ByteArray {
        val wavData = ByteArray(pcmData.size + 44)
        val header = createWavHeader(pcmData.size, numChannels, sampleRate, bitPerSample)
        System.arraycopy(header, 0, wavData, 0, header.size)
        System.arraycopy(pcmData, 0, wavData, header.size, pcmData.size)
        return wavData
    }

    /**
     * @param pcmLen pcm数据长度
     * @param numChannels 声道设置, mono = 1, stereo = 2
     * @param sampleRate 采样频率
     * @param bitPerSample 单次数据长度, 例如8bits
     * @return wav头部信息
     */
    private fun createWavHeader(pcmLen: Int, numChannels: Int, sampleRate: Int, bitPerSample: Int): ByteArray {
        val header = ByteArray(44)
        // ChunkID, RIFF, 占4bytes
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        // ChunkSize, pcmLen + 36, 占4bytes
        val chunkSize = (pcmLen + 36).toLong()
        header[4] = (chunkSize and 0xff).toByte()
        header[5] = (chunkSize shr 8 and 0xff).toByte()
        header[6] = (chunkSize shr 16 and 0xff).toByte()
        header[7] = (chunkSize shr 24 and 0xff).toByte()
        // Format, WAVE, 占4bytes
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        // Subchunk1ID, 'fmt ', 占4bytes
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        // Subchunk1Size, 16, 占4bytes
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat, pcm = 1, 占2bytes
        header[20] = 1
        header[21] = 0
        // NumChannels, mono = 1, stereo = 2, 占2bytes
        header[22] = numChannels.toByte()
        header[23] = 0
        // SampleRate, 占4bytes
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        // ByteRate = SampleRate * NumChannels * BitsPerSample / 8, 占4bytes
        val byteRate = (sampleRate * numChannels * bitPerSample / 8).toLong()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // BlockAlign = NumChannels * BitsPerSample / 8, 占2bytes
        header[32] = (numChannels * bitPerSample / 8).toByte()
        header[33] = 0
        // BitsPerSample, 占2bytes
        header[34] = bitPerSample.toByte()
        header[35] = 0
        // Subhunk2ID, data, 占4bytes
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        // Subchunk2Size, 占4bytes
        header[40] = (pcmLen and 0xff).toByte()
        header[41] = (pcmLen shr 8 and 0xff).toByte()
        header[42] = (pcmLen shr 16 and 0xff).toByte()
        header[43] = (pcmLen shr 24 and 0xff).toByte()

        return header
    }
}

class CameraUtils {

    companion object {

        /** Check if this device has a camera */
        fun checkCameraHardware(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }

        /** A safe way to get an instance of the Camera object. */
        fun getCameraInstance(): Camera? {
            return try {
                Camera.open(1) // attempt to get a Camera instance
            } catch (e: Exception) {
                // Camera is not available (in use or does not exist)
                null // returns null if camera is unavailable
            }
        }


        fun setCameraDisplayOrientation(
            activity: Activity,
            cameraId: Int, camera: android.hardware.Camera
        ) : Int {
            val info = android.hardware.Camera.CameraInfo()
            android.hardware.Camera.getCameraInfo(cameraId, info)
            val rotation = activity.windowManager.defaultDisplay
                .rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }

            var result: Int
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360
            }
            camera.setDisplayOrientation(result)
            return result
        }
    }

}
