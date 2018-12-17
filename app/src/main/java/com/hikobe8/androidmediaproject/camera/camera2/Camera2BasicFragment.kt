package com.hikobe8.androidmediaproject.camera.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.inflate
import kotlinx.android.synthetic.main.activity_camera2_basic.*
import java.util.*
import kotlin.collections.ArrayList

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-17 16:44
 *  description :
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2BasicFragment : Fragment() {

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.activity_camera2_basic, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener {
            Snackbar.make(it, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureview.isAvailable) {
            openCamera(textureview.width, textureview.height)
        } else {

        }
    }

    override fun onPause() {
        stopBackgroundThread()
        super.onPause()
    }

    /**
     * 初始化并开启后台线程和对应的Handler
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.let {
            it.start()
            mBackgroundHandler = Handler(it.looper)
        }
    }

    /**
     * 关闭后台线程
     */
    private fun stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread?.quitSafely()
        } else {
            mBackgroundThread?.quit()
        }
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e:InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs(width, height)
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager:CameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in manager.cameraIdList) {
            val characteristics:CameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            //这里只使用后置摄像头
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }
            val map:StreamConfigurationMap =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue
            val largest = Collections.max(Arrays.asList(
                map.getOutputSizes(ImageFormat.JPEG)) as ArrayList<Size>, CompareSizesByArea())
            Log.e("test", largest.toString())
            return
        }
    }

    class CompareSizesByArea : Comparator<Size> {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun compare(o1: Size?, o2: Size?): Int {
            return o1?.width!! * o1.height - o2?.width!! * o2.height
        }
    }

}