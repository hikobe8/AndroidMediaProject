package com.hikobe8.androidmediaproject.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.hikobe8.androidmediaproject.R

/***
 *  Author : ryu18356@gmail.com
 *  Create at 2018-12-07 16:23
 *  description : 自定义View 使用canvas 绘制图片
 */
class ImageDrawerView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, null, 0)

    private var mDrawable:Drawable? = null

    init {
        val attributes = context?.obtainStyledAttributes(attrs, R.styleable.AppCompatImageView)
        val resourceID = attributes?.getResourceId(R.styleable.AppCompatImageView_android_src, -1)
        attributes?.recycle()
        mDrawable = resources.getDrawable(resourceID!!)
        //必须设置bounds才能draw出图像
        mDrawable?.setBounds(0, 0, mDrawable?.intrinsicWidth!!, mDrawable?.intrinsicHeight!!)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mDrawable?.intrinsicWidth!!, mDrawable?.intrinsicHeight!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mDrawable?.draw(canvas!!)
    }

}