package com.scp.wallet.mnemonics

import java.math.BigInteger
import java.text.Normalizer

//Approximated translation of gitlab.com/NebulousLabs/entropy-mnemonics
interface Dictionary {

    companion object {
        const val DICTIONARY_SIZE: Long = 1626

        fun getDictionaryFromId(did: String?) : Dictionary {
            return when (did) {
                English.DICTIONARY_ID -> English()
                German.DICTIONARY_ID -> German()
                Japanese.DICTIONARY_ID -> Japanese()
                else -> English()
            }
        }
    }

    val data: Array<String>
    val prefixLen: Int

    // Converts an input ByteArray to a human-friendly phrase. The conversion is reversible.
    fun toPhrase(entropy: ByteArray) : Array<String>? {
        if (entropy.isEmpty()) {
            return null
        }
        val intEntropy = bytesToInt(entropy)
        return intToPhrase(intEntropy).toTypedArray()
    }

    // FromPhrase converts an input phrase back to the original ByteArray.
    fun fromPhrase(p: Array<String>) : ByteArray? {
        if (p.isEmpty()) {
            return null
        }

        val intEntropy = phraseToInt(p) ?: return null
        return intToBytes(intEntropy)
    }

    // Converts a ByteArray to a BigInteger in a way that preserves
    // leading 0s, and ensures there is a perfect 1:1 mapping between Ints and ByteArrays.
    private fun bytesToInt(bs: ByteArray) : BigInteger {

        val base = BigInteger.valueOf(256)
        var exp = BigInteger.ONE
        var result = -BigInteger.ONE
        for (b in bs) {
            // Byte is converted to unsigned. This differs from the original implementation since
            // in go bytes are stored unsigned and in kotlin they are signed
            var tmp = BigInteger.valueOf(b.toUByte().toLong())
            tmp += BigInteger.ONE
            tmp *= exp
            exp *= base
            result += tmp
        }
        return result

    }

    // intToBytes converts a BigInteger to a ByteArray. It's the inverse of bytesToInt.
    private fun intToBytes(bi: BigInteger) : ByteArray {

        var biCopy = bi
        var base = BigInteger.valueOf(256)
        var result = byteArrayOf()
        while(biCopy >= base) {
            val i = (biCopy % base).toByte()
            result += byteArrayOf(i)
            biCopy -= base
            biCopy /= base
        }
        result += byteArrayOf(biCopy.toByte())
        return result

    }

    // phraseToInt coverts an ArrayList of Strings into a BigInteger that
    // uniquely represents the array.
    private fun phraseToInt(p: Array<String>) : BigInteger? {

        val base = BigInteger.valueOf(DICTIONARY_SIZE)
        var exp = BigInteger.ONE
        var result = -BigInteger.ONE
        for (w in p) {
            // Normalize the input.
            val word = Normalizer.normalize(w, Normalizer.Form.NFD)

            // Get the first prefixLen chars from the string.
            var prefix = ""
            var runeCount = 0
            for (r in word) {
                prefix += r

                runeCount++
                if (runeCount == prefixLen) {
                    break
                }
            }

            // Find the index associated with the phrase.
            var tmp = BigInteger.ZERO
            var found = false
            for (i in data.indices) {
                if(data[i].startsWith(prefix)) {
                    tmp +=  BigInteger.valueOf(i.toLong())
                    found = true
                    break
                }
            }
            if(!found) {
                return null
            }

            // Add the index to the big int.
            tmp += BigInteger.ONE
            tmp *= exp
            exp *= base
            result += tmp
        }

        return result

    }

    // Converts a BigInteger to an ArrayList of human readable Strings.
    // It's the inverse of phraseToInt
    private fun intToPhrase(bi: BigInteger) : ArrayList<String> {

        var biCopy = bi
        val base = BigInteger.valueOf(DICTIONARY_SIZE)
        val result = arrayListOf<String>()

        while (biCopy >= base) {
            val i = (biCopy % base).toInt()
            result.add(this.data[i])
            biCopy -= base
            biCopy /= base
        }
        result.add(this.data[biCopy.toInt()])

        return result

    }

}