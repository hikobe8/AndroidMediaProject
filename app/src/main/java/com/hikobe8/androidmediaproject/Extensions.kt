package com.hikobe8.androidmediaproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-07 17:02
 *  description :
 */

fun ViewGroup.inflate(resource: Int, root: ViewGroup, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(resource, root, attachToRoot)

fun Long.formatTime(): String {
    val value = this
    val timeBuilder = StringBuilder()
    when {
        value < 60 -> timeBuilder
            .append(value)
            .append("s")
        value < 3600 -> timeBuilder
            .append(value / 60)
            .append("m")
            .append(value % 60)
            .append("s")
        value < 86400 -> timeBuilder
            .append(value / 3600)
            .append("h")
            .append(value / 60 % 60)
            .append("m")
            .append(value % 60)
            .append("s")
        else -> throw IllegalArgumentException("value is too large to format!")
    }
    return timeBuilder.toString()
}