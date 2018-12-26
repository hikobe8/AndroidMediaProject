package com.hikobe8.androidmediaproject.opengl.common

import android.content.Context
import android.opengl.GLES20
import android.text.TextUtils
import android.util.Log
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-26 17:07
 *  description : 加载shader的工具类
 */
class ShaderUtil {

    companion object {

        const val TAG = "ShaderUtil"

        /**
         * 加载glsl代码到内存中，并没有写成异步方法，glsl代码多了可能会ANR
         * @param context
         * @param path glsl 代码的assets路径
         */
        private fun loadShaderString(context: Context, path: String): String {
            val strBuilder = StringBuilder()
            var ais: BufferedReader? = null
            try {
                ais = BufferedReader(InputStreamReader(context.assets.open(path)))
                var eof = false
                while (!eof) {
                    val line = ais.readLine()
                    eof = TextUtils.isEmpty(line)
                    if (!eof)
                        strBuilder.append(line).append("\n")
                }
                strBuilder.deleteCharAt(strBuilder.length - 1)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "load shader : $path failed!")
            } finally {
                ais?.close()
            }
            return strBuilder.toString()
        }

        fun loadShader(context: Context, path: String, type: Int): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, loadShaderString(context, path))
            GLES20.glCompileShader(shader)
            return shader
        }

    }

}