package com.scp.wallet.utils

import androidx.annotation.StringRes
import com.scp.wallet.ScpWalletApp

object Strings {
    fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
        return ScpWalletApp.instance.getString(stringRes, *formatArgs)
    }
}