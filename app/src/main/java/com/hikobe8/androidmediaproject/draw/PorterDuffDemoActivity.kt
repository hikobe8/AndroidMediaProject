package com.hikobe8.androidmediaproject.draw

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import com.hikobe8.androidmediaproject.R
import kotlinx.android.synthetic.main.activity_porter_duff_demo.*
import java.io.File
import java.io.FileOutputStream

class PorterDuffDemoActivity : AppCompatActivity() {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, PorterDuffDemoActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_porter_duff_demo)
        btn_show.setOnClickListener {
            porter_view.extractImgInMirror()
            val bmp = Bitmap.createBitmap(porter_view.width, porter_view.height, Bitmap.Config.ARGB_4444)
            val canvas = Canvas(bmp)
            porter_view.draw(canvas)
            iv_processed.setImageBitmap(bmp)
            val os = FileOutputStream(File(Environment.getExternalStorageDirectory(), "0test.png"))
            bmp.compress(Bitmap.CompressFormat.PNG, 0, os)
        }
    }
}


class PorterDuffView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    constructor(context: Context?) : this(context, null, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private var mSrcBitmap: Bitmap? = null //源图
    private var mDstBitmap: Bitmap? = null //遮罩层
    private val mDrawRect = Rect()
    private var mPaint:Paint? = Paint().apply {
        isAntiAlias = true
    }

    private val mXfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)

    init {
        mSrcBitmap = BitmapFactory.decodeResource(resources, R.drawable.portrait)
        mDstBitmap = BitmapFactory.decodeResource(resources, R.drawable.test_capture_face_bg)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mDrawRect.set(0,0,w,h)
    }

    fun setBitmaps(srcBmp:Bitmap, dstBmp:Bitmap){
        mSrcBitmap = srcBmp
        mDstBitmap = dstBmp
    }

    fun extractImgInMirror(){
        visibility = INVISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mSrcBitmap?.let {
            canvas?.drawBitmap(it, null, mDrawRect, mPaint)
        }
        mPaint?.xfermode = mXfermode
        mDstBitmap?.let {
            canvas?.drawBitmap(it, null, mDrawRect, mPaint)
        }
        mPaint?.xfermode = null
    }

}