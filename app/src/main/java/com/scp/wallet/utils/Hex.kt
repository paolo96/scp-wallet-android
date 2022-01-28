package com.scp.wallet.utils

object Hex {

    private val HEX_CHAR_TABLE = byteArrayOf(
        '0'.code.toByte(),
        '1'.code.toByte(),
        '2'.code.toByte(),
        '3'.code.toByte(),
        '4'.code.toByte(),
        '5'.code.toByte(),
        '6'.code.toByte(),
        '7'.code.toByte(),
        '8'.code.toByte(),
        '9'.code.toByte(),
        'a'.code.toByte(),
        'b'.code.toByte(),
        'c'.code.toByte(),
        'd'.code.toByte(),
        'e'.code.toByte(),
        'f'.code.toByte())

    fun stringToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun bytesToHexToString(bytes: ByteArray): String {
        val hexBytes = binaryToHex(bytes)
        var res = ""
        hexBytes.forEach {
            res += it.toInt().toChar()
        }
        return res
    }

    private fun binaryToHex(bytes: ByteArray): ByteArray {
        val hex = ByteArray(2 * bytes.size)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hex[i * 2] = HEX_CHAR_TABLE[v.ushr(4)]
            hex[i * 2 + 1] = HEX_CHAR_TABLE[v and 0xF]
        }
        return hex
    }

}