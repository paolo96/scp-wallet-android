package com.scp.wallet.utils

import kotlin.math.roundToInt

object Dates {

    fun timestampToReadable(t: Long) : String {

        val now = System.currentTimeMillis() / 1000

        if(t == 0L) {
            return "Unknown"
        } else if(now - t < 120) {
            return "Moments ago"
        } else if(now - t < 3600*2) {
            return "${((now - t)/60.0).roundToInt()} minutes ago"
        } else if(now - t < 3600*48) {
            return "${((now - t)/3600.0).roundToInt()} hours ago"
        } else if(now - t < 3600*24*30*6) {
            return "${((now - t)/(24*3600.0)).roundToInt()} days ago"
        } else {
            return "Long time ago"
        }

    }

}