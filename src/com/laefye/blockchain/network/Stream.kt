package com.laefye.blockchain.network

interface Stream {
    val isClosed: Boolean

    fun read(byteArray: ByteArray): Int

    fun write(byteArray: ByteArray)

    fun close()
}