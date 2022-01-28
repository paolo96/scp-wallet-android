package com.scp.wallet.scp



class SpendableKey(var secretKeys: Array<ByteArray>?, val unlockConditions: UnlockConditions) {

    companion object {

        const val GLOBAL_TIME_LOCK = 0

    }

}
