package com.laefye.blockchain.network

import java.net.URI

interface ConnectManager {
    fun setHandler(handler: (Stream) -> Unit)
    fun listen()
    fun close()
    fun connect(uri: URI): Stream

    val isClosed: Boolean
}