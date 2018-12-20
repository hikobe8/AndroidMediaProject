package com.hikobe8.androidmediaproject.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
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

    private val mFaceDetectionListener:Camera.FaceDetectionListener = Camera.FaceDetectionListener { faces, camera ->
        if (faces.isNotEmpty()) {
            val result = RectF()
            matrix?.mapRect(result, RectF(faces[0].rect))
            Log.d("FaceDetection", ("face detected: ${faces.size}" +
                    " Face 1 Location X: ${result.centerX()}" +
                    " Y: ${result.centerY()}"))
            face_detection_view.update(result)
        }
    }

    private var matrix: Matrix? = null

    private fun startCameraPreview() {
        if (!CameraUtils.checkCameraHardware(this)) {
            finish()
        } else {
            //Create an instance of Camera
            mCamera = CameraUtils.getCameraInstance()
            mPreview = mCamera?.let {
                it.setFaceDetectionListener(mFaceDetectionListener)
                val displayOrientation = CameraUtils.setCameraDisplayOrientation(this, 1, it)
                val info:Camera.CameraInfo = Camera.CameraInfo()
                info.let {
                    Camera.getCameraInfo(1, it)
                }
                // Need mirror for front camera.
                matrix = Matrix()
                val mirror = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                matrix?.setScale(if (mirror) -1f else 1f, 1f)
                // This is the value for android.hardware.Camera.setDisplayOrientation.
                matrix?.postRotate(displayOrientation.toFloat())
                // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
                // UI coordinates range from (0, 0) to (width, height).
                //这里写死的，测试机为小米note3 1920*1080
                matrix?.postScale(1080 / 2000f, 1920 / 2000f)
                matrix?.postTranslate(1080 / 2f, 1920 / 2f)
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

    private val mPictureCallback = Camera.PictureCallback { data, _ ->
        Observable.create(ObservableOnSubscribe<String> {
            val pictureFile = File(FileUtils.getCameraDir(), System.currentTimeMillis().toString() + ".jpeg")
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
