package com.scp.wallet.scp

import com.google.gson.annotations.SerializedName
import com.scp.wallet.utils.Bytes

class UnlockConditions(@SerializedName("publickeys") val publicKeys: Array<ScpKey>, @SerializedName("signaturesrequired") val signatureRequired: Int = 1, val timelock: Int = SpendableKey.GLOBAL_TIME_LOCK) {

    fun toByteArray() : ByteArray {
        var buf = byteArrayOf()
        buf += Bytes.intToInt64ByteArray(timelock)
        buf += Bytes.intToInt64ByteArray(publicKeys.size)
        for(spk in publicKeys) {
            buf += spk.toByteArray()
        }
        buf += Bytes.intToInt64ByteArray(signatureRequired)
        return buf
    }

}