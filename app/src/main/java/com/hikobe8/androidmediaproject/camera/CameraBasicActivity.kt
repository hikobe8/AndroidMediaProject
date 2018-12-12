package com.hikobe8.androidmediaproject.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hikobe8.androidmediaproject.CameraUtils
import com.hikobe8.androidmediaproject.PermissionUtils
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_camera_basic.*

class CameraBasicActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, CameraBasicActivity::class.java))
        }
    }

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionArray: ArrayList<String> = ArrayList()
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.CAMERA)) {
                permissionArray.add(Manifest.permission.CAMERA)
            }
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
                permissionArray.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!PermissionUtils.checkPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissionArray.size < 1) {
                initViews()
            } else {
                PermissionUtils.requestPermission(this, permissionArray.toArray(arrayOf()), 10)
            }
        } else {
            initViews()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.size > 1) {
            initViews()
        } else {
            onBackPressed()
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_camera_basic)
        startCameraPreview()
    }

    private fun startCameraPreview() {
        if (!checkCameraHardware(this)) {
            finish()
        } else {
            //Create an instance of Camera
            mCamera = getCameraInstance()
            mPreview = mCamera?.let {
                CameraUtils.setCameraDisplayOrientation(this, 0, it)
                CameraPreview(this, it)
            }
            mPreview?.also {
                camera_preview.addView(it)
            }
        }
    }

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    /** A safe way to get an instance of the Camera object. */
    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(0) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

}
