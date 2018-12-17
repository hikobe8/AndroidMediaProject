package com.hikobe8.androidmediaproject.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-17 10:56
 *  description : 人脸检测辅助显示框
 */

class FaceDetectionView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    constructor(context: Context?):this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?):this(context, attrs, 0)

    private val mPaint:Paint = Paint()
    private var mRectToDraw = RectF()

    init {
        mPaint.color = Color.RED
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = 10f
        mPaint.isAntiAlias = true
    }

    fun update(rectF: RectF?) {
        rectF?.let{
            mRectToDraw = rectF
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mRectToDraw.let {
           if (!it.isEmpty)
            canvas?.drawRect(mRectToDraw, mPaint)
        }
    }

}