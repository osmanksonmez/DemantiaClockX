package com.example.demantiaclockx

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.*

class CustomAnalogClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val calendar = Calendar.getInstance()
    
    // Renkler
    private val clockFaceColor = Color.parseColor("#1A1A1A")
    private val hourMarkColor = Color.WHITE
    private val minuteMarkColor = Color.parseColor("#CCCCCC")
    private val hourHandColor = Color.WHITE
    private val minuteHandColor = Color.WHITE
    private val secondHandColor = Color.parseColor("#FF4444")
    private val centerDotColor = Color.WHITE
    
    // Boyutlar (dp cinsinden, sonra px'e çevrilecek)
    private var clockRadius = 0f
    private var centerX = 0f
    private var centerY = 0f
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        clockRadius = min(w, h) / 2f * 0.95f // %95'ini kullan, daha az kenar boşluğu
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Mevcut zamanı al
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        // Saat kadranını çiz
        drawClockFace(canvas)
        
        // Saat işaretlerini çiz
        drawHourMarks(canvas)
        drawMinuteMarks(canvas)
        
        // Sayıları çiz
        drawNumbers(canvas)
        
        // Akrep, yelkovan ve saniye ibresini çiz
        drawHands(canvas, hour, minute, second)
        
        // Merkez noktayı çiz
        drawCenter(canvas)
        
        // Her saniye yeniden çiz
        postInvalidateDelayed(1000)
    }
    
    private fun drawClockFace(canvas: Canvas) {
        paint.apply {
            color = clockFaceColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, clockRadius, paint)
        
        // Dış çerçeve
        paint.apply {
            color = hourMarkColor
            style = Paint.Style.STROKE
            strokeWidth = clockRadius * 0.01f
        }
        canvas.drawCircle(centerX, centerY, clockRadius, paint)
    }
    
    private fun drawHourMarks(canvas: Canvas) {
        paint.apply {
            color = hourMarkColor
            style = Paint.Style.STROKE
            strokeWidth = clockRadius * 0.02f // Boyuta göre ayarla
        }
        
        for (i in 0 until 12) {
            val angle = i * 30f - 90f // -90 derece çünkü 12 saat üstte olmalı
            val startRadius = clockRadius * 0.85f
            val endRadius = clockRadius * 0.95f
            
            val startX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * startRadius
            val startY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * startRadius
            val endX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * endRadius
            val endY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * endRadius
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
    
    private fun drawMinuteMarks(canvas: Canvas) {
        paint.apply {
            color = minuteMarkColor
            style = Paint.Style.STROKE
            strokeWidth = clockRadius * 0.008f // Boyuta göre ayarla
        }
        
        for (i in 0 until 60) {
            if (i % 5 != 0) { // Saat işaretleri hariç
                val angle = i * 6f - 90f // Her dakika 6 derece
                val startRadius = clockRadius * 0.90f
                val endRadius = clockRadius * 0.95f
                
                val startX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * startRadius
                val startY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * startRadius
                val endX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * endRadius
                val endY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * endRadius
                
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
    
    private fun drawNumbers(canvas: Canvas) {
        paint.apply {
            color = hourMarkColor
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = clockRadius * 0.15f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        for (i in 1..12) {
            val angle = i * 30f - 90f
            val textRadius = clockRadius * 0.75f
            
            val x = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * textRadius
            val y = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * textRadius + paint.textSize / 3
            
            canvas.drawText(i.toString(), x, y, paint)
        }
    }
    
    private fun drawHands(canvas: Canvas, hour: Int, minute: Int, second: Int) {
        // Akrep (saat ibresi)
        val hourAngle = (hour % 12) * 30f + minute * 0.5f - 90f
        drawHand(canvas, hourAngle, clockRadius * 0.5f, hourHandColor, clockRadius * 0.03f)
        
        // Yelkovan (dakika ibresi)
        val minuteAngle = minute * 6f - 90f
        drawHand(canvas, minuteAngle, clockRadius * 0.7f, minuteHandColor, clockRadius * 0.02f)
        
        // Saniye ibresi
        val secondAngle = second * 6f - 90f
        drawHand(canvas, secondAngle, clockRadius * 0.8f, secondHandColor, clockRadius * 0.01f)
    }
    
    private fun drawHand(canvas: Canvas, angle: Float, length: Float, color: Int, strokeWidth: Float) {
        paint.apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        
        val endX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * length
        val endY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * length
        
        canvas.drawLine(centerX, centerY, endX, endY, paint)
    }
    
    private fun drawCenter(canvas: Canvas) {
        paint.apply {
            color = centerDotColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, clockRadius * 0.03f, paint)
    }
}