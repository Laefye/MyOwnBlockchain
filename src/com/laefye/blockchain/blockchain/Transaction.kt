package com.laefye.blockchain.blockchain

import java.io.Serializable

data class Transaction(
    val text: String,
    val timestamp: Long,
) : Serializable {
    fun hash(): ByteArray {
        return Hash.hash("$text - $timestamp")
    }
}
