package com.scp.wallet

import com.scp.wallet.scp.*
import com.scp.wallet.utils.Hex
import org.junit.Test

import java.math.BigInteger

class TransactionTest {

    @Test
    fun sigHashTest() {

        UtilsTest.useLazySodiumJava()

        val blankByteArray = ByteArray(32){0}

        val transactionTest1 = Transaction(
            arrayOf(ScpInput(blankByteArray, UnlockConditions(arrayOf(), 0))),
            arrayOf(ScpOutput(CurrencyValue(BigInteger.valueOf(0)),blankByteArray)),
            arrayOf(CurrencyValue(BigInteger.valueOf(0))),
            arrayOf(byteArrayOf('o'.code.toByte()), byteArrayOf('t'.code.toByte())))
        transactionTest1.signatures = arrayOf(TransactionSignature(blankByteArray, 0, 0))

        assert(Hex.bytesToHexToString(transactionTest1.sigHash(0,0)) == "7c174645d5281c93e3b7dfb56a0492fa486be66170987c69e53222060f286465")

        val transactionTest2 = Transaction(
            arrayOf(ScpInput(blankByteArray, UnlockConditions(arrayOf(), 1))),
            arrayOf(ScpOutput(CurrencyValue(BigInteger.valueOf(58734)),blankByteArray)),
            arrayOf(CurrencyValue(BigInteger.valueOf(4234))),
            arrayOf(byteArrayOf('o'.code.toByte()), byteArrayOf('t'.code.toByte())))
        transactionTest2.signatures = arrayOf(TransactionSignature(blankByteArray, 0, 0))

        assert(Hex.bytesToHexToString(transactionTest2.sigHash(0,0)) == "c4dbf87daba1d1f29dbd145bca69004323e37899bee3112b204f91669c952f13")

    }

}