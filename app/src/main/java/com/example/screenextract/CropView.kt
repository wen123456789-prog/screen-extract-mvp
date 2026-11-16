package com.example.screenextract

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

class CropView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private companion object { const val MODE_NONE = 0; const val MODE_MOVE = 1; const val MODE_RESIZE = 2 }

    private var bitmap: Bitmap? = null
    private val displayRect = RectF()
    private val cropRect = RectF()

    private val maskPaint = Paint().apply { color = 0xA0000000.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private val handleRadius = 16f
    private var mode = MODE_NONE
    private var activeCorner = 0
    private var lastX = 0f
    private var lastY = 0f

    fun setBitmap(b: Bitmap) {
        bitmap = b
        if (width > 0 && height > 0) layoutBitmap()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh); layoutBitmap()
    }

    private fun layoutBitmap() {
        val b = bitmap ?: return
        if (width == 0 || height == 0) return
        val scale = min(width.toFloat() / b.width, height.toFloat() / b.height)
        val dw = b.width * scale; val dh = b.height * scale
        val left = (width - dw) / 2f; val top = (height - dh) / 2f
        displayRect.set(left, top, left + dw, top + dh)
        cropRect.set(displayRect.left + dw * 0.15f, displayRect.top + dh * 0.15f, displayRect.right - dw * 0.15f, displayRect.bottom - dh * 0.15f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = bitmap ?: return
        canvas.drawBitmap(b, null, displayRect, bitmapPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, maskPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), maskPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, maskPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, maskPaint)
        canvas.drawRect(cropRect, borderPaint)
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint)
    }

    private fun hitCorner(x: Float, y: Float): Int {
        val r = handleRadius * 2.5f
        if (dist(x, y, cropRect.left, cropRect.top) < r) return 1
        if (dist(x, y, cropRect.right, cropRect.top) < r) return 2
        if (dist(x, y, cropRect.left, cropRect.bottom) < r) return 3
        if (dist(x, y, cropRect.right, cropRect.bottom) < r) return 4
        return 0
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return sqrt(dx * dx + dy * dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeCorner = hitCorner(x, y)
                mode = when { activeCorner != 0 -> MODE_RESIZE; cropRect.contains(x, y) -> MODE_MOVE; else -> MODE_NONE }
                lastX = x; lastY = y; return mode != MODE_NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX; val dy = y - lastY
                when (mode) { MODE_MOVE -> moveCrop(dx, dy); MODE_RESIZE -> resizeCrop(activeCorner, x, y) }
                lastX = x; lastY = y; invalidate(); return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { mode = MODE_NONE; return true }
        }
        return super.onTouchEvent(event)
    }

    private fun moveCrop(dx: Float, dy: Float) {
        if (cropRect.left + dx < displayRect.left) return
        if (cropRect.right + dx > displayRect.right) return
        if (cropRect.top + dy < displayRect.top) return
        if (cropRect.bottom + dy > displayRect.bottom) return
        cropRect.offset(dx, dy)
    }

    private fun resizeCrop(corner: Int, x: Float, y: Float) {
        val minSize = 40f
        val cx = x.coerceIn(displayRect.left, displayRect.right)
        val cy = y.coerceIn(displayRect.top, displayRect.bottom)
        when (corner) {
            1 -> { if (cropRect.right - cx > minSize) cropRect.left = cx; if (cropRect.bottom - cy > minSize) cropRect.top = cy }
            2 -> { if (cx - cropRect.left > minSize) cropRect.right = cx; if (cropRect.bottom - cy > minSize) cropRect.top = cy }
            3 -> { if (cropRect.right - cx > minSize) cropRect.left = cx; if (cy - cropRect.top > minSize) cropRect.bottom = cy }
            4 -> { if (cx - cropRect.left > minSize) cropRect.right = cx; if (cy - cropRect.top > minSize) cropRect.bottom = cy }
        }
    }

    fun getCropBitmap(): Bitmap? {
        val b = bitmap ?: return null
        if (displayRect.width() <= 0 || cropRect.width() <= 0) return null
        val scale = b.width / displayRect.width()
        val left = ((cropRect.left - displayRect.left) * scale).toInt().coerceIn(0, b.width - 1)
        val top = ((cropRect.top - displayRect.top) * scale).toInt().coerceIn(0, b.height - 1)
        val right = ((cropRect.right - displayRect.left) * scale).toInt().coerceIn(left + 1, b.width)
        val bottom = ((cropRect.bottom - displayRect.top) * scale).toInt().coerceIn(top + 1, b.height)
        return Bitmap.createBitmap(b, left, top, right - left, bottom - top)
    }
}
