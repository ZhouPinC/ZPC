package com.example.test.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.test.R
import java.util.LinkedList
import java.util.concurrent.LinkedBlockingQueue

class VoiceWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- 配置参数 ---
    private var barWidthPx = 10f
    private var barGapPx = 15f
    private var minHeightPx = 30f
    private var maxHeightPx = 150f
    private var cornerRadiusPx = 5f
    private var waveColor = Color.WHITE
    
    // 滚动速度 (像素/毫秒)
    // 默认速度：每秒走过约 150dp 的距离，可根据 waveSpeed 调整
    private var speedPxPerMs = 0.15f

    // --- 核心：无限滚动逻辑 ---
    // 一个完整波形的宽度 (条宽 + 间隙)
    private val itemWidth: Float
        get() = barWidthPx + barGapPx

    // 当前向左滚动的累积偏移量 (0 -> itemWidth)
    private var currentScrollOffset = 0f

    // 上一帧的时间戳
    private var lastFrameTime = 0L

    // 数据缓冲区
    private val dataQueue = LinkedBlockingQueue<Float>()
    // 屏幕上显示的数据
    private val displayList = LinkedList<Float>()
    private var maxBarCount = 0

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val rectF = RectF()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VoiceWaveView)
        waveColor = typedArray.getColor(R.styleable.VoiceWaveView_waveColor, Color.WHITE)
        barWidthPx = typedArray.getDimension(R.styleable.VoiceWaveView_waveBarWidth, dp2px(4f))
        barGapPx = typedArray.getDimension(R.styleable.VoiceWaveView_waveGap, dp2px(6f))
        minHeightPx = typedArray.getDimension(R.styleable.VoiceWaveView_waveMinHeight, dp2px(12f))
        maxHeightPx = typedArray.getDimension(R.styleable.VoiceWaveView_waveMaxHeight, dp2px(58f))
        cornerRadiusPx = typedArray.getDimension(R.styleable.VoiceWaveView_waveCornerRadius, dp2px(2f))
        
        // 使用默认速度：每 100ms 走完一个 itemWidth
        val speedFactor = 100
        speedPxPerMs = (barWidthPx + barGapPx) / speedFactor.toFloat()

        typedArray.recycle()
        paint.color = waveColor

        // 预填充数据，铺满屏幕，防止刚开始空白
        for (i in 0..50) displayList.add(0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (itemWidth > 0) {
            maxBarCount = (w / itemWidth).toInt() + 5
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // --- 1. 计算时间差 (Delta Time) ---
        val now = System.nanoTime()
        // 如果是第一次绘制，dt 设为 0
        val dt = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1_000_000f // 纳秒转毫秒
        lastFrameTime = now

        // --- 2. 物理位移 ---
        // 这一帧应该移动多少像素 = 速度 * 时间
        val dx = speedPxPerMs * dt
        currentScrollOffset += dx

        // --- 3. 数据消费逻辑 ---
        // 当累积偏移量超过一个波形条的宽度时，就消耗一个数据，并重置偏移量
        // 使用 while 处理极端情况（比如卡顿很久后，一次性跳过多个条目）
        while (currentScrollOffset >= itemWidth) {
            currentScrollOffset -= itemWidth
            consumeData()
        }

        // --- 4. 绘制逻辑 ---
        val centerY = height / 2f
        
        // 绘制起始点：最右侧 - 当前的平滑偏移量
        var drawX = width.toFloat() - currentScrollOffset

        // 倒序绘制
        val iterator = displayList.descendingIterator()
        while (iterator.hasNext()) {
            val amplitude = iterator.next()
            
            // 简单的线性高度
            val barHeight = minHeightPx + (amplitude * (maxHeightPx - minHeightPx))
            val finalHeight = barHeight.coerceAtMost(maxHeightPx)
            
            val top = centerY - (finalHeight / 2f)
            val bottom = centerY + (finalHeight / 2f)
            val left = drawX - barWidthPx
            val right = drawX

            rectF.set(left, top, right, bottom)
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)

            drawX -= itemWidth
            
            // 性能优化：画出屏幕左边就停止
            if (drawX < -barWidthPx) break
        }

        // --- 5. 请求下一帧 ---
        // 类似于游戏引擎的 Game Loop，只要 View 可见就持续刷新
        if (isAttachedToWindow) {
            invalidate()
        }
    }

    private fun consumeData() {
        // 优先取新数据，没有则取 0
        val nextAmplitude = dataQueue.poll() ?: 0f
        displayList.add(nextAmplitude)
        
        if (displayList.size > maxBarCount + 5) {
            displayList.removeFirst()
        }
    }

    fun addAmplitude(ratio: Float) {
        dataQueue.offer(ratio.coerceIn(0f, 1f))
    }
    
    // 视图不可见时重置时间戳，防止切回来瞬间发生巨大的位移跳跃
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            lastFrameTime = 0L
            invalidate()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 可以在这里做一些清理
    }

    private fun dp2px(dp: Float): Float = (dp * resources.displayMetrics.density)
}