package com.laefye.blockchain.network

import com.laefye.blockchain.network.exceptions.NetworkException
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI

class TcpConnectManager(port: Int) : ConnectManager {
    private val serverSocket = ServerSocket(port)
    private var handler: ((Stream) -> Unit)? = null

    override fun setHandler(handler: (Stream) -> Unit) {
        this.handler = handler
    }

    override fun listen() {
        while (!serverSocket.isClosed) {
            try {
                val socket = serverSocket.accept()
                handler?.invoke(TcpStream(socket))
            } catch (e: SocketException) {
                if (e.message == "Socket closed") {
                    break
                }
                throw e
            }
        }
    }

    override fun close() {
        serverSocket.close()
    }

    override fun connect(uri: URI): Stream {
        if (uri.scheme != "tcp") {
            throw NetworkException(NetworkException.NOT_CONNECTED)
        }
        val socket: Socket
        try {
            socket = Socket(uri.host, uri.port)
        } catch (e: ConnectException) {
            throw NetworkException(NetworkException.NOT_CONNECTED)
        }
        return TcpStream(socket)
    }

    override val isClosed: Boolean
        get() = serverSocket.isClosed
}