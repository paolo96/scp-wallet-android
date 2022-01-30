package com.scp.wallet.scp

import com.google.gson.Gson
import com.scp.wallet.api.API
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.utils.Hex
import com.scp.wallet.wallet.Wallet
import java.lang.Exception
import java.math.BigInteger

class TransactionBuilder(val wallet: Wallet) {

    var parents = arrayListOf<Transaction>()
    var transaction = Transaction(arrayOf(), arrayOf(), arrayOf())

    var newParents = arrayListOf<Int>()
    var scpInputs = arrayListOf<Int>()
    var signatures = arrayListOf<Int>()

    fun newFoundedByParents(amountWithoutFee: CurrencyValue, dest: ByteArray, feeIncluded: Boolean, callback: (Pair<Array<Transaction>, Transaction>) -> Unit, callbackError: (String) -> Unit) {

        API.getScprimeData({ consensusHeight, min, max, _ ->

            wallet.getSeed()?.let { seed ->

                val createdAddresses = arrayListOf<String>()

                val dustThreshold = min * BigInteger.valueOf(3)
                val fee = Transaction.fee(min, max)

                val amount = if(feeIncluded) {
                    if(amountWithoutFee.value <= fee.value) {
                        callbackError("There isn't enough balance to cover the miner fees.")
                        return@let
                    }
                    amountWithoutFee
                } else {
                    amountWithoutFee+fee
                }

                val parentNewAddress = wallet.getHelper().nextAddress(seed)
                val parentUnlockConditions = parentNewAddress.unlockConditions
                wallet.addKeys(arrayOf(parentNewAddress))
                createdAddresses.add(UnlockHash.fromUnlockConditionsToAddress(parentNewAddress.unlockConditions))

                val parentTxnInputs = arrayListOf<ScpInput>()
                val parentTxnOutputs = arrayListOf<ScpOutput>()

                val scpOutputsResult = wallet.getHelper().getAllUnspentOutputs(wallet.getTransactions(), wallet.getKeys(), true)
                val scpOutputs = scpOutputsResult.first
                val scpOutputsRespendTimeout = scpOutputsResult.second

                //Check if the wallet has enough funds to send the transaction
                //Also checks if the funds are in unconfirmed transactions and stops if the
                //necessary funds are in unconfirmed transactions
                var fund = CurrencyValue(BigInteger.valueOf(0))
                var potentialFund = CurrencyValue(scpOutputsRespendTimeout.sumOf { it.value.value })
                val spentOutputs = arrayListOf<ScpOutput>()
                outputsLoop@ for(i in scpOutputs.indices) {

                    if(scpOutputs[i].id == null) {
                        println("[unexpected] unspent ScpOutput without id")
                        continue
                    }

                    if(scpOutputs[i].value.value < dustThreshold) {
                        continue
                    }

                    val outputKey = wallet.getKeys().find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(scpOutputs[i].unlockHash) }
                    if(outputKey == null) {
                        println("[unexpected] key not found for ScpOutput belonging to the wallet -> ${Hex.bytesToHexToString(UnlockHash.appendChecksum(scpOutputs[i].unlockHash))}")
                        continue
                    }

                    if(consensusHeight < outputKey.unlockConditions.timelock) {
                        continue
                    }

                    val scpInput = ScpInput(scpOutputs[i].id!!, outputKey.unlockConditions)
                    parentTxnInputs.add(scpInput)
                    spentOutputs.add(scpOutputs[i])

                    fund += scpOutputs[i].value
                    potentialFund += scpOutputs[i].value

                    if(fund.value >= amount.value) {
                        break
                    }
                }
                if(fund.value < amount.value) {
                    if(potentialFund.value >= amount.value) {
                        callbackError("Necessary funds are in unconfirmed transactions. Wait until the pending transactions have been confirmed.")
                    } else {
                        callbackError("Cannot send ${amount.toScpReadable()}. The balance for this wallet is ${potentialFund.toScpReadable()}.")
                    }
                    return@let
                }

                val exactOutput = ScpOutput(amount, UnlockHash.fromUnlockConditions(parentUnlockConditions))
                parentTxnOutputs.add(exactOutput)

                if(amount.value != fund.value) {
                    val refundAddress = wallet.getHelper().nextAddress(seed)
                    createdAddresses.add(UnlockHash.fromUnlockConditionsToAddress(refundAddress.unlockConditions))
                    val refundUnlockConditions = refundAddress.unlockConditions
                    wallet.addKeys(arrayOf(refundAddress))
                    val refundOutput = ScpOutput(fund-amount, UnlockHash.fromUnlockConditions(refundUnlockConditions))
                    parentTxnOutputs.add(refundOutput)
                }

                val parentTxn = Transaction(parentTxnInputs.toTypedArray(), parentTxnOutputs.toTypedArray(), arrayOf())

                for(scpInput in parentTxn.inputs) {
                    wallet.getKeys().find{ it.unlockConditions.publicKeys.contentEquals(scpInput.unlockConditions.publicKeys)}?.let { key ->
                        key.secretKeys?.let { secretKeys ->
                            addSignatures(parentTxn, scpInput.unlockConditions, scpInput.parentId, secretKeys, consensusHeight)
                        }
                    }
                }

                newParents.add(parents.size)
                parents.add(parentTxn)
                scpInputs.add(transaction.inputs.size)

                val parentTxnOutputId = parentTxn.scpOutputId(0)
                val newInput = ScpInput(parentTxnOutputId, parentUnlockConditions)
                val newInputs = arrayListOf(newInput) + transaction.inputs
                transaction.inputs = newInputs.toTypedArray()
                val newMinerFees = arrayListOf(fee) + transaction.minerFees
                transaction.minerFees = newMinerFees.toTypedArray()

                val output = ScpOutput(amount-fee, dest)
                val newOutputs = arrayListOf(output) + transaction.outputs
                transaction.outputs = newOutputs.toTypedArray()

                try {
                    callback(sign(consensusHeight))
                } catch (e: Exception) {
                    callbackError("There was an exception while trying to sign the new transaction.")
                }

            }
        }, {
            callbackError("The transaction could not be created due to a connection issue")
        })

    }

    private fun sign(consensusHeight: Int) : Pair<Array<Transaction>, Transaction> {

        for (inputIndex in scpInputs) {
            val input = transaction.inputs[inputIndex]
            val keyFound = wallet.getKeys().find { UnlockHash.fromUnlockConditions(it.unlockConditions).contentEquals(UnlockHash.fromUnlockConditions(input.unlockConditions)) }
            if(keyFound != null){
                val newSigIndexes = addSignatures(transaction, input.unlockConditions, input.parentId, keyFound.secretKeys ?: throw Exception("Wallet keys are not loaded"), consensusHeight)
                signatures.add(newSigIndexes)
                continue
            }
            throw Exception("transaction builder added an input that it cannot sign")
        }

        return Pair(parents.toTypedArray(), transaction)
    }

    //Multisig not supported currently
    private fun addSignatures(txn: Transaction, uc: UnlockConditions, parentId: ByteArray, secretKeys: Array<ByteArray>, height: Int) : Int {

        var newSigIndex = 0

        pkLoop@for(i in uc.publicKeys.indices) {
            for (j in secretKeys.indices) {
                val pubKey = Crypto.crypto.publicKey(secretKeys[j])
                if(!uc.publicKeys[i].key.contentEquals(pubKey)) {
                    continue
                }

                val sig = TransactionSignature(parentId, i)
                newSigIndex = txn.signatures.size
                val newSigs = arrayListOf(sig)
                newSigs.addAll(txn.signatures)
                txn.signatures = newSigs.toTypedArray()
                val sigIndex = txn.signatures.size-1
                val sigHash = txn.sigHash(sigIndex, height)
                val encodedSig = Crypto.crypto.signMessage(sigHash, secretKeys[j])
                txn.signatures[sigIndex].signature = encodedSig

                break@pkLoop
            }
        }

        return newSigIndex
    }

}