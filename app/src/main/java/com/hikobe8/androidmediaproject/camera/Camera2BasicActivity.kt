package com.hikobe8.androidmediaproject.camera

import android.os.Bundle
import android.support.design.widget.Snackbar
import com.hikobe8.androidmediaproject.BaseActivity
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_camera2_basic.*

class Camera2BasicActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_basic)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

}
