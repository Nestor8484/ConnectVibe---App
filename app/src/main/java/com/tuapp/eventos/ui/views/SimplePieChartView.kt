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
    
    // Datos de ejemplo: Categoría to (Porcentaje, Color)
    private val data = listOf(
        Pair(45f, 0xFF1565C0.toInt()), // Comida - Azul Oscuro
        Pair(30f, 0xFF1E88E5.toInt()), // Bebida - Azul Medio
        Pair(15f, 0xFF64B5F6.toInt()), // Logística - Azul Claro
        Pair(10f, 0xFFBBDEFB.toInt())  // Otros - Azul Muy Claro
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
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