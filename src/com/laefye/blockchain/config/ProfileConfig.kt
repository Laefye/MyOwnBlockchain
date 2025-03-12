package com.laefye.blockchain.config

import com.laefye.blockchain.network.Profile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.exists

class ProfileConfig(private val publicFileName: String, private val privateFileName: String) {
    private fun readFile(fileName: String): ByteArray {
        val file = FileInputStream(fileName)
        val buffer = ByteArray(1024)
        val size = file.read(buffer)
        file.close()
        return buffer.copyOf(size)
    }

    fun readProfile(): Profile {
        if (Path.of(publicFileName).exists() && Path.of(privateFileName).exists()) {
            val profile = Profile.fromBytes(readFile(publicFileName), readFile(privateFileName))
            return profile
        }
        val profile = Profile.random()
        saveProfile(profile)
        return profile
    }

    private fun saveFile(fileName: String, byteArray: ByteArray) {
        val file = FileOutputStream(fileName)
        file.write(byteArray)
        file.close()
    }

    fun saveProfile(profile: Profile) {
        saveFile(publicFileName, profile.keyPair.public.encoded)
        saveFile(privateFileName, profile.keyPair.private.encoded)
    }
}