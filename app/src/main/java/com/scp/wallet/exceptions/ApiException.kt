package com.scp.wallet.exceptions

import com.scp.wallet.R
import com.scp.wallet.utils.Strings
import java.lang.Exception

class ApiException(message: String = Strings.get(R.string.exception_api)) : Exception(message)