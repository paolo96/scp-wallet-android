package com.scp.wallet.exceptions

import com.scp.wallet.R
import com.scp.wallet.utils.Strings
import java.lang.Exception

class WalletLockedException(message: String = Strings.get(R.string.exception_wallet_locked)) : Exception(message)