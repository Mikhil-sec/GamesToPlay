package com.mikhilnaika.continueapp.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Compose's `LocalContext` is sometimes a wrapper (e.g. inside themed contexts) — unwrap to
 * find the real Activity so RevenueCat/AdMob calls that need one don't crash. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
