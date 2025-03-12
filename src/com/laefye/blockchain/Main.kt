package com.laefye.blockchain

import com.laefye.blockchain.blockchain.BlockchainClient
import com.laefye.blockchain.blockchain.Transaction
import com.laefye.blockchain.config.ProfileConfig
import com.laefye.blockchain.network.*
import java.net.URI
import java.util.Date

data class Me(
    val age: Int
)


fun main(args: Array<String>) {
    if (args.find { it == "-h" } != null) {
        println("-h - Show help")
        println("-p <URI> - Set your peer tcp://ip:port")
        return
    }
    val me = if (args.find { it == "-p" } != null) {
        URI.create(args[args.indexOfFirst { it == "-p" } + 1])
    } else {
        URI.create("tcp://localhost:4015")
    }
    val profile = ProfileConfig("profile.key", "profile-public.key").readProfile()
    val node = Node(profile, TcpConnectManager(me.port), me)
    val blockchainClient = BlockchainClient(node)
    node.setHandler(blockchainClient::handle)
    node.listen()
    println("Starting at $me")
    println("Your id in P2P network: ${profile.id}")
    println("add_peer $me ${profile.id}")
    while (true) {
        print(">")
        val command = readln().split(" ")
        if (command[0] == "stop") {
            node.close()
            break
        }
        if (command[0] == "find_peer") {
            val peer = node.findValue(Identifier.fromString(command[1]))
            if (peer != null) {
                println("${command[1]} - $peer")
            }
        }
        if (command[0] == "peers") {
            for (peer in node.peers) {
                println("${peer.key} - ${peer.value}")
            }
        }
        if (command[0] == "add_peer") {
            node.connectPeer(URI.create(command[1]), Identifier.fromString(command[2]))
        }
        if (command[0] == "mine") {
            blockchainClient.mine(listOf(Transaction(command[1], 1234)))
        }
        if (command[0] == "sync") {
            blockchainClient.sync()
        }
    }
}