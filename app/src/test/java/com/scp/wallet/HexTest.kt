package com.scp.wallet

import com.scp.wallet.utils.Hex
import org.junit.Test

class HexTest {

    @Test
    fun bytesToStringTest() {

        val blankByteArray = ByteArray(3){0}
        assert(Hex.bytesToHexToString(blankByteArray) == "000000")

        val testByteArray = byteArrayOf(0x0e, 0x1a, 0x78)
        assert(Hex.bytesToHexToString(testByteArray) == "0e1a78")

    }

    @Test
    fun stringToBytesTest() {

        val blankByteArray = ByteArray(3){0}
        assert(Hex.stringToBytes("000000").contentEquals(blankByteArray))

        val testByteArray = byteArrayOf(0x0e, 0x1a, 0x78)
        assert(Hex.stringToBytes("0e1a78").contentEquals(testByteArray))

    }

    @Test
    fun consistencyTest() {

        val testByteArray = byteArrayOf(0x0e, 0x1a, 0x78)
        assert((Hex.stringToBytes(Hex.bytesToHexToString(testByteArray))).contentEquals(testByteArray))

        val testString = "0e1a78"
        assert(Hex.bytesToHexToString(Hex.stringToBytes(testString)) == testString)

    }

}