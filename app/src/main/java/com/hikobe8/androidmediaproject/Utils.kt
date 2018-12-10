package com.hikobe8.androidmediaproject

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresApi
import java.io.File

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-10 11:08
 *  description : 工具类集合
 */

class PermissionUtils {

    companion object {

        @RequiresApi(Build.VERSION_CODES.M)
        fun checkPermissionGranted(context: Context, permission:String): Boolean {
                val checkSelfPermission = context.checkSelfPermission(permission)
                return checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun requestPermission(activity:Activity, permissions:Array<String>, reqCode:Int) {
            activity.requestPermissions(permissions, reqCode)
        }
    }

}

class FileUtils {
    companion object {
        fun getStorageDir(): String {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath, BuildConfig.APPLICATION_ID)
            if (!dir.exists())
                dir.mkdirs()
            return dir.absolutePath
        }
    }
}
