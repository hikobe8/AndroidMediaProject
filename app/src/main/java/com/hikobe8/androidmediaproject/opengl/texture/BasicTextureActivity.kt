package com.hikobe8.androidmediaproject.opengl.texture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import kotlinx.android.synthetic.main.activity_basic_texture.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BasicTextureActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, BasicTextureActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_texture)
        gl_content1.setEGLContextClientVersion(2)
        gl_content1.setRenderer(BasicTextureRenderer(this, R.drawable.batman))
        gl_content1.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        gl_content2.setEGLContextClientVersion(2)
        gl_content2.setRenderer(BasicTextureRenderer(this, R.drawable.fengj))
        gl_content2.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
}

class BasicTextureRenderer(context: Context, resId:Int = R.drawable.portrait) : GLSurfaceView.Renderer {

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

    private var mResId = resId

    //glsl 位置坐标句柄,用于设置坐标
    private var mPositionHandle = -1

    private var mMatrixHandle = -1

    private var mTextureCoordinateHandle = -1

    private var mGlTextureSamplerHandle = -1

    private val mContext = context.applicationContext
    private var mProgram = -1
    private lateinit var mBitmap: Bitmap
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureVertexBuffer: FloatBuffer
    private val mMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.8f, 0.8f, 0.8f, 1f)
        val vertexShader = ShaderUtil.loadShader(mContext, "texture/t_vertex.glsl", GLES20.GL_VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadShader(mContext, "texture/t_fragment.glsl", GLES20.GL_FRAGMENT_SHADER)
        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        GLES20.glLinkProgram(mProgram)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        mGlTextureSamplerHandle = GLES20.glGetUniformLocation(mProgram, "vTexture")

        mBitmap = BitmapFactory.decodeResource(mContext.resources, mResId)
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

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        if (width > height) {
            //横屏
            val aspectRatio = width.toFloat() / height
            val imgWidth = mBitmap.width
            val imgHeight = mBitmap.height
            if (imgWidth > imgHeight) {
                //宽图
                val imgAspectRatio = imgWidth.toFloat() / imgHeight
                Matrix.orthoM(mMatrix, 0, -1f, 1f, -imgAspectRatio/aspectRatio, imgAspectRatio/aspectRatio, -1f, 1f)
            } else {
                //长图
                val imgAspectRatio = imgHeight.toFloat() / imgWidth
                Matrix.orthoM(mMatrix, 0, -aspectRatio * imgAspectRatio, aspectRatio * imgAspectRatio, -1f, 1f, -1f, 1f)
            }


        } else {
            //竖屏
            val aspectRatio = height.toFloat() / width
            val imgWidth = mBitmap.width
            val imgHeight = mBitmap.height
            if (imgWidth >= imgHeight) {
                //宽图
                val imgAspectRatio = imgWidth.toFloat() / imgHeight
                Matrix.orthoM(mMatrix, 0, -1f, 1f, -imgAspectRatio*aspectRatio, imgAspectRatio*aspectRatio, -1f, 1f)
            } else {
                //长图
                val imgAspectRatio = imgHeight.toFloat() / imgWidth
                Matrix.orthoM(mMatrix, 0, -imgAspectRatio/aspectRatio, imgAspectRatio/aspectRatio, -1f, 1f, -1f, 1f)
            }


        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        GLES20.glUniform1i(mGlTextureSamplerHandle, 0)
        createTexture()
        //创建纹理
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

    private fun createTexture(): Int {
        val texture = IntArray(1)
        if (!mBitmap.isRecycled) {
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
            return texture[0]
        }
        return 0
    }

}