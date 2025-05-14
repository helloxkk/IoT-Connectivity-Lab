package cn.hellokk.ble.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import cn.hellokk.ble.R
import java.util.*

/**
 * 作者: Kun on 2025/5/10.
 * 邮箱: vip@hellokk.cc.
 * 描述: 自定义水波纹动画视图
 */
class RippleAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 水波纹圆圈数量
    private val rippleCount = 4

    // 保存每个水波纹的当前半径
    private val rippleRadii = FloatArray(rippleCount)

    // 保存每个水波纹的当前透明度
    private val rippleAlphas = IntArray(rippleCount)

    // 最大半径比例 (相对于视图宽度的一半)
    private val maxRadiusRatio = 0.85f

    // 最小半径
    private val minRadius = 0f

    // 水波纹动画的时长
    private val animationDuration = 3000L

    // 每个水波纹的延迟时间
    private val rippleDelay = animationDuration / rippleCount

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 水波纹颜色
    private val rippleColor = ContextCompat.getColor(context, R.color.ripple_color)

    // 水波纹起始透明度
    private val startAlpha = 180

    // 动画是否运行中
    private var isAnimating = false

    // 动画控制器
    private val animators = mutableListOf<ValueAnimator>()

    // 随机生成器，用于创建错开效果
    private val random = Random()

    init {
        // 设置画笔
        paint.style = Paint.Style.FILL
        paint.color = rippleColor

        // 初始化水波纹参数
        for (i in 0 until rippleCount) {
            rippleRadii[i] = 0f
            rippleAlphas[i] = 0
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 画布中心点
        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制所有水波纹
        for (i in 0 until rippleCount) {
            if (rippleAlphas[i] > 0) {
                paint.alpha = rippleAlphas[i]
                canvas.drawCircle(centerX, centerY, rippleRadii[i], paint)
            }
        }

        // 如果正在动画，继续请求绘制
        if (isAnimating) {
            invalidate()
        }
    }

    fun startRippleAnimation() {
        if (isAnimating) return

        isAnimating = true

        // 计算最大半径
        val maxRadius = width.coerceAtMost(height) / 2f * maxRadiusRatio

        // 停止所有现有动画
        stopAllAnimators()

        // 为每个水波纹创建动画
        for (i in 0 until rippleCount) {
            // 创建半径动画
            val animator = ValueAnimator.ofFloat(minRadius, maxRadius)
            animator.duration = animationDuration
            animator.startDelay = i * rippleDelay
            animator.interpolator = DecelerateInterpolator()
            animator.repeatCount = ValueAnimator.INFINITE

            animator.addUpdateListener { animation ->
                val radius = animation.animatedValue as Float
                rippleRadii[i] = radius

                // 根据半径计算透明度，从startAlpha到0
                rippleAlphas[i] = (startAlpha * (1 - radius / maxRadius)).toInt()

                // 请求重绘
                invalidate()
            }

            // 将动画添加到列表
            animators.add(animator)

            // 启动动画
            animator.start()
        }
    }

    fun stopRippleAnimation() {
        if (!isAnimating) return

        stopAllAnimators()
        isAnimating = false

        // 重置水波纹参数
        for (i in 0 until rippleCount) {
            rippleRadii[i] = 0f
            rippleAlphas[i] = 0
        }

        invalidate()
    }

    private fun stopAllAnimators() {
        for (animator in animators) {
            animator.cancel()
        }
        animators.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllAnimators()
    }
}