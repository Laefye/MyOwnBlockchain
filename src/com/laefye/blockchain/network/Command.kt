package com.laefye.blockchain.network

import java.io.Serializable
import java.net.URI

open class Command : Serializable {
    class Ping : Command()
    class Pong : Command()

    class Store(val uri: URI) : Command()

    class GetNodes() : Command()
    class FindValue(val identifier: Identifier) : Command()

    open class FindResult : Command()

    class NodesResult(val nodes: Map<Identifier, URI>) : FindResult()
    class ValueResult(val uri: URI) : FindResult()
}