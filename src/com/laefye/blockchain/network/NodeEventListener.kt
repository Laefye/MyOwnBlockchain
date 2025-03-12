package com.laefye.blockchain.network

interface NodeEventListener {
    fun postRefresh(node: Node)
}