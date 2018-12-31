package com.hikobe8.androidmediaproject.opengl.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-29 14:25
 *  description : 相机预览类，基于GlSurfaceView实现
 */
class CameraView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer {

    private var mCameraId = 1

    constructor(context: Context?) : this(context, null)

    private var mCamera: Camera? = null

    private var mCameraRenderer = CameraRenderer(context)

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mCameraRenderer.onSurfaceCreated(gl, config)
        mCamera = Camera.open(mCameraId)
        //初始化相机
        val param = mCamera?.parameters
        param?.setPreviewSize(1280, 720)
        param?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        mCamera?.parameters = param
        mCamera?.setPreviewTexture(mCameraRenderer.mSurfaceTexture)
        mCameraRenderer.mSurfaceTexture?.setOnFrameAvailableListener {
            requestRender()
        }
        mCamera?.startPreview()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mCameraRenderer.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        mCameraRenderer.onDrawFrame(gl)
    }
}

/***
 *  Author : yurui@palmax.cn
 *  Create at 2018/12/29 18:07
 *  description : 相机renderer
 */
class CameraRenderer(context: Context?) : GLSurfaceView.Renderer {

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

    private var mMatrixHandle = -1

    private var mTextureCoordinateHandle = -1

    private var mGlTextureSamplerHandle = -1

    private val mContext = context?.applicationContext
    private var mProgram = -1
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureVertexBuffer: FloatBuffer

    var mSurfaceTexture: SurfaceTexture? = null

    private var mTextureId: Int = -1

    private val mMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        val vertexShader = ShaderUtil.loadShader(mContext!!, "texture/t_vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadShader(mContext, "camera/fragment_camera.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
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

        mTextureId = createTexture()
        mSurfaceTexture = SurfaceTexture(mTextureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        if (width > height) {
            //横屏
            val aspectRatio = width.toFloat() / height
            Matrix.orthoM(mMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        } else {
            //竖屏
            val aspectRatio = height.toFloat() / width
            Matrix.orthoM(mMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f)
        }
        Matrix.orthoM(mMatrix, 0, -1f, 1f, -1f,1f, -1f, 1f)
//        Matrix.rotateM(mMatrix, 0, 270f, 0f, 0f, 1f)
        Matrix.scaleM(mMatrix, 0, -1f, 1f, 1f) //绕x轴反转前置摄像头
        Matrix.rotateM(mMatrix, 0, 90f, 0f, 0f, 1f) //旋转前置摄像头
    }

    override fun onDrawFrame(gl: GL10?) {
        mSurfaceTexture?.updateTexImage()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId)
        GLES20.glUniform1i(mGlTextureSamplerHandle, 0)
        //创建纹理
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            COUNT_PER_COORD, GLES20.GL_FLOAT, false, 0, mVertexBuffer
        )
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
    }


    private fun createTexture(): Int {
        val texture = IntArray(1)
        //生成纹理
        GLES20.glGenTextures(1, texture, 0)
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return texture[0]
    }

}