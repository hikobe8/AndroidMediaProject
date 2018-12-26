package com.hikobe8.androidmediaproject.opengl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.basic.OpenGLBasicDrawActivity

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-26 16:05
 *  description :
 */
class OpenGLMainActivity: AppCompatActivity(){

    companion object {
        fun launch(context:Context) {
            context.startActivity(Intent(context, OpenGLMainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_main)
    }

    fun go2BasicDraw(v:View) {
        OpenGLBasicDrawActivity.launch(this)
    }

}