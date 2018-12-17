package com.hikobe8.androidmediaproject.camera.camera2

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-17 17:11
 *  description :
 */
class AutoFitTextureView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    TextureView(context, attrs, defStyleAttr) {

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private var mRatioWidth = 0
    private var mRatioHeight = 0

    fun setAspectRatio(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            mRatioWidth = width
            mRatioHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * 1f * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, (height * 1f * mRatioWidth / mRatioHeight).toInt())
            } else {
                setMeasuredDimension((width * 1f * mRatioHeight / mRatioWidth).toInt(), height)
            }
        }

    }

}