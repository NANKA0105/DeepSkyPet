package com.deepsky.pet.gesture

import android.view.MotionEvent
import android.view.WebView
import android.view.WindowManager

class GestureHandler(
    private val onTap: (Pair<Int, Int>) -> Unit,
    private val onDoubleTap: (Pair<Int, Int>) -> Unit,
    private val onLongPress: (Pair<Int, Int>) -> Unit,
    private val onFling: (Int, Int) -> Unit
) {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTapTime = 0L
    private var touchStartTime = 0L
    private var hasMoved = false
    private var consecutiveTaps = 0
    private var lastConsecutiveTapTime = 0L

    companion object {
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val LONG_PRESS_TIMEOUT = 600L
        private const val MOVE_THRESHOLD = 10
        private const val CONSECUTIVE_TAP_WINDOW = 2000L
    }

    fun handleTouch(
        event: MotionEvent,
        params: WindowManager.LayoutParams,
        wm: WindowManager,
        webView: WebView,
        evaluateJs: (String) -> Unit
    ): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchStartTime = System.currentTimeMillis()
                hasMoved = false
                evaluateJs("window.petEngine && window.petEngine.onTouchDown()")
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (Math.abs(dx) > MOVE_THRESHOLD || Math.abs(dy) > MOVE_THRESHOLD) {
                    hasMoved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    wm.updateViewLayout(webView, params)
                    evaluateJs("window.petEngine && window.petEngine.onDrag()")
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                val elapsed = System.currentTimeMillis() - touchStartTime
                if (!hasMoved) {
                    val pos = Pair(params.x, params.y)
                    when {
                        elapsed > LONG_PRESS_TIMEOUT -> {
                            onLongPress(pos)
                            evaluateJs("window.petEngine && window.petEngine.onLongPress()")
                        }
                        System.currentTimeMillis() - lastTapTime < DOUBLE_TAP_TIMEOUT -> {
                            onDoubleTap(pos)
                            evaluateJs("window.petEngine && window.petEngine.onDoubleTap()")
                        }
                        else -> {
                            lastTapTime = System.currentTimeMillis()
                            onTap(pos)

                            // Consecutive tap counter
                            val now = System.currentTimeMillis()
                            if (now - lastConsecutiveTapTime < CONSECUTIVE_TAP_WINDOW) {
                                consecutiveTaps++
                            } else {
                                consecutiveTaps = 1
                            }
                            lastConsecutiveTapTime = now
                            evaluateJs("window.petEngine && window.petEngine.onTap()")
                            if (consecutiveTaps == 3) {
                                evaluateJs("window.petEngine && window.petEngine.onTripleTap()")
                            } else if (consecutiveTaps == 5) {
                                evaluateJs("window.petEngine && window.petEngine.onPentaTap()")
                            } else if (consecutiveTaps == 8) {
                                evaluateJs("window.petEngine && window.petEngine.onOctoTap()")
                            }
                        }
                    }
                } else {
                    evaluateJs("window.petEngine && window.petEngine.onDragEnd()")
                }
                evaluateJs("window.petEngine && window.petEngine.onTouchUp()")
                true
            }
            else -> false
        }
    }
}
