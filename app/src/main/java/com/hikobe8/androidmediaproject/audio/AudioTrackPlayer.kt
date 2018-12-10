package com.hikobe8.androidmediaproject.audio

import android.media.*
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 19:03
 *  description : 使用AudioTrack实现的简易PCM音频播放器
 */

class AudioTrackPlayer {

    companion object {
        const val DEFAULT_SAMPLE_SIZE = 44100
        const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val DEFAULT_CHANNEL_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioTrack:AudioTrack?= null

    fun play(audioPath: String) {
        if (mAudioTrack != null) {
            stop()
        }
        Observable.create(ObservableOnSubscribe<AudioTrack> {
            //write audio data in a byte array
            val file = File(audioPath)
            if (file.exists() && file.length() > 0) {
                var bis: BufferedInputStream? = null
                var byteArrayOutputStream: ByteArrayOutputStream? = null
                try {
                    bis = BufferedInputStream(FileInputStream(file))
                    byteArrayOutputStream = ByteArrayOutputStream(file.length().toInt())
                    val buffer = ByteArray(1024)
                    var len: Int
                    var isEnd = false
                    while (!isEnd) {
                        len = bis.read(buffer)
                        if (len == -1) {
                            isEnd = true
                        } else {
                            byteArrayOutputStream.write(buffer, 0, len)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    bis?.close()
                    byteArrayOutputStream?.close()
                }
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    DEFAULT_SAMPLE_SIZE,
                    DEFAULT_CHANNEL_CONFIG,
                    DEFAULT_CHANNEL_FORMAT,
                    byteArrayOutputStream?.toByteArray()!!.size,
                    AudioTrack.MODE_STATIC
                )
                mAudioTrack?.write(byteArrayOutputStream.toByteArray()!!, 0, byteArrayOutputStream.toByteArray()!!.size)
                it.onNext(mAudioTrack!!)
            }
            it.onComplete()
        }).subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<AudioTrack> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(audioTrack: AudioTrack) {
                    audioTrack.play()
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    fun stop() {
        mAudioTrack?.stop()
        mAudioTrack?.release()
        mAudioTrack = null
    }

}