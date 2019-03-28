package com.hikobe8.androidmediaproject.opengl.egl.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.hikobe8.androidmediaproject.opengl.camera.CameraRenderer
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import com.hikobe8.androidmediaproject.opengl.egl.RayRenderer
import com.hikobe8.androidmediaproject.opengl.texture.TextureRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-27 10:22
 *  description : 相机渲染Renderer,带FBO
 */
class RayCameraRenderer(context: Context) : RayRenderer, SurfaceTexture.OnFrameAvailableListener {

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {

    }

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

    private var mMatrixHandle = -1

    private val mContext = context.applicationContext
    private var mProgram = -1
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureVertexBuffer: FloatBuffer
    private var mVboId: Int = -1
    private var mFboId: Int = -1
    private var mFboTextureId: Int = -1
    private var mSurfaceTexture: SurfaceTexture? = null
    var onSurfaceCreateListener: OnSurfaceCreateListener? = null
    private val mTextureRender = TextureRenderer(context)
    private val mMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )


    override fun onSurfaceCreated() {
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        mTextureRender.onSurfaceCreated()
        val vertexShader = ShaderUtil.loadShader(mContext!!, "texture/t_vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadShader(mContext, "camera/fragment_camera.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")

        mVertexBuffer = ByteBuffer
            .allocateDirect(CameraRenderer.COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(CameraRenderer.COORDS)
        mVertexBuffer.position(0)
        mTextureVertexBuffer = ByteBuffer
            .allocateDirect(CameraRenderer.TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(CameraRenderer.TEXTURE_COORDS)
        mTextureVertexBuffer.position(0)
        //create VBO
        createVBO()
    }

    private fun createVBO() {
        val vbos = IntArray(1)
        GLES20.glGenBuffers(1, vbos, 0)
        mVboId = vbos[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            COORDS.size * 4 + TEXTURE_COORDS.size * 4,
            null,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER,
            0,
            COORDS.size * 4,
            mVertexBuffer
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER,
            COORDS.size * 4,
            TEXTURE_COORDS.size * 4,
            mTextureVertexBuffer
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        mTextureRender.onSurfaceSizeChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
        //create FBO
        createFBO(width, height)
        mTextureRender.setTextureId(mFboTextureId)
        //create Camera Texture
        val cameraTextureIds = IntArray(1)
        GLES20.glGenTextures(1, cameraTextureIds, 0)
        mTextureId = cameraTextureIds[0]
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId)
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        mSurfaceTexture = SurfaceTexture(mTextureId)
        mSurfaceTexture!!.setOnFrameAvailableListener(this)
        onSurfaceCreateListener?.onSurfaceCreate(mSurfaceTexture!!)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        Matrix.rotateM(mMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.scaleM(mMatrix, 0, 1f, -1f,  1f)
    }


    private fun createFBO(width: Int, height: Int) {
        //创建fbo
        val fbos = IntArray(1)
        GLES20.glGenBuffers(1, fbos, 0)
        mFboId = fbos[0]
        //绑定fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId)

        //创建与FBO绑定的纹理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        mFboTextureId = textures[0]
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId)
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
            mFboTextureId,
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


    private var mTextureId: Int = -1

    override fun onDraw() {
        //更新数据
        mSurfaceTexture?.updateTexImage()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        GLES20.glUseProgram(mProgram)
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 8, COORDS.size * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        //draw fbo
        mTextureRender.onDraw()
    }

    interface OnSurfaceCreateListener {
        fun onSurfaceCreate(surfaceTexture: SurfaceTexture)
    }
}