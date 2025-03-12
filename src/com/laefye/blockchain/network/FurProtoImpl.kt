package com.laefye.blockchain.network

import com.laefye.blockchain.network.exceptions.NetworkException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.SocketException
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


class FurProtoImpl(private val stream: Stream, private val profile: Profile) {
    private fun createPublicKey(publicKeyBytes: ByteArray): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(publicKeySpec)
    }

    private fun readHeader(): Header {
        val buffer = ByteArray(256)
        stream.read(buffer)
        val byteBuffer = ByteBuffer.wrap(buffer)
        val version = byteBuffer.getLong()
        val publicKeySize = byteBuffer.getInt()
        val publicKey = ByteArray(publicKeySize)
        byteBuffer.get(publicKey)
        val contentLength = byteBuffer.getInt()
        val signatureSize = byteBuffer.getInt()
        val signature = ByteArray(signatureSize)
        byteBuffer.get(signature)
        val isUserMessage = byteBuffer.get() == 1.toByte()
        return Header(
            version,
            createPublicKey(publicKey),
            contentLength,
            signature,
            isUserMessage,
        )
    }

    private fun writeHeader(header: Header) {
        val byteBuffer = ByteBuffer.allocate(256)
        byteBuffer.putLong(header.version)
        byteBuffer.putInt(header.publicKey.encoded.size)
        byteBuffer.put(header.publicKey.encoded)
        byteBuffer.putInt(header.contentLength)
        byteBuffer.putInt(header.sign.size)
        byteBuffer.put(header.sign)
        byteBuffer.put(if (header.isUserMessage) 1 else 0)
        stream.write(byteBuffer.array())
    }

    private fun writeRawBody(body: ByteArray) {
        stream.write(body)
    }

    private fun readRawBody(body: ByteArray) {
        stream.read(body)
    }

    fun close() {
        stream.close()
    }

    private fun writeRawMessage(body: ByteArray, isUserMessage: Boolean) {
        writeHeader(Header(
            profile.version,
            profile.keyPair.public,
            body.size,
            profile.sign(body),
            isUserMessage
        ))
        writeRawBody(body)
    }

    private fun readRawMessage(header: Box<Header>): ByteArray {
        val tempHeader = readHeader()
        if (tempHeader.version > profile.version) {
            throw NetworkException("Unsupported version")
        }
        val body = ByteArray(tempHeader.contentLength)
        readRawBody(body)
        if (!profile.verify(tempHeader.publicKey, tempHeader.sign, body)) {
            throw NetworkException("Invalid sign")
        }
        header.value = tempHeader
        return body
    }

    fun <T : Serializable> writeMessage(body: T, isUserMessage: Boolean = true) {
        val outputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(outputStream)
        objectOutputStream.writeObject(body)
        writeRawMessage(outputStream.toByteArray(), isUserMessage)
    }

    fun readMessage(header: Box<Header>? = null): Any? {
        val body = readRawMessage(header ?: Box())
        val inputStream = ByteArrayInputStream(body)
        val objectInputStream = ObjectInputStream(inputStream)
        return objectInputStream.readObject()
    }

    val isClosed: Boolean
        get() = stream.isClosed
}