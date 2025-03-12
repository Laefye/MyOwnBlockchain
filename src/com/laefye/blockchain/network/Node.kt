package com.laefye.blockchain.network

import com.laefye.blockchain.network.exceptions.NetworkException
import java.io.Serializable
import java.net.URI

class Node(val profile: Profile, private val connectManager: ConnectManager, val me: URI) {
    init {
        connectManager.setHandler(this::handle)
    }

    val peers = mutableMapOf<Identifier, URI>()

    private var handler: ((Header, FurProtoImpl, Any?) -> Unit)? = null

    fun setHandler(handler: (Header, FurProtoImpl, Any?) -> Unit) {
        this.handler = handler
    }

    private fun getNearest(identifier: Identifier, collection: Collection<Identifier>): Identifier? {
        return collection.minByOrNull { it.distanceTo(identifier) }
    }

    private fun getNodes(): Map<Identifier, URI> {
        return peers
    }

    private fun handle(stream: Stream) {
        val protocol = FurProtoImpl(stream, profile)
        val header = Box<Header>()
        try {
            while (!protocol.isClosed) {
                val body = protocol.readMessage(header)
                if (header.value.isUserMessage) {
                    handler?.invoke(header.value, protocol, body)
                } else {
                    when (body) {
                        is Command.Ping -> {
                            protocol.writeMessage(Command.Pong(), false)
                        }
                        is Command.Store -> {
                            if (header.value.id != profile.id) {
                                peers[header.value.id] = body.uri
                            }
                        }
                        is Command.GetNodes -> {
                            protocol.writeMessage(Command.NodesResult(getNodes()), false)
                        }
                        is Command.FindValue -> {
                            val value = peers[body.identifier]
                            if (value == null) {
                                protocol.writeMessage(Command.NodesResult(getNodes()), false)
                            } else {
                                protocol.writeMessage(Command.ValueResult(value), false)
                            }
                        }
                    }
                }
            }
        } catch (e: NetworkException) {
            if (e.message != NetworkException.IS_CLOSED) {
                e.printStackTrace()
            }
        }
    }

    fun listen() {
        Thread(connectManager::listen).start()
        Thread {
            while (!connectManager.isClosed) {
                try {
                    refresh()
                } catch (e: NetworkException) {
                    if (e.message != NetworkException.NOT_CONNECTED) {
                        throw e
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    fun connect(uri: URI, identifier: Identifier) : FurProtoImpl {
        val protocol = FurProtoImpl(connectManager.connect(uri), profile)
        protocol.writeMessage(Command.Ping(), false)
        val header = Box<Header>()
        val pong = protocol.readMessage(header)
        if (pong !is Command.Pong || header.value.id != identifier) {
            throw NetworkException(NetworkException.NOT_CONNECTED)
        }
        return protocol
    }

    fun connectById(identifier: Identifier) : FurProtoImpl {
        val uri = findValue(identifier) ?: throw NetworkException(NetworkException.NOT_CONNECTED)
        return connect(uri, identifier)
    }

    private fun store(protocol: FurProtoImpl, me: URI) {
        protocol.writeMessage(Command.Store(me), false)
    }

    private fun getNodes(protocol: FurProtoImpl): Map<Identifier, URI> {
        protocol.writeMessage(Command.GetNodes(), false)
        val result = protocol.readMessage()
        if (result !is Command.NodesResult) {
            throw NetworkException(NetworkException.NOT_CONNECTED)
        }
        return result.nodes
    }

    private fun findValue(protocol: FurProtoImpl, identifier: Identifier): Command.FindResult {
        protocol.writeMessage(Command.FindValue(identifier), false)
        val result = protocol.readMessage()
        if (result !is Command.FindResult) {
            throw NetworkException(NetworkException.NOT_CONNECTED)
        }
        return result
    }

    fun connectPeer(uri: URI, identifier: Identifier) {
        if (identifier != profile.id) {
            connect(uri, identifier).close()
            peers[identifier] = uri
        }
    }

    private fun refresh() {
        var size = 0
        while (size != peers.size) {
            size = peers.size
            val nearest = getNearest(profile.id, peers.keys)!!
            val uri = peers[nearest]!!
            val protocol = connect(uri, nearest)
            for (node in getNodes(protocol)) {
                if (node.key != profile.id) {
                    peers[node.key] = node.value
                }
            }
            protocol.close()
        }
        val nearest = getNearest(profile.id, peers.keys)
        if (nearest != null) {
            val protocol = connect(peers[nearest]!!, nearest)
            store(protocol, me)
            protocol.close()
        }
    }

    fun findValue(identifier: Identifier): URI? {
        val map = mutableMapOf<Identifier, URI>()
        map.putAll(peers)
        var size = 0
        while (size != map.size) {
            size = map.size
            val nearest = getNearest(identifier, map.keys)!!
            val uri = map[nearest]!!
            val protocol = connect(uri, nearest)
            val result = findValue(protocol, identifier)
            if (result is Command.ValueResult) {
                map[identifier] = result.uri
                break
            } else if (result is Command.NodesResult) {
                map.putAll(result.nodes)
            }
            protocol.close()
        }
        return map[identifier]
    }

    fun close() {
        connectManager.close()
    }
}