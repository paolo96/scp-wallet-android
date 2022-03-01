package com.scp.wallet.wallet

import com.scp.wallet.crypto.Crypto
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.exceptions.InvalidSeedStringException
import com.scp.wallet.exceptions.WalletLockedException
import com.scp.wallet.mnemonics.Dictionary
import com.scp.wallet.scp.*
import com.scp.wallet.utils.Bytes
import com.scp.wallet.utils.Hex
import kotlinx.coroutines.runBlocking
import java.math.BigInteger

class WalletHelper(private val dataAccess: WalletDataAccess) {

    //Converts a ByteArray seed into a human readable seed
    fun seedToString(seed: ByteArray, dictID: String?) : String {
        val fullChecksum = crypto.blake2b(seed)
        val checksumSeed = seed.copyOf() + fullChecksum.take(Wallet.SEED_CHECKSUM_SIZE)
        val seedWords = Dictionary.getDictionaryFromId(dictID).toPhrase(checksumSeed)
        seedWords?.let {
            return it.joinToString(" ")
        }
        throw Exception("Unable to convert seed (bytes) to a human readable seed")
    }

    //Converts a human readable seed into a ByteArray seed
    fun stringToSeed(str: String, dictID: String? = null) : ByteArray {

        for(c in str) {
            if(c.isUpperCase()) {
                throw InvalidSeedStringException("all words must be lowercase")
            }
            if(!c.isLetter() && !c.isWhitespace()) {
                throw InvalidSeedStringException("illegal character found '$c'")
            }
        }

        val strArray = str.split(" ").toTypedArray()

        if(dictID == null || Wallet.SUPPORTED_DICTIONARIES.contains(dictID)) {

            if(strArray.size != 28 && strArray.size != 29) {
                throw InvalidSeedStringException("seed length must be 28 or 29 words")
            }

            if(!str.matches(Regex("^([a-z]{4,12}){1}( {1}[a-z]{4,12}){27,28}$"))) {
                throw InvalidSeedStringException("invalid formatting")
            }

        } else {
            throw InvalidSeedStringException("unsupported dictionary $dictID")
        }

        val checksumSeedBytes = Dictionary.getDictionaryFromId(dictID).fromPhrase(strArray)
        checksumSeedBytes?.let {

            val seed = checksumSeedBytes.copyOf()

            if(checksumSeedBytes.size != 38) {
                throw InvalidSeedStringException("illegal number of bytes ${checksumSeedBytes.size}")
            }

            val fullChecksum = crypto.blake2b(checksumSeedBytes.take(Crypto.ENTROPY_SIZE).toByteArray())
            if(checksumSeedBytes.size != Crypto.ENTROPY_SIZE + Wallet.SEED_CHECKSUM_SIZE
                || !fullChecksum.take(Wallet.SEED_CHECKSUM_SIZE).toByteArray().contentEquals(checksumSeedBytes.takeLast(checksumSeedBytes.size - Crypto.ENTROPY_SIZE).toByteArray())) {

                throw InvalidSeedStringException("invalid checksum, check misspelled words")
            }

            return seed.take(checksumSeedBytes.size - Wallet.SEED_CHECKSUM_SIZE).toByteArray()
        }
        throw InvalidSeedStringException("conversion to bytes failed")

    }

    //initSeed is called when a new seed received
    fun initSeed(walletKey: ByteArray?, seed: ByteArray) {

        //If walletkey is null, use the seed as the walletkey
        val encryptionKey = walletKey ?: crypto.blake2b(seed)

        dataAccess.updateSeed(encryptionKey, seed)

    }

    fun nextAddress(seed: ByteArray) : SpendableKey {
        return nextAddresses(1, seed)[0]
    }

    fun nextAddresses(n: Int, seed: ByteArray) : Array<SpendableKey> {

        val currProgress = dataAccess.getProgress()
        dataAccess.updateProgress(currProgress+n)

        return generateKeys(seed, currProgress, n)

    }

    fun signTransaction(transaction: Transaction, keys: Array<SpendableKey>, toSign: Array<ByteArray>, height: Int) {

        val findUnlockConditions = fun (id: ByteArray) : UnlockConditions? {
            for (sci in transaction.inputs) {
                if(sci.parentId.contentEquals(id)) {
                    return sci.unlockConditions
                }
            }
            return null
        }

        val findSigningKey = fun(uc: UnlockConditions, puKeyIndex: Int) : ByteArray? {
            if(puKeyIndex >= uc.publicKeys.size) {
                return null
            }
            val pk = uc.publicKeys[puKeyIndex]
            keys.find{ it.unlockConditions.publicKeys.contentEquals(uc.publicKeys)}?.let {
                val sk = it

                val keysSecrets = sk.secretKeys ?: throw WalletLockedException()
                for(key in keysSecrets) {
                    val pubKey = crypto.publicKey(key)
                    if(pk.key.contentEquals(pubKey)) {
                        return key
                    }
                }

            }
            return null
        }

        for (id in toSign) {

            var sigIndex = -1
            for(i in transaction.signatures.indices) {
                if(transaction.signatures[i].parentId.contentEquals(id)) {
                    sigIndex = i
                    break
                }
            }
            if(sigIndex == -1) {
                throw Exception("toSign references signatures not present in transaction")
            }

            val uc = findUnlockConditions(id) ?: throw Exception("toSign references IDs not present in transaction")
            val sk = findSigningKey(uc, transaction.signatures[sigIndex].publicKeyIndex) ?: throw Exception("could not locate signing key for ${Hex.bytesToHexToString(id)}")

            val sigHash = transaction.sigHash(sigIndex, height)
            val encodedSig = crypto.signMessage(sigHash, sk)
            transaction.signatures[sigIndex].signature = encodedSig

        }


    }

