package com.scp.wallet.api

import android.util.Base64
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.*
import java.math.BigInteger
import com.scp.wallet.scp.*


object API {

    var host: String? = null
    private const val VERSION = "1.0"

    //TODO only https servers should be trusted
    val TRUSTED_HOSTS = arrayListOf(
        "http://34.242.255.125:14280"
    )

    private fun getEndpoint() : String {
        if(host == null) {
            //This shouldn't happen if the app is launched from LaunchActivity
            host = TRUSTED_HOSTS.random()
        }
        return "$host/$VERSION"
    }
    
    fun getScprimeData(callback: (consensusHeight: Int, minFee: BigInteger, maxFee: BigInteger, scpUsd: Double?) -> Unit, callbackErr: (Int?) -> Unit) {

        getRequest("${getEndpoint()}/scprime/data", null, { jsonObject ->

            try {
                val scpUsd = if(jsonObject.get("scpPrice").isJsonNull) {
                    null
                } else {
                    jsonObject.get("scpPrice").asDouble
                }

                val networkData = jsonObject.get("networkData").asJsonObject
                val consensusHeight = networkData.get("consensusHeight").asInt
                val minFee = networkData.get("minFee").asBigInteger
                val maxFee = networkData.get("maxFee").asBigInteger
                callback(consensusHeight, minFee, maxFee, scpUsd)
            } catch (e: JsonParseException) {
                callbackErr(null)
            }

        }, callbackErr)

    }

    fun getTransactions(addresses: Array<UnlockConditions>, callback: (Array<Transaction>) -> Unit, callbackErr: (Int?) -> Unit) {

        val unlockHashes = addresses.map { UnlockHash.fromUnlockConditionsToAddress(it) }
        val publicKeys = arrayListOf<String>()
        for (ad in addresses) {
            for (pk in ad.publicKeys) {
                publicKeys.add(Base64.encodeToString(pk.key, Base64.NO_WRAP))
            }
        }

        val jsonRequest = Gson().toJson(mapOf(Pair("addresses", unlockHashes), Pair("publickeys", publicKeys)))
        postRequest("${getEndpoint()}/addresses/transactions/batch", jsonRequest, { jsonObject ->

            try {
                if(jsonObject.get("transactions").isJsonNull) {
                    callback(arrayOf())
                } else {

                    val result = arrayListOf<Transaction>()
                    val transactions = jsonObject.get("transactions").asJsonArray

                    for (t in transactions) {
                        val tObject = t.asJsonObject
                        result.add(Transaction.transactionFromJsonObject(tObject))
                    }

                    callback(result.toTypedArray())

                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
                callbackErr(null)
            }

        }, {
            callbackErr(it)
        })

    }

    fun postTransactions(parents: Array<Transaction>, transaction: Transaction, callback: (Boolean) -> Unit) {

        val txns = JsonObject()
        val parentsJson = JsonArray(parents.size)
        parents.forEach {
            parentsJson.add(it.toJsonObject())
        }
        txns.addProperty("parents", Gson().toJson(parentsJson))
        txns.addProperty("transaction", Gson().toJson(transaction.toJsonObject()))

        val txnsValidate = JsonArray(parents.size+1)
        parents.forEach {
            txnsValidate.add(it.toJsonObject())
        }
        txnsValidate.add(transaction.toJsonObject())


        val requestJson = JsonObject()
        requestJson.addProperty("validateData", Gson().toJson(txnsValidate))
        requestJson.add("broadcastData", txns)

        postRequest("${getEndpoint()}/transactions", Gson().toJson(requestJson), {
            callback(true)
        }, {
            callback(false)
        })

    }

    private fun getRequest(url: String, params: List<Pair<String, String>>? = null, callback: (JsonObject) -> Unit, callbackErr: (Int?) -> Unit) {

        Fuel.get(url, params).responseString { _, _, result ->
            result.fold({ d ->

                try {

                    val jsonD = JsonParser.parseString(d).asJsonObject

                    callback(jsonD)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callbackErr(null)
                }

            }, { err ->
                callbackErr(err.response.statusCode)
            })
        }

    }

    private fun postRequest(url: String, json: String, callback: (JsonObject) -> Unit, callbackErr: (Int?) -> Unit) {

        Fuel.post(url).jsonBody(json).responseString { _, _, result ->
            result.fold({ d ->

                try {

                    val jsonD = JsonParser.parseString(d).asJsonObject

                    callback(jsonD)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callbackErr(null)
                }

            }, { err ->
                callbackErr(err.response.statusCode)
            })
        }

    }

}