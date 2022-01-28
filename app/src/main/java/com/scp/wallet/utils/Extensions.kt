package com.scp.wallet.utils

import android.content.res.Resources.getSystem
import android.util.TypedValue
import java.util.*

val Number.dp: Int get() = (this.toInt() / getSystem().displayMetrics.density).toInt()
val Number.px: Int get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), getSystem().displayMetrics).toInt()
val String.capitalized: String get() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
