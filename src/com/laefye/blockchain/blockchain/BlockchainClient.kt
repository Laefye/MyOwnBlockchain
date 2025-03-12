package com.laefye.blockchain.blockchain

import com.laefye.blockchain.blockchain.db.Blockchain
import com.laefye.blockchain.network.FurProtoImpl
import com.laefye.blockchain.network.Header
import com.laefye.blockchain.network.Identifier
import com.laefye.blockchain.network.Node

class BlockchainClient(val node: Node) {

    val blockchain = Blockchain("blockchain.db")
    init {
        blockchain.init()
    }

    private var syncing = false

    fun handle(header: Header, protocol: FurProtoImpl, any: Any?) {
        when (any) {
            is Command.GetBlockchainState -> {
                protocol.writeMessage(Command.BlockchainState(blockchain.getLastBlock()?.height ?: 0, syncing))
            }
            is Command.LookupBlock -> {
                val block = blockchain.lookupBlock(any.height)
                protocol.writeMessage(Command.SentBlock(block))
            }
            is Command.Mined -> {
                if (syncing) return
                val lastBlock = blockchain.getLastBlock()
                if (lastBlock == null) {
                    println("Someone Mined Genesis Block ${any.block}")
                    blockchain.storeBlock(any.block)
                    broadcastMineBlock(any.block)
                } else if (any.block.previous.contentEquals(lastBlock.hash) && any.block.validate()) {
                    println("Someone Mined Block ${any.block}")
                    blockchain.storeBlock(any.block)
                    broadcastMineBlock(any.block)
                }
            }
        }
    }

    private fun broadcastMineBlock(block: Block) {
        for (peer in node.peers) {
            val protocol = node.connect(peer.value, peer.key)
            protocol.writeMessage(Command.Mined(block))
            protocol.close()
        }
    }

    fun mine(transaction: List<Transaction>) {
        val lastBlock = blockchain.getLastBlock()
        val block = Block(0, lastBlock?.hash ?: byteArrayOf(), (lastBlock?.height ?: 0) + 1, transaction.toMutableList())
        println("Started mining (previous ${lastBlock?.hash?.toHexString()}")
        block.mine()
        println("I mined $block ${block.hash.toHexString()}")
        blockchain.storeBlock(block)
        broadcastMineBlock(block)
    }

    private fun getPeerMaximalHeight(): Identifier? {
        var id: Identifier? = null
        var height = 0
        for (peer in node.peers) {
            val protocol = node.connect(peer.value, peer.key)
            val state = getBlockchainState(protocol)
            protocol.close()
            if (state == null) {
                continue
            }
            if (state.height > height) {
                height = state.height
                id = peer.key
            }
        }
        return id
    }

    private fun getBlockchainState(protocol: FurProtoImpl): Command.BlockchainState? {
        protocol.writeMessage(Command.GetBlockchainState())
        val state = protocol.readMessage()
        if (state !is Command.BlockchainState || state.syncing) {
            return null
        }
        return state
    }

    fun lookupBlock(protocol: FurProtoImpl, height: Int): Block? {
        protocol.writeMessage(Command.LookupBlock(height))
        val state = protocol.readMessage()
        if (state !is Command.SentBlock) {
            return null
        }
        return state.block
    }

    fun syncFromNode(id: Identifier) {
        val protocol = node.connectById(id)
        var currentHeight = blockchain.getLastBlock()?.height ?: 0
        var state = getBlockchainState(protocol)!!
        while (state.height != currentHeight) {
            for (i in currentHeight+1..state.height) {
                val block = lookupBlock(protocol, i)
                if (block != null) {
                    println("Syncing: Added $block")
                    blockchain.storeBlock(block)
                    currentHeight = i
                }
            }
            state = getBlockchainState(protocol)!!
        }
        protocol.close()
    }

    fun sync() {
        syncing = true
        val maximalPeer = getPeerMaximalHeight()
        if (maximalPeer != null) {
            println("Started syncing")
            syncFromNode(maximalPeer)
            println("Stopped syncing")
        }
        syncing = false
    }
}