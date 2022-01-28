package com.scp.wallet.scp

import com.google.gson.annotations.SerializedName

class ScpInput(@SerializedName("parentid") val parentId: ByteArray, @SerializedName("unlockconditions") val unlockConditions: UnlockConditions) {

    fun toByteArray() : ByteArray {
        return parentId + unlockConditions.toByteArray()
    }

}
