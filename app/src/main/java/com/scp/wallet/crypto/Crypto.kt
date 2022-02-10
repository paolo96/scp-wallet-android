package com.scp.wallet.crypto

import com.goterl.lazysodium.LazySodium
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.utils.Key
import java.lang.Exception

class Crypto(private var lazySodium: LazySodium) {

    companion object {

        const val ENTROPY_SIZE = 32
        const val HASH_SIZE = 32
        const val PK_SIZE = 32
        const val ED25519_BYTES = 64

        const val XSALSA20_POLY1305_MACBYTES = 16
        const val XSALSA20_POLY1305_NONCEBYTES = 24

        lateinit var crypto: Crypto

        //Initializes Lazy sodium Android version, java version is used for unit testing instead
        fun initCryptoAndroid() {
            crypto = Crypto(LazySodiumAndroid(SodiumAndroid()))
        }

    }

    //Generates a random ByteArray of given size
    fun randomByteArray(size: Int) : ByteArray {
        return lazySodium.randomBytesBuf(size)
    }

    //Calculates Blake2b-256 hash of the given bytearray
    fun blake2b(input: ByteArray) : ByteArray {
        val result = ByteArray(HASH_SIZE)
        lazySodium.cryptoGenericHash(result, result.size, input.copyOf(), input.size.toLong())
        return result
    }

    //Generates a pair of public/private keys using ec25519 and the given entropy as seed
    fun generateKeyPairDeterministic(entropy: ByteArray) : com.goterl.lazysodium.utils.KeyPair {

        if(entropy.size != ENTROPY_SIZE) {
            throw Exception("Entropy of wrong size when generating key pair: ${entropy.size}")
        }
        return lazySodium.cryptoSignSeedKeypair(entropy.copyOf())

    }

    //Returns the public key for the given private key (Ed25519)
    fun publicKey(privateKey: ByteArray) : ByteArray {
        val publicKey = ByteArray(PK_SIZE)
        lazySodium.cryptoSignEd25519SkToPk(publicKey, privateKey.copyOf())
        return publicKey
    }

    //Signs a message using a private key
    fun signMessage(data: ByteArray, sk: ByteArray) : ByteArray {
        val signedMessage = ByteArray(data.size + ED25519_BYTES)
        lazySodium.cryptoSign(signedMessage, data.copyOf(), data.size.toLong(), sk.copyOf())
        return signedMessage
    }

    //Encrypts a message using XSALSA20 and POLY1305
    fun encryptMessage(message: ByteArray, nonce: ByteArray, key: ByteArray) : ByteArray {
        val encryptedMessage = ByteArray(XSALSA20_POLY1305_MACBYTES+message.size)
        lazySodium.cryptoSecretBoxEasy(encryptedMessage, message.copyOf(), message.size.toLong(), nonce.copyOf(), key.copyOf())
        return encryptedMessage
    }

    //Decrypts message encrypted with encryptMessage
    fun decryptMessage(encryptedMessage: ByteArray, nonce: ByteArray, key: ByteArray) : ByteArray {
        val decryptedMessage = ByteArray(encryptedMessage.size*2)
        if(lazySodium.cryptoSecretBoxOpenEasy(decryptedMessage, encryptedMessage.copyOf(), encryptedMessage.size.toLong(), nonce.copyOf(), key.copyOf())) {
            val nonZeroIndex = decryptedMessage.indexOfLast { it != 0x00.toByte() }
            return decryptedMessage.take(nonZeroIndex+1).toByteArray()
        } else {
            throw Exception("Decryption failed")
        }
    }

    //Encrypts a string message using XSALSA20 and POLY1305
    fun encryptMessageString(message: String, nonce: ByteArray, key: ByteArray) : String {
        return lazySodium.cryptoSecretBoxEasy(message, nonce.copyOf(), Key.fromBytes(key.copyOf()))
    }

    //Decrypts a string message encrypted with encryptMessage
    fun decryptMessageString(encryptedMessage: String, nonce: ByteArray, key: ByteArray) : String {
        return lazySodium.cryptoSecretBoxOpenEasy(encryptedMessage, nonce.copyOf(), Key.fromBytes(key.copyOf()))
    }

}

