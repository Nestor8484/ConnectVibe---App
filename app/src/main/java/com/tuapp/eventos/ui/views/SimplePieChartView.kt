package com.tuapp.eventos.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SimplePieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    private var data: List<Pair<Float, Int>> = emptyList()

    fun setData(newData: List<Pair<Float, Int>>) {
        this.data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (data.isEmpty()) return

        val size = Math.min(width, height).toFloat()
        val padding = 20f
        rectF.set(padding, padding, size - padding, size - padding)
        
        var startAngle = 0f
        for (item in data) {
            paint.color = item.second
            val sweepAngle = (item.first / 100f) * 360f
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }
}