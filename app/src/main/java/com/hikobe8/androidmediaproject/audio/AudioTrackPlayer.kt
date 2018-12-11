package com.hikobe8.androidmediaproject.audio

import android.media.*
import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.lang.Exception
import java.nio.ByteBuffer

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 19:03
 *  description : 使用AudioTrack实现的简易PCM音频播放器
 */

class AudioTrackPlayer {

    companion object {
        const val TAG = "AudioTrackPlayer"
        const val DEFAULT_SAMPLE_SIZE = 44100
        const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val DEFAULT_CHANNEL_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var mAudioTrack: AudioTrack? = null
    private var mDisposable: Disposable? = null
    var mOnPlayCompletelyListener: OnPlayCompletelyListener? = null

    interface OnPlayCompletelyListener {
        fun onPlayCompletely()
    }

    fun play(audioPath: String) {
        if (mAudioTrack != null) {
            stop()
        }
        Observable.create(ObservableOnSubscribe<AudioTrack> {
            //write audio data in a byte array
            val file = File(audioPath)
            if (file.exists() && file.length() > 0) {
                var bis: BufferedInputStream? = null
                try {
                    bis = BufferedInputStream(FileInputStream(file))
                    val bufferSize =
                            AudioRecord.getMinBufferSize(
                                AudioRecordCapturer.DEFAULT_SAMPLE_SIZE,
                                AudioRecordCapturer.DEFAULT_CHANNEL_CONFIG,
                                AudioRecordCapturer.DEFAULT_CHANNEL_FORMAT
                            )
                    mAudioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        DEFAULT_SAMPLE_SIZE,
                        DEFAULT_CHANNEL_CONFIG,
                        DEFAULT_CHANNEL_FORMAT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                    val buffer = ByteArray(bufferSize)
                    var touchEnd = false
                    mAudioTrack?.play()
                    while (!touchEnd) {
                        if (it.isDisposed) {
                            touchEnd = true
                            continue
                        }
                        val len = bis.read(buffer)
                        if (len == -1) {
                            touchEnd = true
                            continue
                        }
                        mAudioTrack?.write(buffer, 0, len)
                    }
                    mAudioTrack?.stop()
                    //mOnPlayCompletelyListener?.onPlayCompletely() todo: correct OnPlayCompletelyListener invocation
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    bis?.close()
                }
                if (mAudioTrack != null)
                    it.onNext(mAudioTrack!!)
            }
            it.onComplete()
        }).subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<AudioTrack> {
                override fun onComplete() {
                    mDisposable?.dispose()
                }

                override fun onSubscribe(d: Disposable) {
                    mDisposable = d
                }

                override fun onNext(audioTrack: AudioTrack) {

                }

                override fun onError(e: Throwable) {
                    stop()
                }
            })
    }

    fun playStatic(audioPath: String) {
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
                        if (it.isDisposed) {
                            isEnd = true
                            continue
                        }
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
                val pcmBytes = byteArrayOutputStream?.toByteArray()
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    DEFAULT_SAMPLE_SIZE,
                    DEFAULT_CHANNEL_CONFIG,
                    DEFAULT_CHANNEL_FORMAT,
                    pcmBytes!!.size,
                    AudioTrack.MODE_STATIC
                )
                mAudioTrack?.write(pcmBytes, 0, pcmBytes.size)
                mAudioTrack?.notificationMarkerPosition = ((file.length() / 2).toInt())
                it.onNext(mAudioTrack!!)
//                val wavBytes = PcmToWavUtil.pcmToWav(pcmBytes, 2, DEFAULT_SAMPLE_SIZE, 16)
//                val wavFile = File(FileUtils.getStorageDir(), "Pcm2Wav.wav")
//                if (!wavFile.exists())
//                    wavFile.createNewFile()
//                val bos = FileOutputStream(wavFile)
//                bos.write(wavBytes)
//                bos.flush()
//                bos.close()
            }
            it.onComplete()
        }).subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<AudioTrack> {
                override fun onComplete() {
                    mDisposable?.dispose()
                }

                override fun onSubscribe(d: Disposable) {
                    mDisposable = d
                }

                override fun onNext(audioTrack: AudioTrack) {
                    audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onMarkerReached(track: AudioTrack?) {
                            Log.e(TAG, "playbackHeadPosition changed : " + audioTrack.playbackHeadPosition.toString())
                        }

                        override fun onPeriodicNotification(track: AudioTrack?) {
                            Log.e(TAG, "audio plays completely")
                            mOnPlayCompletelyListener?.onPlayCompletely()
                        }

                    })
                    audioTrack.play()
                }

                override fun onError(e: Throwable) {
                    stop()
                }
            })
    }

    fun stop() {
        if (mDisposable!= null && !mDisposable?.isDisposed!!) {
            mDisposable?.dispose()
            mDisposable = null
        }
        mAudioTrack?.stop()
        mAudioTrack?.release()
        mAudioTrack = null
    }

}