package com.laefye.blockchain.blockchain

import com.laefye.blockchain.network.FurProtoImpl
import com.laefye.blockchain.network.Header
import com.laefye.blockchain.network.Identifier
import com.laefye.blockchain.network.Node
import com.laefye.blockchain.network.exceptions.NetworkException
import java.net.URI
import kotlin.system.exitProcess

class BlockchainClient(val node: Node) {
    val blockchain = Blockchain()
    var syncing = false

    fun handle(header: Header, protocol: FurProtoImpl, any: Any?) {
        if (any is Command.Mined) {
            if (blockchain.blocks.last().hash().contentEquals(any.previous)) {
                val block = Block(
                    any.nonce,
                    any.previous,
                    any.height,
                )
                block.transactions.addAll(any.transactions)
                if (block.validate()) {
                    blockchain.blocks.add(block)
                    println("${header.id} Mined new block")
                    for (i in node.peers) {
                        val friend = node.connect(i.value, i.key)
                        friend.writeMessage(Command.Mined(block.height, block.transactions, block.nonce, block.previous))
                        friend.close()
                    }
                }
            }
        } else if (any is Command.GetBlockchainState) {
            protocol.writeMessage(Command.BlockchainState(blockchain.blocks.last().height, syncing))
        } else if (any is Command.LookupBlock) {
            protocol.writeMessage(Command.Block(
                blockchain.blocks.last().height,
                blockchain.blocks[any.height].height,
                blockchain.blocks[any.height].transactions,
                blockchain.blocks[any.height].nonce,
                blockchain.blocks[any.height].previous,
            ))
        }
    }

    fun mine(transaction: List<Transaction>) {
        val block = Block(0, blockchain.blocks.last().hash(), blockchain.blocks.last().height + 1)
        block.transactions.addAll(transaction)
        println("Mining ${block.height}")
        block.mine()
        println("Mined ${block.hash().toHexString()}")
        blockchain.blocks += block
        try {
            for (i in node.peers) {
                val protocol = node.connect(i.value, i.key)
                protocol.writeMessage(Command.Mined(block.height, block.transactions, block.nonce, block.previous))
                protocol.close()
            }
        } catch (e: NetworkException) {
            if (e.message != NetworkException.NOT_CONNECTED) {
                throw e
            }
        }
    }

    fun sync() {
        syncing = true
        var top = 0
        var id: Identifier? = null
        var uri: URI? = null
        for (i in node.peers) {
            val protocol = node.connect(i.value, i.key)
            protocol.writeMessage(Command.GetBlockchainState())
            val state = protocol.readMessage()
            protocol.close()
            if (state !is Command.BlockchainState || state.syncing) {
                continue
            }
            if (state.height >= top) {
                top = state.height
                id = i.key
                uri = i.value
            }
        }
        if (id != null && uri != null) {
            val protocol = node.connect(uri, id)
            for (i in blockchain.blocks.last().height..top) {
                protocol.writeMessage(Command.LookupBlock(i))
                val result = protocol.readMessage()
                if (result !is Command.Block) {
                    continue
                }
                val block = Block(result.nonce, result.previous, result.height)
                block.transactions.addAll(result.transactions)
                if (!block.validate()) {
                    continue
                }
                blockchain.blocks.add(block)
            }
            protocol.close()
        }
        syncing = false
    }
}