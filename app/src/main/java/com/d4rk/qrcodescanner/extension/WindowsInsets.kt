package com.d4rk.qrcodescanner.extension

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.WindowInsets

fun View.applySystemWindowInsets(
    applyLeft: Boolean = false,
    applyTop: Boolean = false,
    applyRight: Boolean = false,
    applyBottom: Boolean = false
) {
    doOnApplyWindowInsets { view, insets, padding ->
        val left = if (applyLeft) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            insets.systemWindowInsetLeft
        } else {
            TODO("VERSION.SDK_INT < KITKAT_WATCH")
        } else 0
        val top = if (applyTop) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            insets.systemWindowInsetTop
        } else {
            TODO("VERSION.SDK_INT < KITKAT_WATCH")
        } else 0
        val right = if (applyRight) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            insets.systemWindowInsetRight
        } else {
            TODO("VERSION.SDK_INT < KITKAT_WATCH")
        } else 0
        val bottom = if (applyBottom) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            insets.systemWindowInsetBottom
        } else {
            TODO("VERSION.SDK_INT < KITKAT_WATCH")
        } else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

@SuppressLint("RestrictedApi")
fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
    // Create a snapshot of the view's padding state
    val initialPadding = recordInitialPaddingForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding state
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        setOnApplyWindowInsetsListener { v, insets ->
            f(v, insets, initialPadding)
            // Always return the insets, so that children can also use them
            insets
        }
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

data class InitialPadding(val left: Int, val top: Int,
                          val right: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            requestApplyInsets()
        }
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    v.requestApplyInsets()
                }
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}