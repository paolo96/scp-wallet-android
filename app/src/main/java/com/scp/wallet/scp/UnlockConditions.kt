package com.scp.wallet.scp

import com.google.gson.annotations.SerializedName
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.crypto.Crypto.Companion.HASH_SIZE
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.crypto.MerkleTree
import com.scp.wallet.exceptions.InvalidUnlockHashException
import com.scp.wallet.utils.Bytes
import com.scp.wallet.utils.Hex

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