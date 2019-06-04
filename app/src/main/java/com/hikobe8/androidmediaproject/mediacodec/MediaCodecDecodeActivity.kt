package com.hikobe8.androidmediaproject.mediacodec

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.show
import com.hikobe8.audio_extractor.AudioDecoder
import kotlin.concurrent.thread

class MediaCodecDecodeActivity : AppCompatActivity() {

    var mAudioDecoder: AudioDecoder? = null

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, MediaCodecDecodeActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec_decode)
        mAudioDecoder = AudioDecoder()
        thread {
            mAudioDecoder?.initDecoder()
        }
        "开始播放".show(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAudioDecoder?.release()
        "结束播放".show(this)
    }

}
