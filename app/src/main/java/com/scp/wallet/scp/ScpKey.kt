package com.scp.wallet.scp

import com.scp.wallet.utils.Bytes
import java.lang.Exception
import java.nio.charset.Charset

class ScpKey(val key: ByteArray, val specifier: ByteArray = newSpecifier(ALGORITHM)) {

    companion object {

        const val SPECIFIER_LEN = 16
        const val ALGORITHM = "ed25519"

        //Returns a unique ByteArray representation of the given ascii string
        fun newSpecifier(name: String) : ByteArray {
            val asciiCharset = Charset.forName("US-ASCII")
            if(!asciiCharset.newEncoder().canEncode(name)) {
                throw Exception("Specifier contains non ascii characters")
            }
            if(name.length > SPECIFIER_LEN) {
                throw Exception("Specifier too long")
            }
            return name.toByteArray(asciiCharset)
        }

    }

    fun toByteArray() : ByteArray {

        var keyByteArray = Bytes.addTrailingZerosToByteArray(specifier, SPECIFIER_LEN)
        keyByteArray += Bytes.intToInt64ByteArray(key.size)
        keyByteArray += key
        return keyByteArray

    }

}