package com.scp.wallet.exceptions

import java.lang.Exception

class ApiException(message: String = "API request failed") : Exception(message)