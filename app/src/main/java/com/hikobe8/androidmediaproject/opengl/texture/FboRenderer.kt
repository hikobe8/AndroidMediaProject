package com.hikobe8.androidmediaproject.opengl.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.hikobe8.androidmediaproject.R
import com.hikobe8.androidmediaproject.opengl.common.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2019-03-22 16:26
 *  description :
 */
class FboRenderer(context: Context, resId: Int = R.mipmap.ic_launcher) : GLSurfaceView.Renderer {

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

    private var mTextureCoordinateHandle = -1

    private var mGlTextureSamplerHandle = -1

    private var mMatrixHandle = -1

    private val mContext = context.applicationContext
    private var mProgram = -1
    private lateinit var mBitmap: Bitmap
    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureVertexBuffer: FloatBuffer
    private var mTextureRenderer: TextureRenderer = TextureRenderer(context)
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
        mGlTextureSamplerHandle = GLES20.glGetUniformLocation(mProgram, "vTexture")
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        mBitmap = BitmapFactory.decodeResource(mContext.resources, mResId)
        mVertexBuffer = ByteBuffer
            .allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(COORDS).apply {
                position(0)
            }
        mTextureVertexBuffer = ByteBuffer
            .allocateDirect(TEXTURE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEXTURE_COORDS).apply {
                position(0)
            }
        mImageTextureId = createImageTexture()
        mTextureRenderer.onSurfaceCreated()
    }

    /**
     * 使用当前surface的尺寸创建FBO,因为使用图片的原始尺寸创建的话图片特别大的时候失真很严重
     */
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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(mGlTextureSamplerHandle, 0)
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

    private var mFboId: Int = -1
    private var mFboTextureId: Int = -1
    private var mImageTextureId: Int = -1

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        mTextureRenderer.onSurfaceSizeChanged(width, height)
        createFBO(width, height)
        mOnTextureAvailableListener?.onTextureAvailable(mFboTextureId)
        mTextureRenderer.setTextureId(mFboTextureId)
        if (width > height) {
            //横屏
            val aspectRatio = width.toFloat() / height
            val imgWidth = mBitmap.width
            val imgHeight = mBitmap.height
            if (imgWidth > imgHeight) {
                //宽图
                val imgAspectRatio = imgWidth.toFloat() / imgHeight
                Matrix.orthoM(mMatrix, 0, -1f, 1f, -imgAspectRatio / aspectRatio, imgAspectRatio / aspectRatio, -1f, 1f)
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
                Matrix.orthoM(mMatrix, 0, -1f, 1f, -imgAspectRatio * aspectRatio, imgAspectRatio * aspectRatio, -1f, 1f)
            } else {
                //长图
                val imgAspectRatio = imgHeight.toFloat() / imgWidth
                Matrix.orthoM(mMatrix, 0, -imgAspectRatio / aspectRatio, imgAspectRatio / aspectRatio, -1f, 1f, -1f, 1f)
            }
        }
        Matrix.rotateM(mMatrix, 0, 180f, 1f, 0f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mImageTextureId)
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
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD)
        mTextureRenderer.onDraw()
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    interface OnTextureAvailableListener {
        fun onTextureAvailable(textureId: Int)
    }

    var mOnTextureAvailableListener: OnTextureAvailableListener? = null

    private fun createImageTexture(): Int {
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
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return texture[0]
        }
        return 0
    }

}