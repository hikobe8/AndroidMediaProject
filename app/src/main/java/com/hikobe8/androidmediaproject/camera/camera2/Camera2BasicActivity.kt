package com.hikobe8.androidmediaproject.camera.camera2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import com.hikobe8.androidmediaproject.BaseActivity
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_camera2_basic.*

class Camera2BasicActivity : BaseActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, Camera2BasicActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_basic)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, Camera2BasicFragment())
            .commit()
    }

}
