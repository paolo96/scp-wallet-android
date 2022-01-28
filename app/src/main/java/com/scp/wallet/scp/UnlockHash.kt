package com.scp.wallet.scp

import com.scp.wallet.crypto.Crypto.Companion.HASH_SIZE
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.crypto.MerkleTree
import com.scp.wallet.exceptions.InvalidUnlockHashException
import com.scp.wallet.utils.Bytes
import com.scp.wallet.utils.Hex

object UnlockHash {

    private const val UNLOCK_HASH_CHECKSUM_LENGTH = 6

    //Appends a checksum of UNLOCK_HASH_CHECKSUM_LENGTH length.
    //Unlock hashes should always be checkedsummed when displaying them to the end users
    fun appendChecksum(unlockHash: ByteArray) : ByteArray {

        val checksum = crypto.blake2b(unlockHash)
        return unlockHash + checksum.take(UNLOCK_HASH_CHECKSUM_LENGTH)

    }

    //Returns the unlock hash bytes from the given hexadecimal address string
    fun fromString(input: String) : ByteArray {

        if(input.length != HASH_SIZE*2 + UNLOCK_HASH_CHECKSUM_LENGTH*2) {
            throw InvalidUnlockHashException("Wrong address length")
        }

        val inputBytes = Hex.stringToBytes(input)
        val unlockBytes = inputBytes.take(HASH_SIZE).toByteArray()
        val checksumBytes = inputBytes.takeLast(UNLOCK_HASH_CHECKSUM_LENGTH).toByteArray()

        val recalculatedChecksum = appendChecksum(unlockBytes).takeLast(UNLOCK_HASH_CHECKSUM_LENGTH).toByteArray()

        if(!recalculatedChecksum.contentEquals(checksumBytes)) {
            throw InvalidUnlockHashException("Invalid checksum")
        }

        return unlockBytes
    }

    //Calculates the unlock hash for the given unlock conditions
    fun fromUnlockConditions(uc: UnlockConditions) : ByteArray {

        val tree = MerkleTree()
        tree.push(Bytes.intToInt64ByteArray(uc.timelock))
        for(key in uc.publicKeys) {
            tree.push(key.toByteArray())
        }
        tree.push(Bytes.intToInt64ByteArray(uc.signatureRequired))
        return tree.root()

    }

    fun fromUnlockConditionsToAddress(uc: UnlockConditions) : String {
        return Hex.bytesToHexToString(appendChecksum(fromUnlockConditions(uc)))
    }

}