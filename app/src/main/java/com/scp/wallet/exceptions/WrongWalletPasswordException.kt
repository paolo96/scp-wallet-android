package com.scp.wallet.exceptions

import java.lang.Exception

class WrongWalletPasswordException(message: String = "Wrong wallet password provided") : Exception(message)