package com.scp.wallet.scp

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.scp.wallet.crypto.Crypto.Companion.HASH_SIZE
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.scp.ScpOutput.Companion.ID_SPECIFIER
import com.scp.wallet.utils.Bytes
import com.scp.wallet.utils.Hex
import java.lang.Exception
import java.math.BigInteger

class Transaction(@SerializedName("siacoininputs") var inputs: Array<ScpInput>, @SerializedName("siacoinoutputs") var outputs: Array<ScpOutput>, @SerializedName("minerfees") var minerFees: Array<CurrencyValue>, @SerializedName("arbitrarydata") var arbitraryData: Array<ByteArray> = arrayOf()) {

    companion object {
        val ESTIMATED_TRANSACTION_SIZE = CurrencyValue(BigInteger.valueOf(750))

        fun transactionFromJsonObject(tObject: JsonObject) : Transaction {

            val resultInputs = arrayListOf<ScpInput>()
            if(!tObject.get("siacoininputs").isJsonNull) {
                val inputs = tObject.get("siacoininputs").asJsonArray
                for(i in inputs) {

                    val resultPublicKeys = arrayListOf<ScpKey>()

                    val inputObject = i.asJsonObject
                    val unlockConditions = inputObject.get("unlockconditions").asJsonObject

                    if(!unlockConditions.get("publickeys").isJsonNull) {
                        val publicKeysResp = unlockConditions.get("publickeys").asJsonArray
                        for (pk in publicKeysResp) {
                            resultPublicKeys.add(ScpKey(Base64.decode(pk.asJsonObject.get("key").asString, Base64.NO_WRAP)))
                        }
                    }

                    resultInputs.add(ScpInput(Hex.stringToBytes(inputObject.get("parentid").asString), UnlockConditions(resultPublicKeys.toTypedArray(), unlockConditions.get("signaturesrequired").asInt, unlockConditions.get("timelock").asInt)))

                }
            }

            val resultOutputs = arrayListOf<ScpOutput>()
            if(!tObject.get("siacoinoutputs").isJsonNull) {
                val outputs = tObject.get("siacoinoutputs").asJsonArray
                for (o in outputs) {
                    val output = o.asJsonObject
                    val outputResult = ScpOutput(CurrencyValue.initFromString(output.get("value").asString), Hex.stringToBytes(output.get("unlockhash").asString.take(HASH_SIZE*2)))
                    resultOutputs.add(outputResult)
                }
            }


            val resultMinerFees = arrayListOf<CurrencyValue>()
            if(!tObject.get("minerfees").isJsonNull) {
                val mFees = tObject.get("minerfees").asJsonArray
                for (mFee in mFees) {
                    resultMinerFees.add(CurrencyValue.initFromString(mFee.asString))
                }
            }

            val resultTransaction = Transaction(resultInputs.toTypedArray(), resultOutputs.toTypedArray(), resultMinerFees.toTypedArray())
            val height = tObject.get("height").asInt
            resultTransaction.confirmationHeight = if(height == 0) null else height
            val blockTimestamp = tObject.get("blocktimestamp").asLong
            resultTransaction.blockTimestamp = if(blockTimestamp == 0L) null else blockTimestamp
            val tId = tObject.get("id").asString
            resultTransaction.id = if(tId == "") null else tId
            for(i in resultTransaction.outputs.indices) {
                resultTransaction.outputs[i].id = resultTransaction.scpOutputId(i)
            }

            return resultTransaction

        }

        fun fee(min: BigInteger, max: BigInteger) : CurrencyValue {
            val averageEstimate = (min + max) / BigInteger.valueOf(2)
            return CurrencyValue(averageEstimate) * ESTIMATED_TRANSACTION_SIZE
        }

    }

    var id: String? = null
    var confirmationHeight: Int? = null
    var blockTimestamp: Long? = null
    var walletValue: CurrencyValue? = null

    var confirmationBlocksPassed: Int? = null
    var signatures: Array<TransactionSignature> = arrayOf()

    // Returns the ID of a ScpOutput at the given index
    fun scpOutputId(i: Int) : ByteArray {

        var buf = Bytes.addTrailingZerosToByteArray(ScpKey.newSpecifier(ID_SPECIFIER), ScpKey.SPECIFIER_LEN)
        buf += dataToByteArray()
        buf += Bytes.intToInt64ByteArray(i)
        return crypto.blake2b(buf)

    }

    // Returns the hash of the fields in a transaction covered by a given signature.
    fun sigHash(i: Int, height: Int) : ByteArray {
        val sig = signatures[i]
        if(sig.wholeTransaction) {
            return wholeSigHash(sig, height)
        }
        partialSigHash(sig, height)
        throw Exception("Multisig not supported")
    }

    //Signs a simple transaction
    //Not capable of signing transactions containing: spf coins, file contracts,
    //contracts revisions, storage proofs
    fun wholeSigHash(sig: TransactionSignature, height: Int) : ByteArray {

        var buf = dataToByteArray()

        //transaction signature data
        buf += sig.parentId
        buf += Bytes.intToInt64ByteArray(sig.publicKeyIndex)
        buf += Bytes.intToInt64ByteArray(sig.timelock)

        return crypto.blake2b(buf)

    }

    //Signs a multisig transaction
    //Not implemented yet
    fun partialSigHash(sig: TransactionSignature, height: Int) : ByteArray {
        return byteArrayOf()
    }

