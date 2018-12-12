package com.hikobe8.androidmediaproject.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.hikobe8.androidmediaproject.CameraUtils
import com.hikobe8.androidmediaproject.FileUtils
import com.hikobe8.androidmediaproject.PermissionUtils
import com.hikobe8.androidmediaproject.R
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_camera_basic.*
import java.io.File
import java.io.FileOutputStream

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
                CameraUtils.setCameraDisplayOrientation(this, 1, it)
                CameraPreview(this, it)
            }
            mPreview?.also {
                camera_preview.addView(it)
            }
            button_capture.setOnClickListener {
                mCamera?.takePicture(null, null, mPictureCallback)
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
            Camera.open(1) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private val mPictureCallback = Camera.PictureCallback { data, _ ->
        Observable.create(ObservableOnSubscribe<String> {
            val pictureFile = File(FileUtils.getStorageDir(), System.currentTimeMillis().toString() + ".jpeg")
            if (!pictureFile.exists())
                pictureFile.createNewFile()
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(pictureFile)
                fos.write(data)
                fos.flush()
                it.onNext(pictureFile.absolutePath)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
            it.onComplete()
        })
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Toast.makeText(this, "Picture saved : $it ", Toast.LENGTH_SHORT).show()
            }
    }

}
