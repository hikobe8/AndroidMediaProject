package com.hikobe8.androidmediaproject.opengl.basic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_open_glbasic_draw.*

/***
 *  Author : yurui@palmax.cn
 *  Create at 2018/12/26 16:28
 *  description : OpenGLES 的基本图形绘制
 */
class OpenGLBasicDrawActivity : AppCompatActivity() {

    companion object {

        const val REQ_CODE = 0x1

        fun launch(context: Context) {
            val intent = Intent(context, OpenGLBasicDrawActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var mType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_glbasic_draw)
        mType = savedInstanceState?.getInt("type")?:0
        gl_content.setShape(BasicRendererFactory.createRenderer(this, mType))
        btn_switch.setOnClickListener{
            BasicGeometricChoiceActivity.launch(this, REQ_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        gl_content.onResume()
    }

    override fun onPause() {
        super.onPause()
        gl_content.onPause()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.putInt("type", mType)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK && data?.hasExtra("type") == true) {
            mType = data.getIntExtra("type", -1)
            gl_content.setShape(BasicRendererFactory.createRenderer(this, mType))
        }
    }

}

/***
 *  Author : yurui@palmax.cn
 *  Create at 2018/12/27 10:18
 *  description : 基本几何图形的工厂类 - 创建基本几何图形的renderer
 */
class BasicRendererFactory {

    companion object {

        const val TYPE_TRIANGLE = 0
        const val TYPE_ISOSCELES_TRIANGLE = 1
        const val TYPE_RECT = 2

        fun createRenderer(context: Context, type: Int):Shape {
            return when(type) {
                TYPE_TRIANGLE -> TriangleShape(context)

                TYPE_RECT -> RectShape(context)

                TYPE_ISOSCELES_TRIANGLE -> IsoscelesTriangleShape(context)

                else -> TriangleShape(context)
            }
        }
    }

}
