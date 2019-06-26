package com.hikobe8.androidmediaproject.opengl.texture

import android.content.Context
import android.opengl.GLES20
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
class TextureRenderer(context: Context) : RayRenderer {

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
    private var mFragmentShaderName: String? = null

    fun setFragmentShaderName(fragmentShader: String) {
        mFragmentShaderName = fragmentShader
    }

    fun setTextureId(textureId: Int) {
        mTextureId = textureId
    }


    override fun onSurfaceCreated() {
        GLES20.glClearColor(0.8f, 0.8f, 0.8f, 1f)
        val vertexShader = ShaderUtil.loadShader(mContext, "texture/f_vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader =
            ShaderUtil.loadShader(mContext, mFragmentShaderName ?: "texture/t_fragment.glsl", GLES20.GL_FRAGMENT_SHADER)
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
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        /**
         * 多 sampler 的 FBO 时注意一定要激活这个GL_TEXTURE0
         */
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
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
    }

}