package com.hikobe8.androidmediaproject.draw

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.hikobe8.androidmediaproject.R
import java.lang.Exception

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-07 18:57
 *  description :
 */
class ImageSurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mIsRunning = false
    }

    private var mIsRunning = false

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mIsRunning = true
        Thread(Runnable {
            while (mIsRunning) {
                val canvas = holder?.lockCanvas()
                if (canvas != null) {
                    try {
                        mDrawable?.draw(canvas)
                    } catch (ex:Exception) {
                        ex.printStackTrace()
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }

            }
        }).start()
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private var mDrawable: Drawable? = null

    init {
        val attributes = context?.obtainStyledAttributes(attrs, R.styleable.AppCompatImageView)
        val resourceID = attributes?.getResourceId(R.styleable.AppCompatImageView_android_src, -1)
        attributes?.recycle()
        mDrawable = resources.getDrawable(resourceID!!)
        //必须设置bounds才能draw出图像
        mDrawable?.setBounds(0, 0, mDrawable?.intrinsicWidth!!, mDrawable?.intrinsicHeight!!)
        holder.addCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mDrawable?.intrinsicWidth!!, mDrawable?.intrinsicHeight!!)
    }

}