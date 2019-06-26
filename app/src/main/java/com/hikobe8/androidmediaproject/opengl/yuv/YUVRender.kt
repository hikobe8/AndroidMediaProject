package com.hikobe8.androidmediaproject.opengl.yuv

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import com.hikobe8.androidmediaproject.opengl.egl.RayRenderer
import com.hikobe8.androidmediaproject.opengl.texture.TextureRenderer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-06-24 18:19
 *  description :
 */
class YUVRender(private val mContext: Context) : RayRenderer {

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

    private var mVertexBuffer: FloatBuffer? = null
    private var mTextureBuffer: FloatBuffer? = null
    private var mProgram = 0
    private var mVPosition = 0
    private var mFPosition = 0
    private var mMatrixHandle = 0

    private var mSamplerY = 0
    private var mSamplerU = 0
    private var mSamplerV = 0

    private var mTextureYUV: IntArray? = null
    private var mFBOId = 0
    private var mTextureId = 0
    private var mTextureRenderer: TextureRenderer = TextureRenderer(mContext)


    private var mYBufferData: Buffer? = null
    private var mUBufferData: Buffer? = null
    private var mVBufferData: Buffer? = null

    private var mMatrix = FloatArray(16)

    override fun onSurfaceCreated() {
        mTextureRenderer.onSurfaceCreated()
        val vertexShader = ShaderUtil.loadShader(mContext, "texture/vertex_shader_yuv.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader =
            ShaderUtil.loadShader(mContext, "texture/fragment_shader_yuv.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mVPosition = GLES20.glGetAttribLocation(mProgram, "v_Position")
        mFPosition = GLES20.glGetAttribLocation(mProgram, "f_Position")
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_Matrix")

        mSamplerY = GLES20.glGetUniformLocation(mProgram, "sampler_y")
        mSamplerU = GLES20.glGetUniformLocation(mProgram, "sampler_u")
        mSamplerV = GLES20.glGetUniformLocation(mProgram, "sampler_v")

        mTextureYUV = IntArray(3)
        GLES20.glGenTextures(3, mTextureYUV, 0)
        for (i in 0 until 3) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureYUV!![i])
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        mVertexBuffer = ByteBuffer
            .allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(COORDS).apply { position(0) }
        mTextureBuffer = ByteBuffer
            .allocateDirect(TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEXTURE_COORDS).apply { position(0) }
        Matrix.setIdentityM(mMatrix, 0)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        Matrix.rotateM(mMatrix, 0, 180f, 1f, 0f, 0f)
        GLES20.glViewport(0, 0, width, height)
        mTextureRenderer.onSurfaceSizeChanged(width, height)
        createFBO(width, height)
        mTextureRenderer.setTextureId(mTextureId)
    }

    private fun createFBO(width: Int, height: Int) {
        //创建fbo
        val fbos = IntArray(1)
        GLES20.glGenBuffers(1, fbos, 0)
        mFBOId = fbos[0]
        //绑定fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOId)

        //创建与FBO绑定的纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        mTextureId = textures[0]
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId)
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            mTextureId,
            0
        )
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("FboRenderer", "fbo wrong")
        } else {
            Log.e("FboRenderer", "fbo success")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private var mWidth = 0
    private var mHeight = 0

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOId)
        if (mWidth > 0 && mHeight > 0
            && mYBufferData != null && mUBufferData != null && mYBufferData != null
        ) {
            GLES20.glUseProgram(mProgram)
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
            GLES20.glEnableVertexAttribArray(mVPosition)
            GLES20.glVertexAttribPointer(mVPosition, 2, GLES20.GL_FLOAT, false, 8, mVertexBuffer)
            GLES20.glEnableVertexAttribArray(mFPosition)
            GLES20.glVertexAttribPointer(mFPosition, 2, GLES20.GL_FLOAT, false, 8, mTextureBuffer)
            //y
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureYUV!![0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, mWidth, mHeight,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mYBufferData
            )
            GLES20.glUniform1i(mSamplerY, 0)

            //u
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureYUV!![1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, mWidth / 2, mHeight / 2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mUBufferData
            )
            GLES20.glUniform1i(mSamplerU, 1)

            //v
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureYUV!![2])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, mWidth / 2, mHeight / 2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mVBufferData
            )
            GLES20.glUniform1i(mSamplerV, 2)

            mYBufferData?.clear()
            mUBufferData?.clear()
            mVBufferData?.clear()

            mYBufferData = null
            mUBufferData = null
            mVBufferData = null

        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDisableVertexAttribArray(mVPosition)
        GLES20.glDisableVertexAttribArray(mFPosition)
        mTextureRenderer.onDraw()
    }

    fun setFrameData(w: Int, h: Int, by: ByteArray, bu: ByteArray, bv: ByteArray) {
        mWidth = w
        mHeight = h
        mYBufferData = ByteBuffer.wrap(by)
        mUBufferData = ByteBuffer.wrap(bu)
        mVBufferData = ByteBuffer.wrap(bv)
    }

}