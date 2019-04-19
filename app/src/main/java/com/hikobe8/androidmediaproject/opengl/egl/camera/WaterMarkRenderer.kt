package com.hikobe8.androidmediaproject.opengl.egl.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import com.hikobe8.androidmediaproject.opengl.egl.RayRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-22 16:27
 *  description :
 */
class WaterMarkRenderer(context: Context) : RayRenderer {

    companion object {
        val COORDS = floatArrayOf(
            -1f, 1f, //left top
            -1f, -1f, //left bottom
            1f, 1f, //right top
            1f, -1f //right bottom
        )

        const val COUNT_PER_COORD = 2

        val TEXTURE_COORDS = floatArrayOf(
            0f, 0f, //left top
            0f, 1f,  //left bottom
            1f, 0f,  //right top
            1f, 1f //right bottom
        )
    }

    //glsl 位置坐标句柄,用于设置坐标
    private var mPositionHandle = -1

    private var mTextureCoordinateHandle = -1

    private var mGlTextureSamplerHandle = -1

    private val mContext = context.applicationContext
    private var mProgram = -1
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureVertexBuffer: FloatBuffer
    private var mTextureId = -1

    override fun onSurfaceCreated() {
        val vertexShader = ShaderUtil.loadShader(mContext, "texture/f_vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader =
            ShaderUtil.loadShader(mContext, "texture/t_fragment.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
        mGlTextureSamplerHandle = GLES20.glGetUniformLocation(mProgram, "vTexture")

        mVertexBuffer = ByteBuffer
            .allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(COORDS)
        mVertexBuffer.position(0)
        mTextureVertexBuffer = ByteBuffer
            .allocateDirect(TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEXTURE_COORDS)
        mTextureVertexBuffer.position(0)
        mBitmap = BitmapFactory.decodeResource(mContext.resources, R.drawable.portrait)
        mTextureId = createImageTexture()
    }

    private var mWidth:Int = 0
    private var mHeight:Int = 0

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        mWidth = width
        mHeight = height
//        GLES20.glViewport((width/2f).toInt(), (height/2f).toInt(), (width/2f).toInt() + mBitmap!!.width, (height/2f).toInt() + mBitmap!!.height)
    }

    override fun onDraw() {
        GLES20.glViewport(100, 200, 200, 300)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD)
        GLES20.glUseProgram(mProgram)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, COUNT_PER_COORD, GLES20.GL_FLOAT, false, 0, mVertexBuffer)
        GLES20.glVertexAttribPointer(
            mTextureCoordinateHandle,
            COUNT_PER_COORD,
            GLES20.GL_FLOAT,
            false,
            0,
            mTextureVertexBuffer
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glViewport(0, 0, mWidth, mHeight)
    }

    private var mBitmap: Bitmap? = null

    private fun createImageTexture(): Int {
        val texture = IntArray(1)
        if (mBitmap != null && !mBitmap!!.isRecycled) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0)
            //绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return texture[0]
        }
        return 0
    }

}