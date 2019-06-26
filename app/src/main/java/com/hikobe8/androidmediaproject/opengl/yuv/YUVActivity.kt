package com.hikobe8.androidmediaproject.opengl.yuv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_yuv.*
import java.io.File
import java.io.FileInputStream

class YUVActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, YUVActivity::class.java))
        }
        val YUV_PATH = Environment.getExternalStorageDirectory().absolutePath + File.separator +"sintel_640_360.yuv"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yuv)
    }

    fun start(view: View) {

        Thread(Runnable {
            try {
                val w = 640
                val h = 360
                val fis = FileInputStream(File(YUV_PATH))
                val y = ByteArray(w * h)
                val u = ByteArray(w * h / 4)
                val v = ByteArray(w * h / 4)

                while (true) {
                    val ry = fis.read(y)
                    val ru = fis.read(u)
                    val rv = fis.read(v)
                    if (ry > 0 && ru > 0 && rv > 0) {
                        yuvview.setFrameData(w, h, y, u, v)
                        Thread.sleep(40)
                    } else {
                        Log.d("Ray_yuv", "完成")
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

}
