package com.hikobe8.androidmediaproject.opengl.basic

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.hikobe8.androidmediaproject.R

class BasicGeometricChoiceActivity : AppCompatActivity() {

    companion object {

        fun launch(activity: AppCompatActivity, reqCode:Int) {
            activity.startActivityForResult(Intent(activity, BasicGeometricChoiceActivity::class.java), reqCode)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_geometric_choice)
    }

    fun onClickedShape(view: View) {
        val type = when(view.id) {
            R.id.btn_triangle -> BasicRendererFactory.TYPE_TRIANGLE
            R.id.btn_rect -> BasicRendererFactory.TYPE_RECT
            R.id.btn_isosceles -> BasicRendererFactory.TYPE_ISOSCELES_TRIANGLE
            else -> BasicRendererFactory.TYPE_TRIANGLE
        }
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("type", type)
        })
        finish()
    }

}
