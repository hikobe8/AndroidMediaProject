package com.hikobe8.androidmediaproject.mediacodec

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.show
import com.hikobe8.audio_extractor.AudioDecoder
import kotlinx.android.synthetic.main.activity_media_codec_decode.*

class MediaCodecDecodeActivity : AppCompatActivity() {

    private var mAudioDecoder: AudioDecoder? = null

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, MediaCodecDecodeActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec_decode)
        mAudioDecoder = AudioDecoder.newInstance()
        mAudioDecoder!!.setAudioCallback(object : AudioDecoder.AudioInfoCallback {
            override fun onGetPlayProgress(progress: Long) {
                runOnUiThread {
                    tv_progress.text = formatSeconds(progress)
                    seek.progress = progress.toInt()
                }
            }

            override fun onGetAudioDuration(duration: Long) {
                runOnUiThread {
                    tv_duration.text = formatMicroSeconds(duration)
                    seek.max = (duration / 1000000).toInt()
                }
            }
        })
        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mAudioDecoder?.seek(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    fun play(v: View) {
        seek.max = 0
        seek.progress = 0
        mAudioDecoder?.setPreparedListener(object: AudioDecoder.DecoderPreparedListener {
            override fun onPrepared() {
                mAudioDecoder?.start()
            }
        })
        mAudioDecoder?.prepare()
        "开始播放".show(this)
    }

    fun stop(v: View?) {
        mAudioDecoder?.release()
        "结束播放".show(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        stop(null)
    }

    /**
     * 格式化秒 为 hh:mm:ss
     */
    private fun formatSeconds(seconds: Long): String {
        val hh: Int
        val mm: Int
        var ss: Int
        return when {
            seconds > 3599 -> {
                hh = (seconds / 3600).toInt()
                mm = (seconds / 60 % 60).toInt()
                ss = (seconds % 60).toInt()
                "${fillTime(hh)}:${fillTime(mm)}:${fillTime(ss)}"
            }
            seconds > 59 -> {
                mm = (seconds / 60).toInt()
                ss = (seconds % 60).toInt()
                "00:${fillTime(mm)}:${fillTime(ss)}"
            }
            else -> "00:00:${fillTime(seconds.toInt())}"
        }
    }

    /**
     * 格式化微秒 为 hh:mm:ss
     */
    private fun formatMicroSeconds(timeUs: Long): String {
        val seconds = timeUs / 1000000
        return formatSeconds(seconds)
    }

    private fun fillTime(time: Int): String {
        return if (time > 9) "$time" else "0$time"
    }

}
