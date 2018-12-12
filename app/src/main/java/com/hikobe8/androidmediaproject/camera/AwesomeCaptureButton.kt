package com.hikobe8.androidmediaproject.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-12 14:27
 *  description :
 */

class AwesomeCaptureButton(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_SIZE = 200
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null)

    private var mPaint: Paint
    private lateinit var mPath: Path

    init {
        setBackgroundColor(Color.TRANSPARENT)
        mPaint = kotlin.run {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = Color.RED
            paint
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(DEFAULT_SIZE, widthMeasureSpec)
        val height = resolveSize(DEFAULT_SIZE, heightMeasureSpec)
        Math.min(width, height).let {
            setMeasuredDimension(it, it)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mPath = Path()
        mPath.addCircle(w/2f, h/2f, w/2f, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawCircle(width/2f, height/2f, width/2f, mPaint)
    }

}