package com.hikobe8.androidmediaproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-07 17:02
 *  description :
 */

fun ViewGroup.inflate(resource: Int, root: ViewGroup, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(resource, root, attachToRoot)