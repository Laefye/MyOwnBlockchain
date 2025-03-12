package com.laefye.blockchain.network

import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


class Profile(val version: Long, val keyPair: KeyPair) {
    val id: Identifier
        get() = getId(keyPair.public)

    companion object {
        private const val VERSION = 1L

        fun random(): Profile {
            val ecSpec = ECGenParameterSpec("secp256r1")
            val g = KeyPairGenerator.getInstance("EC")
            g.initialize(ecSpec, SecureRandom())
            return Profile(VERSION, g.generateKeyPair())
        }

        fun fromBytes(publicKeyBytes: ByteArray, privateKeyBytes: ByteArray): Profile {
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKey = keyFactory.generatePrivate(privateKeySpec)
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            return Profile(VERSION, KeyPair(publicKey, privateKey))
        }

        fun getId(publicKey: PublicKey): Identifier {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            return Identifier(messageDigest.digest(publicKey.encoded))
        }
    }

    private fun createSignature(): Signature = Signature.getInstance("SHA256withECDSA")

    fun sign(body: ByteArray): ByteArray {
        val signature = createSignature()
        signature.initSign(keyPair.private)
        signature.update(body)
        return signature.sign()
    }

    fun verify(publicKey: PublicKey, sign: ByteArray, body: ByteArray): Boolean {
        val signature = createSignature()
        signature.initVerify(publicKey)
        signature.update(body)
        return signature.verify(sign)
    }
}