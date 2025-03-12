package com.laefye.blockchain.network

import com.laefye.blockchain.network.exceptions.NetworkException
import java.net.Socket

class TcpStream(private val socket: Socket) : Stream {
    override val isClosed: Boolean
        get() = socket.isClosed

    override fun read(byteArray: ByteArray): Int {
        val size = socket.getInputStream().read(byteArray)
        if (size == -1) {
            throw NetworkException(NetworkException.IS_CLOSED)
        }
        return size
    }

    override fun write(byteArray: ByteArray) {
        socket.getOutputStream().write(byteArray)
    }

    override fun close() {
        socket.close()
    }
}