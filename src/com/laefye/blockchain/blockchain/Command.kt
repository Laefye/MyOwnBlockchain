package com.laefye.blockchain.blockchain

import com.laefye.blockchain.network.Command
import java.io.Serializable

open class Command : Serializable {
    class GetBlockchainState : Command()

    class BlockchainState(val height: Int, val syncing: Boolean) : Command()

    class Mined(val height: Int, val transactions: List<Transaction>, val nonce: Int, val previous: ByteArray) : Command()

    class LookupBlock(val height: Int) : Command()

    class Block(val currentHeight: Int, val height: Int, val transactions: List<Transaction>, val nonce: Int, val previous: ByteArray) : Command()
}