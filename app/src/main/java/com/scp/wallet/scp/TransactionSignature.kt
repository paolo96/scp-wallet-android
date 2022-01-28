package com.scp.wallet.scp


class TransactionSignature(val parentId: ByteArray, val publicKeyIndex: Int, val timelock: Int = 0) {

    //Multi-sig not supported
    val wholeTransaction = true
    var signature: ByteArray? = null

}