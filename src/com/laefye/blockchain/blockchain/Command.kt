package com.laefye.blockchain.blockchain

import com.laefye.blockchain.network.Command
import java.io.Serializable

open class Command : Serializable {
    class GetBlockchainState : Command()

    class BlockchainState(val height: Int, val syncing: Boolean) : Command()

    class Mined(val block: Block) : Command()

    class LookupBlock(val height: Int) : Command()
    class SentBlock(val block: Block?) : Command()
}