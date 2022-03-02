package com.scp.wallet.utils

import com.scp.wallet.R
import kotlin.math.roundToInt

object Dates {

    fun timestampToReadable(t: Long) : String {

        val now = System.currentTimeMillis() / 1000

        return if(t == 0L) {
            Strings.get(R.string.unknown)
        } else if(now - t < 120) {
            Strings.get(R.string.moments_ago)
        } else if(now - t < 3600*2) {
            Strings.get(R.string.minutes_ago, ((now - t)/60.0).roundToInt().toString())
        } else if(now - t < 3600*48) {
            Strings.get(R.string.hours_ago, ((now - t)/3600.0).roundToInt().toString())
        } else if(now - t < 3600*24*30*6) {
            Strings.get(R.string.days_ago, ((now - t)/(24*3600.0)).roundToInt().toString())
        } else {
            Strings.get(R.string.long_time_ago)
        }

    }

}