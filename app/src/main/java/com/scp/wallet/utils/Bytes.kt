package com.scp.wallet.utils

object Bytes {

    private const val INT64_BYTE_SIZE = 8

    fun intToInt64ByteArray(input: Int) : ByteArray {
        val data = input.toUInt()
        val buffer = ByteArray(4)
        for (i in 0..3) buffer[i] = (data shr (i*8)).toByte()
        return addTrailingZerosToByteArray(buffer, INT64_BYTE_SIZE)
    }

    fun addTrailingZerosToByteArray(byteArray: ByteArray, maxSize: Int) : ByteArray {
        return ByteArray(maxSize) {
            if(it < byteArray.size) {
                byteArray[it]
            } else {
                0
            }
        }
    }

}