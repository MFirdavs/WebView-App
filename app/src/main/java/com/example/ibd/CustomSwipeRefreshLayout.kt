// File: CustomSwipeRefreshLayout.kt
package com.example.ibd

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class CustomSwipeRefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var startY = 0f
    private val offsetFraction = 0.03f
    private val swipeAreaHeightFraction = 0.12f
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val height = height
                val offset = height * offsetFraction
                val swipeAreaHeight = height * swipeAreaHeightFraction
                if (startY < offset || startY > (offset + swipeAreaHeight)) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
