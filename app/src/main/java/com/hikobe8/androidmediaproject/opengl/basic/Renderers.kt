package com.hikobe8.androidmediaproject.opengl.basic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.hikobe8.androidmediaproject.opengl.basic.RectShape.Companion.COLOR
import com.hikobe8.androidmediaproject.opengl.basic.TriangleShape.Companion.COORDS_COUNT
import com.hikobe8.androidmediaproject.opengl.basic.TriangleShape.Companion.COORDS_PER_VERTEX
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class Shape(context: Context?) : GLSurfaceView.Renderer {

    protected val mContext = context?.applicationContext!!
    protected var mProgram: Int = 0

    protected var mCoordsBuffer: FloatBuffer? = null

    //glsl 位置坐标句柄,用于设置坐标
    protected var mPositionHandle: Int = 0
    //glsl 片元颜色句柄,用于设置颜色
    protected var mColorHandle: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        val vertexShader = ShaderUtil.loadShader(mContext, "basic/vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadShader(mContext, "basic/fragment.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
    }

}

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-26 17:38
 *  description : 三角形
 */
class TriangleShape(context: Context?) : Shape(context) {

    companion object {
        const val COORDS_PER_VERTEX = 3 //每个坐标3个值 x, y, z
        val COORDS: FloatArray = floatArrayOf(
            0.5f, 0.5f, 0.0f, // top
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f  // bottom right
        )
        val COORDS_COUNT = COORDS.size / COORDS_PER_VERTEX // 坐标个数
        val COLOR = floatArrayOf(1f, 0f, 0f, 1f) // R G B A
    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        //申请底层空间
        mCoordsBuffer = ByteBuffer
            .allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mCoordsBuffer?.put(COORDS)
        mCoordsBuffer?.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            mCoordsBuffer
        )
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, COORDS_COUNT)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

}


/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-26 17:38
 *  description : 矩形
 */
class RectShape(context: Context) : Shape(context) {

    companion object {
        const val COORDS_PER_VERTEX = 3 //每个坐标3个值 x, y, z
        val COORDS: FloatArray = floatArrayOf(
            0.5f, 0.5f, 0.0f,   // top right
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f,  // bottom right
            0.5f, 0.5f, 0.0f,   // top right
            -0.5f, 0.5f, 0.0f,    // top left
            -0.5f, -0.5f, 0.0f  // bottom left
        )
        val COORDS_COUNT = COORDS.size / COORDS_PER_VERTEX // 坐标个数
        val COLOR = floatArrayOf(1f, 0f, 0f, 1f) // R G B A
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        //申请底层空间
        mCoordsBuffer = ByteBuffer
            .allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mCoordsBuffer?.put(COORDS)
        mCoordsBuffer?.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            mCoordsBuffer
        )
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, COORDS_COUNT)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

}

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-26 17:38
 *  description : 等腰三角形
 */
class IsoscelesTriangleShape(context: Context?) : Shape(context) {

    private var mMatrixHandle = 0
    private val mMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        val vertexShader = ShaderUtil.loadShader(mContext, "basic/vertex_matrix.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadShader(mContext, "basic/fragment.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        //申请底层空间
        mCoordsBuffer = ByteBuffer
            .allocateDirect(TriangleShape.COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mCoordsBuffer?.put(TriangleShape.COORDS)
        mCoordsBuffer?.position(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        var aspectRatio = 1f
        if (width > height) {
            //横屏
            aspectRatio = width.toFloat() / height
            Matrix.orthoM(mMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        } else {
            //竖屏
            aspectRatio = height.toFloat() / width
            Matrix.orthoM(mMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4, mCoordsBuffer
        )
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, COORDS_COUNT)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

}