package com.scp.wallet.scp

import com.scp.wallet.utils.Bytes
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

class CurrencyValue(var value: BigInteger) {

    companion object {

        val COIN_PRECISION = BigInteger("1${"0".repeat(27)}")
        val COIN_PRECISION_CENTS = BigInteger("1${"0".repeat(25)}")

        fun initFromString(value: String) : CurrencyValue {
            val bigInt = BigInteger(value)
            return CurrencyValue(bigInt)
        }

        fun initFromDouble(value: Double) : CurrencyValue {
            val bigInt = (BigDecimal.valueOf(value) * COIN_PRECISION.toBigDecimal()).toBigInteger()
            return CurrencyValue(bigInt)
        }

    }

    fun size() : Int {
        return value.bitLength() / 8
    }

    fun toScpReadable() : String {
        val sign = if(value > BigInteger.ZERO) "" else "-"
        val absValue = value.abs()
        return if(absValue >= COIN_PRECISION*BigInteger("1000")) {
            val scpValue: Double = ((absValue * BigInteger.valueOf(1000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${scpValue.roundToLong()} SCP"
        } else if(absValue >= COIN_PRECISION*BigInteger("100")) {
            val scpValue: Double = ((absValue * BigInteger.valueOf(1000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.1f".format(scpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} SCP"
        } else if(absValue >= COIN_PRECISION*BigInteger("10")) {
            val scpValue: Double = ((absValue * BigInteger.valueOf(1000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.2f".format(scpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} SCP"
        } else if(absValue >= COIN_PRECISION) {
            val scpValue: Double = ((absValue * BigInteger.valueOf(1000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.3f".format(scpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} SCP"
        } else if(absValue == BigInteger.ZERO) {
            "0 SCP"
        } else if(absValue >= COIN_PRECISION/BigInteger("1000")) {
            val milliScpValue = ((absValue * BigInteger.valueOf(1000000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.3f".format(milliScpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} mSCP"
        } else if(absValue >= COIN_PRECISION/BigInteger("1000000")) {
            val microScpValue = ((absValue * BigInteger.valueOf(1000000000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.3f".format(microScpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} uSCP"
        } else if(absValue >= COIN_PRECISION/BigInteger("1000000000")) {
            val nanoScpValue = ((absValue * BigInteger.valueOf(1000000000000) / COIN_PRECISION).toDouble() / 1000)
            "$sign${"%.3f".format(nanoScpValue).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")} nSCP"
        } else {
            "~0 SCP"
        }
    }

    //Transforms value to an array of bytes where the first 8 bytes represents
    //the number of bytes used.
    fun toByteArray() : ByteArray {
        val bitsNum = value.bitLength()
        val maxBitsWrap = if(bitsNum%8!=0) 1 else 0
        val maxBitsWrapInverse = if(maxBitsWrap == 1) 0 else 1
        val significantBytes = (bitsNum/8)+maxBitsWrap
        val valueBytes = value.toByteArray()

        var valueBytesResult = byteArrayOf()
        for(i in maxBitsWrapInverse until valueBytes.size) {
            valueBytesResult += byteArrayOf(valueBytes[i])
        }
        return Bytes.intToInt64ByteArray(significantBytes) + valueBytesResult
    }

    operator fun plus(b: CurrencyValue): CurrencyValue {
        return CurrencyValue(value+b.value)
    }

    operator fun minus(b: CurrencyValue): CurrencyValue {
        return CurrencyValue(value-b.value)
    }

    operator fun times(b: CurrencyValue): CurrencyValue {
        return CurrencyValue(value*b.value)
    }

    operator fun div(b: CurrencyValue): CurrencyValue {
        return CurrencyValue(value/b.value)
    }

}