package com.hikobe8.androidmediaproject.opengl.multi_surface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.egl.RayEGLSurfaceView
import com.hikobe8.androidmediaproject.opengl.egl.RayRendererWrapper
import com.hikobe8.androidmediaproject.opengl.texture.FboRenderer
import com.hikobe8.androidmediaproject.opengl.texture.TextureRenderer
import kotlinx.android.synthetic.main.activity_single_texture_multi_surface.*

/***
 *  Author : yurui@palmax.cn
 *  Create at 2019/3/22 17:09
 *  description : 同一纹理渲染到多个Surface上面
 */
class SingleTextureMultiSurfaceActivity : AppCompatActivity(), FboRenderer.OnTextureAvailableListener {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, SingleTextureMultiSurfaceActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_texture_multi_surface)
        surface_origin.setRenderer(RayRendererWrapper(FboRenderer(this).apply {
            mOnTextureAvailableListener = this@SingleTextureMultiSurfaceActivity
        }))
        surface_origin.setRenderMode(RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }

    override fun onTextureAvailable(textureId: Int) {
        runOnUiThread { ll_filters.apply {
            if (childCount > 0) {
                removeAllViews()
            }
            for (i in 0 until 3) {
                val child = MultiGLSurfaceView(context).apply {
                    setSurfaceAndEGLContext(null, surface_origin.getEGLContext())
                    setRenderer(TextureRenderer(context).apply {
                        setFragmentShaderName("texture/fragment_shader${i+1}.glsl")
                        setTextureId(textureId)
                    })
                    setRenderMode(RayEGLSurfaceView.RENDERMODE_WHEN_DIRTY)
                }
                val layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    weight = 1f
                    leftMargin = 10
                    topMargin = 10
                    rightMargin = 10
                    bottomMargin = 10
                }
                addView(child, layoutParams)
            }
        } }
    }

}


