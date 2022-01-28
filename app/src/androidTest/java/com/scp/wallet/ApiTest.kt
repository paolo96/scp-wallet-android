package com.scp.wallet

import com.scp.wallet.api.API
import org.junit.Test
import java.lang.Exception
import java.math.BigInteger
import java.util.concurrent.CountDownLatch

class ApiTest {

    @Test
    fun networkDataTest() {

        val latch = CountDownLatch(1)

        API.getScprimeData({ consensusHeight, minFee, maxFee, _ ->
            assert(consensusHeight > 150000)
            assert(minFee > BigInteger.valueOf(100000))
            assert(maxFee > BigInteger.valueOf(100000))
            assert(maxFee >= minFee)
            latch.countDown()
        }, {
            throw Exception("Failed to retrieve network data $it")
        })

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}