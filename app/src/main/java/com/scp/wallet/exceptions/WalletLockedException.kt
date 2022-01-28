package com.scp.wallet.exceptions

import java.lang.Exception

class WalletLockedException(message: String = "Wallet is locked") : Exception(message)