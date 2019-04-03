package com.hikobe8.androidmediaproject.opengl.record

import android.content.Context
import com.hikobe8.androidmediaproject.opengl.texture.TextureRenderer

/**
 * Author : hikobe8@github.com
 * Time : 2019/4/3 11:33 PM
 * Description :
 */
class RayMediaEncodec(context: Context, textureId: Int) : RayBaseMediaEncoder() {

    init {
        val encodecRenderer = TextureRenderer(context).apply {
            setTextureId(textureId)
        }
        setRenderer(encodecRenderer)
    }


}