    private fun dataToByteArray() : ByteArray {

        var buf = byteArrayOf()

        //inputs
        buf += Bytes.intToInt64ByteArray(inputs.size)
        for(i in inputs) {
            buf += i.toByteArray()
        }

        //outputs
        buf += Bytes.intToInt64ByteArray(outputs.size)
        for(i in outputs) {
            buf += i.toByteArray()
        }

        //file contracts
        buf += Bytes.intToInt64ByteArray(0)

        //file contracts revisions
        buf += Bytes.intToInt64ByteArray(0)

        //storageproofs
        buf += Bytes.intToInt64ByteArray(0)

        //spf inputs (not supported yet)
        buf += Bytes.intToInt64ByteArray(0)

        //spf outputs (not supported yet)
        buf += Bytes.intToInt64ByteArray(0)

        //miner fees
        buf += Bytes.intToInt64ByteArray(minerFees.size)
        for(i in minerFees) {
            buf += i.toByteArray()
        }

        //arbitrary data
        buf += Bytes.intToInt64ByteArray(arbitraryData.size)
        for(i in arbitraryData) {
            buf += Bytes.intToInt64ByteArray(i.size)
            buf += i
        }

        return buf
    }

    fun toJsonObject() : JsonObject {

        val jsonInputs = JsonArray(inputs.size)
        for(i in inputs) {

            val jsonInput = JsonObject()
            jsonInput.addProperty("parentid", Hex.bytesToHexToString(i.parentId))

            val jsonUnlockConditions = JsonObject()
            jsonUnlockConditions.addProperty("timelock", i.unlockConditions.timelock)
            jsonUnlockConditions.addProperty("signaturesrequired", i.unlockConditions.signatureRequired)

            val jsonPKs = JsonArray()

            for(pk in i.unlockConditions.publicKeys) {

                val jsonPK = JsonObject()
                jsonPK.addProperty("key", Base64.encodeToString(pk.key, Base64.NO_WRAP))
                jsonPK.addProperty("algorithm", ScpKey.ALGORITHM)
                jsonPKs.add(jsonPK)

            }

            jsonUnlockConditions.add("publickeys", jsonPKs)
            jsonInput.add("unlockconditions", jsonUnlockConditions)
            jsonInputs.add(jsonInput)

        }


        val jsonOutputs = JsonArray(outputs.size)
        for(o in outputs) {

            val jsonOutput = JsonObject()
            jsonOutput.addProperty("value", o.value.value.toString())
            jsonOutput.addProperty("unlockhash", Hex.bytesToHexToString(UnlockHash.appendChecksum(o.unlockHash)))
            jsonOutputs.add(jsonOutput)

        }

        val jsonMinerFees = JsonArray(minerFees.size)
        for(m in minerFees) {

            jsonMinerFees.add(m.value.toString())

        }


        val jsonSignatures = JsonArray(signatures.size)
        for(s in signatures) {

            s.signature?.let {
                val jsonSignature = JsonObject()
                jsonSignature.addProperty("parentid", Hex.bytesToHexToString(s.parentId))
                jsonSignature.addProperty("publickeyindex", s.publicKeyIndex)
                jsonSignature.addProperty("timelock", s.timelock)
                jsonSignature.addProperty("signature", Base64.encodeToString(it, Base64.NO_WRAP))

                val jsonCoveredFields = JsonObject()
                jsonCoveredFields.addProperty("wholetransaction", s.wholeTransaction)
                jsonCoveredFields.add("siacoininputs", JsonArray())
                jsonCoveredFields.add("siacoinoutputs", JsonArray())
                jsonCoveredFields.add("filecontracts", JsonArray())
                jsonCoveredFields.add("filecontractrevisions", JsonArray())
                jsonCoveredFields.add("storageproofs", JsonArray())
                jsonCoveredFields.add("siafundinputs", JsonArray())
                jsonCoveredFields.add("siafundoutputs", JsonArray())
                jsonCoveredFields.add("minerfees", JsonArray())
                jsonCoveredFields.add("arbitrarydata", JsonArray())
                jsonCoveredFields.add("transactionsignatures", JsonArray())

                jsonSignature.add("coveredfields", jsonCoveredFields)
                jsonSignatures.add(jsonSignature)
            }

        }

        val jsonTransaction = JsonObject()
        jsonTransaction.add("siacoininputs", jsonInputs)
        jsonTransaction.add("siacoinoutputs", jsonOutputs)
        jsonTransaction.add("minerfees", jsonMinerFees)
        jsonTransaction.add("transactionsignatures", jsonSignatures)
        jsonTransaction.add("filecontracts", JsonArray())
        jsonTransaction.add("filecontractrevisions", JsonArray())
        jsonTransaction.add("storageproofs", JsonArray())
        jsonTransaction.add("siafundinputs", JsonArray())
        jsonTransaction.add("siafundoutputs", JsonArray())
        jsonTransaction.add("arbitrarydata", JsonArray())

        return jsonTransaction

    }

    override fun equals(other: Any?): Boolean {

        if(other !is Transaction) {
            return false
        }
        
        val dataSame = other.walletValue?.value == this.walletValue?.value &&
                other.confirmationHeight == this.confirmationHeight &&
                other.blockTimestamp == this.blockTimestamp
        if(!dataSame) return false

        inputsLoop@for(i in other.inputs) {
            for(iN in this.inputs) {
                if(i.toByteArray().contentEquals(iN.toByteArray())) {
                    continue@inputsLoop
                }
            }
            return false
        }

        outputsLoop@for(o in other.outputs) {
            for(oN in this.outputs) {
                if(o.toByteArray().contentEquals(oN.toByteArray())) {
                    continue@outputsLoop
                }
            }
            return false
        }

        minerFeesLoop@for(m in other.minerFees) {
            for(mN in this.minerFees) {
                if(m.value == mN.value) {
                    continue@minerFeesLoop
                }
            }
            return false
        }

        return true
    }

}
