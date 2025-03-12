package com.laefye.blockchain.network.exceptions

class NetworkException(message: String) : Exception(message) {
    companion object {
        const val NOT_CONNECTED = "Not Connected"
        const val IS_CLOSED = "Closed"
    }
}