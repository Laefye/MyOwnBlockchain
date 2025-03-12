package com.laefye.blockchain.network

import java.io.Serializable
import java.math.BigInteger
import java.util.Base64
import kotlin.math.min

class Identifier(private val bytes: ByteArray) : Serializable {
    companion object {
        fun fromString(base64: String): Identifier {
            return Identifier(Base64.getUrlDecoder().decode(base64))
        }
    }

    override fun toString(): String {
        return Base64.getUrlEncoder().encodeToString(bytes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return false
        if (other !is Identifier) return false
        return this.bytes.contentEquals(other.bytes)
    }

    fun distanceTo(other: Identifier): BigInteger {
        val bytes = ByteArray(min(this.bytes.size, other.bytes.size))
        for (i in bytes.indices) {
            bytes[i] = (this.bytes[i].toInt() xor other.bytes[i].toInt()).toByte()
        }
        return BigInteger(1, bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}