package com.laefye.blockchain.network

class Box<T> {
    private var shadowValue: T? = null

    var executed: Boolean = false
        private set

    var value: T
        get() {
            if (executed) {
                return shadowValue!!
            }
            throw RuntimeException("Not executed")
        }
        set(value) {
            executed = true
            shadowValue = value
        }
}
