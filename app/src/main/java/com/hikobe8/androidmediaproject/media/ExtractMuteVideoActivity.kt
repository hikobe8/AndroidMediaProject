package com.hikobe8.androidmediaproject.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.support.annotation.RequiresApi
import com.hikobe8.androidmediaproject.BaseActivity
import com.hikobe8.androidmediaproject.PermissionUtils
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_extract_mute_video.*
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ExtractMuteVideoActivity : BaseActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, ExtractMuteVideoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.checkPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PermissionUtils.requestPermission(mActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                initViews()
            }

        } else {
            initViews()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtils.isAllPermissionsGranted(requestCode, grantResults)) {
            initViews()
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_extract_mute_video)
        setSupportActionBar(toolbar)
        setHomeAsUpEnabled()
        button.setOnClickListener {
            Thread(Runnable {
                try {
                    startExtraction(getFilePath("test.mp4"), getFilePath("test_mute${System.currentTimeMillis()}.mp4"))
                } catch (e:Exception) {
                    e.printStackTrace()
                    log(e.toString())
                }
            }).start()
        }
    }

    private fun getFilePath(filename:String) :String {
        return Environment.getExternalStorageDirectory().absolutePath +File.separator+filename
    }

    private fun startExtraction(inputPath: String, outputPath: String) {
        log("start extracting")

        var muxer: MediaMuxer? = null
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)
        log("start demuxer : $inputPath")
        var videoTrackIndex = -1
        for (i in 0..extractor.trackCount) {
            try {
                val mediaFormat = extractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (!mime.startsWith("video")) {
                    log("mime not video , continue search")
                    continue
                }
                extractor.selectTrack(i)
                muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                videoTrackIndex = muxer.addTrack(mediaFormat)
                muxer.start()
                log("start muxer : $outputPath")
            } catch (e:IllegalArgumentException) {
                e.printStackTrace()
                log("get mediaFormat exception : ${e.message}")
                continue
            }
        }

        if (muxer == null) {
            log("no video found")
            return
        }

        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.presentationTimeUs = 0L
        val buffer = ByteBuffer.allocate(1024 * 1024 * 2)

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                log("read sample data failed, exit!")
                break
            }
            bufferInfo.let {
                it.offset = 0
                it.size = sampleSize
                it.flags = extractor.sampleFlags
                it.presentationTimeUs = extractor.sampleTime
            }
            val keyFrame: Boolean = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) > 0
            log("write sample $keyFrame $sampleSize ${bufferInfo.presentationTimeUs}")
            muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
        extractor.release()

        muxer.stop()
        muxer.release()
        log("extraction success : $outputPath")
    }

    private fun log(content: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            textView.text = textView.text.let {
                "$it \n $content"
            }
        } else {
            runOnUiThread {
                textView.text = textView.text.let {
                    "$it \n $content"
                }
            }
        }
    }
}
