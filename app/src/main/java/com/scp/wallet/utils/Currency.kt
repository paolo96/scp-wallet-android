package com.scp.wallet.utils

object Currency {

    private val supportedCurrencies = arrayOf(
        Pair("USD","$"),
        Pair("EUR","€"),
        Pair("JPY","¥"),
        Pair("GBP","£"),
        Pair("AUD","A$"),
        Pair("CAD","C$"),
        Pair("CHF","CHF"),
        Pair("CNY","¥"),
        Pair("HKD","HK$"),
        Pair("NZD","NZ$"),
        Pair("SEK","kr"),
        Pair("INR","₹")
    )

    fun getCurrencies() : List<String> {
        return supportedCurrencies.map { it.first }
    }

    fun getSymbol(currency: String) : String {
        return supportedCurrencies.find { it.first == currency }?.second ?: "$"
    }

}