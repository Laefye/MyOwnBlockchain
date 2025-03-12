package com.laefye.blockchain.network

import java.security.PublicKey

data class Header(
    val version: Long,
    val publicKey: PublicKey,
    val contentLength: Int,
    val sign: ByteArray,
    val isUserMessage: Boolean,
) {
    val id: Identifier
        get() = Profile.getId(publicKey)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Header

        if (version != other.version) return false
        if (contentLength != other.contentLength) return false
        if (isUserMessage != other.isUserMessage) return false
        if (publicKey != other.publicKey) return false
        if (!sign.contentEquals(other.sign)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + contentLength
        result = 31 * result + isUserMessage.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + sign.contentHashCode()
        return result
    }
}
