package com.laefye.blockchain.blockchain

import java.io.Serializable

data class Transaction(
    val text: String,
    val timestamp: Long,
) : Serializable {
    val hash: ByteArray
        get() = Hash.hash("$text - $timestamp")
}
