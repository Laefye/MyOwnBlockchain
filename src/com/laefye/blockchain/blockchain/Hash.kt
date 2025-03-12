package com.laefye.blockchain.blockchain

import com.laefye.blockchain.network.Identifier
import java.security.MessageDigest

class Hash {
    companion object {
        fun hash(string: String): ByteArray {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            return messageDigest.digest(string.toByteArray())
        }
    }
}