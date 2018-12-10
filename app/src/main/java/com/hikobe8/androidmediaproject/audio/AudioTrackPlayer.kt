package com.hikobe8.androidmediaproject.audio

import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 19:03
 *  description : 使用AudioTrack实现的简易PCM音频播放器
 */

class AudioTrackPlayer {

    fun play(audioPath:String) {
        Observable.create(ObservableOnSubscribe<ByteArray> {
            //write audio data in a byte array
            val file = File(audioPath)
            if (file.exists() && file.length() > 0) {
                val bis:BufferedInputStream = BufferedInputStream(FileInputStream(file))
                //
            }
            it.onComplete()
        }).subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ByteArray>{
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: ByteArray) {
                }

                override fun onError(e: Throwable) {
                }
            })
    }

}