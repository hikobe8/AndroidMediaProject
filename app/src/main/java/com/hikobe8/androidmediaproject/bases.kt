package com.hikobe8.androidmediaproject

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-07 17:05
 *  description : 各种基类
 */

open class BaseActivity: AppCompatActivity() {

    protected val mActivity by lazy {
        this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity
    }

    fun setHomeAsUpEnabled(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun onHomeItemSelected(item: MenuItem?) {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        onHomeItemSelected(item)
        return super.onOptionsItemSelected(item)
    }

}