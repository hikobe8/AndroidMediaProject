package com.hikobe8.androidmediaproject.camera.camera2

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.OrientationEventListener
import com.hikobe8.androidmediaproject.BaseActivity
import com.hikobe8.androidmediaproject.R


class Camera2BasicActivity : BaseActivity() {

    companion object {
        private const val TAG = "Camera2BasicActivity"
        fun launch(context: Context) {
            context.startActivity(Intent(context, Camera2BasicActivity::class.java))
        }
    }

    var mOrientationListener: OrientationEventListener? = null

    var mOritentation = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_basic)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, Camera2BasicFragment())
            .commit()

        mOrientationListener = object : OrientationEventListener(
            this,
            SensorManager.SENSOR_DELAY_NORMAL
        ) {

            override fun onOrientationChanged(orientation: Int) {
                when (orientation) {
                    in 0..45 -> mOritentation = 90
                    in 46..90 -> mOritentation = 0
                    in 91..135 -> mOritentation = 0
                    in 136..180 -> mOritentation = 90
                    in 181..225 -> mOritentation = 90
                    in 226..270 -> mOritentation = 0
                    in 271..360 -> mOritentation = 90
                }
            }
        } as OrientationEventListener

        if (mOrientationListener!!.canDetectOrientation()) {
            Log.v(TAG, "Can detect orientation")
            mOrientationListener!!.enable()
        } else {
            Log.v(TAG, "Cannot detect orientation")
            mOrientationListener!!.disable()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mOrientationListener?.disable()
    }

}
