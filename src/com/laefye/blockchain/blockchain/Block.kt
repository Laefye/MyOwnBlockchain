package com.laefye.blockchain.blockchain

import java.io.Serializable
import java.nio.ByteBuffer
import java.util.Base64

data class Block(var nonce: Int, var previous: ByteArray, val height: Int, val transactions: MutableList<Transaction>) : Serializable {

    val hash: ByteArray
        get() = Hash.hash(transactions.joinToString(" ") { Base64.getUrlEncoder().encodeToString(it.hash) } + nonce + Base64.getUrlEncoder().encodeToString(previous))

    fun validate(): Boolean {
        return hash.toHexString().startsWith("00000")
    }

    fun mine(): Block {
        while (!validate()) {
            nonce++
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (nonce != other.nonce) return false
        if (height != other.height) return false
        if (!previous.contentEquals(other.previous)) return false
        if (transactions != other.transactions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonce
        result = 31 * result + height
        result = 31 * result + previous.contentHashCode()
        result = 31 * result + transactions.hashCode()
        return result
    }
}