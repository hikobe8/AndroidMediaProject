package com.hikobe8.androidmediaproject.draw

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.hikobe8.androidmediaproject.BaseActivity
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_draw_image.*

class DrawImageActivity : BaseActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, DrawImageActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_image)
        setSupportActionBar(toolbar)
        setHomeAsUpEnabled()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_draw_image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        onHomeItemSelected(item)
        when(item?.itemId) {
            R.id.menu_view -> {

            }
            R.id.menu_surface -> {

            }
        }
        return true
    }

}
