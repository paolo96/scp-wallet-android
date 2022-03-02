package com.scp.wallet.exceptions

import com.scp.wallet.R
import com.scp.wallet.utils.Strings

class WrongWalletPasswordException(message: String = Strings.get(R.string.exception_wrong_password)) : Exception(message)