    private fun generateKeys(seed: ByteArray, start: Int, n: Int) : Array<SpendableKey> {

        val keys = arrayListOf<SpendableKey>()
        runBlocking {
            repeat(n) { i ->
                keys.add(generateSpendableKey(seed, start+i))
            }
        }
        return keys.toTypedArray()

    }

    private fun generateSpendableKey(seed: ByteArray, index: Int): SpendableKey {

        val keySeed = crypto.blake2b(seed + Bytes.intToInt64ByteArray(index))
        val keyPair = crypto.generateKeyPairDeterministic(keySeed)
        return SpendableKey(
            arrayOf(keyPair.secretKey.asBytes), UnlockConditions(
                arrayOf(
                    ScpKey(keyPair.publicKey.asBytes)
                )
            )
        )

    }

    fun transactionValue(t: Transaction, keys: List<SpendableKey>, transactions: List<Transaction>) : CurrencyValue {
        var value = BigInteger.ZERO
        for(oI in t.outputs.indices) {
            if(keys.find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(t.outputs[oI].unlockHash) } != null) {
                value += t.outputs[oI].value.value
            }
        }
        for(iI in t.inputs.indices) {
            if(keys.find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(UnlockHash.fromUnlockConditions(t.inputs[iI].unlockConditions)) } != null) {
                val outputId = t.inputs[iI].parentId

                //If this input is from one of our outputs, it counts as spent value
                transactionsLoop@ for(transaction in transactions) {
                    for(o in transaction.outputs) {
                        if(o.id != null && o.id.contentEquals(outputId)) {
                            value -= o.value.value
                            break@transactionsLoop
                        }
                    }
                }

            }
        }
        return CurrencyValue(value)
    }


    //Creates a list of unspent outputs sorted by value
    //If consensusHeight is not null it also returns utxos in unconfirmed transactions
    fun getAllUnspentOutputs(transactions: List<Transaction>, keys: List<SpendableKey>, unconfirmed: Boolean) : Pair<List<ScpOutput>, List<ScpOutput>> {
        val scpOutputs = mutableListOf<ScpOutput>()
        val scpOutputsUnconfirmed = mutableListOf<ScpOutput>()

        runBlocking {
            repeat(transactions.size) { it ->
                val t = transactions[it]
                if(t.confirmationHeight != null) {
                    for(oI in t.outputs.indices) {
                        if(keys.find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(t.outputs[oI].unlockHash) } != null) {
                            val oId = t.outputs[oI].id ?: t.scpOutputId(oI)
                            if (isOutputUnspent(oId, transactions)) {
                                scpOutputs.add(t.outputs[oI])
                            }
                        }
                    }
                } else if(unconfirmed) {
                    for(oI in t.outputs.indices) {
                        if(keys.find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(t.outputs[oI].unlockHash) } != null) {
                            val oId = t.outputs[oI].id ?: t.scpOutputId(oI)
                            if (isOutputUnspent(oId, transactions)) {
                                scpOutputsUnconfirmed.add(t.outputs[oI])
                            }
                        }
                    }
                }
            }
        }

        scpOutputs.sortBy { it.value.value }

        return Pair(scpOutputs, scpOutputsUnconfirmed)
    }

    private fun isOutputUnspent(outputId: ByteArray, transactions: List<Transaction>) : Boolean {

        transactions.forEach { t ->
            t.inputs.forEach { i ->
                if(i.parentId.contentEquals(outputId)) {
                    return false
                }
            }
        }

        return true

    }

    fun hasAddressBeenUsed(addr: SpendableKey, transactions: List<Transaction>) : Boolean {

        for (t in transactions) {
            for(o in t.outputs) {
                if(UnlockHash.fromUnlockConditions(addr.unlockConditions).contentEquals(o.unlockHash)) {
                    return true
                }
            }
            for(i in t.inputs) {
                if(UnlockHash.fromUnlockConditions(addr.unlockConditions).contentEquals(UnlockHash.fromUnlockConditions(i.unlockConditions))) {
                    return true
                }
            }
        }
        return false

    }
    
}