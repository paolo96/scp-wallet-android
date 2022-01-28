package com.scp.wallet.scp

import com.google.gson.annotations.SerializedName

class ScpOutput(val value: CurrencyValue, @SerializedName("unlockhash") var unlockHash: ByteArray) {

    companion object {
        const val ID_SPECIFIER = "siacoin output"
    }

    var id: ByteArray? = null

    fun toByteArray() : ByteArray {
        return value.toByteArray() + unlockHash
    }

}