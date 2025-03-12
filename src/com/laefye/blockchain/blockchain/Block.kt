package com.laefye.blockchain.blockchain

import java.nio.ByteBuffer
import java.util.Base64

class Block(var nonce: Int, var previous: ByteArray, val height: Int) {
    val transactions = mutableListOf<Transaction>()

    fun hash(): ByteArray {
        return Hash.hash(transactions.joinToString(" ") { Base64.getUrlEncoder().encodeToString(it.hash()) } + nonce + Base64.getUrlEncoder().encodeToString(previous))
    }

    fun validate(): Boolean {
        if (height == 0) {
            return true
        }
        if (hash().toHexString().startsWith("00000")) {
            return true
        }
        return false
    }

    fun mine(): Block {
        while (!validate()) {
            nonce++
        }
        return this
    }
}