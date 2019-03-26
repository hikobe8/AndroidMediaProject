package com.hikobe8.androidmediaproject.opengl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.basic.OpenGLBasicDrawActivity
import com.hikobe8.androidmediaproject.opengl.camera.CameraGLActivity
import com.hikobe8.androidmediaproject.opengl.egl.CustomizedGLSurfaceActivity
import com.hikobe8.androidmediaproject.opengl.egl.EglBasicActivity
import com.hikobe8.androidmediaproject.opengl.multi_texture.SingleSurfaceMultiTextureActivity
import com.hikobe8.androidmediaproject.opengl.multi_surface.SingleTextureMultiSurfaceActivity
import com.hikobe8.androidmediaproject.opengl.texture.BasicTextureActivity
import com.hikobe8.androidmediaproject.opengl.texture.FboTextureActivity

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

    fun onCLicked(v:View) {
        when(v.id) {
            R.id.btn_basic -> OpenGLBasicDrawActivity.launch(this)
            R.id.btn_basic_texture -> BasicTextureActivity.launch(this)
            R.id.btn_camera1 -> CameraGLActivity.launch(this)
            R.id.btn_fbo -> FboTextureActivity.launch(this)
            R.id.btn_egl -> EglBasicActivity.launch(this)
            R.id.btn_customized_gl -> CustomizedGLSurfaceActivity.launch(this)
            R.id.btn_multi_surface -> SingleTextureMultiSurfaceActivity.launch(this)
            R.id.btn_multi_texture -> SingleSurfaceMultiTextureActivity.launch(this)
        }

    }

}