package com.scp.wallet.exceptions

import com.scp.wallet.R
import com.scp.wallet.utils.Strings

class WalletLockedException(message: String = Strings.get(R.string.exception_wallet_locked)) : Exception(message)