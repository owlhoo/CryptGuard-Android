package com.example.cryptguard

import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec

object Encrypter {
    private const val ITERATION_COUNT = 50_000
    private const val AES_KEY_LEN = 256

    // Encrypt the data from stream, encode it to base64 and append salt and iv to it (base64 too)
    @RequiresApi(Build.VERSION_CODES.O)
    fun encryptStreamAndGetBase64(input: InputStream, password: String): String {
        // create the salt
        val sr = SecureRandom.getInstanceStrong()
        val passwordSalt = ByteArray(16)
        sr.nextBytes(passwordSalt)

        // create the key for aes
        val keySpec = PBEKeySpec(password.toCharArray(), passwordSalt, ITERATION_COUNT, AES_KEY_LEN)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(keySpec)

        // create the cipher and initialise it with key
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        // read from input stream and encrypt the data
        var result: ByteArray
        input.buffered().use {
            val bytes = it.readBytes()
            result = cipher.doFinal(bytes)
        }

        // return the encrypted data in base64
        return appendCipherDataToBase64(result, passwordSalt, cipher.iv)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decryptStream(input: InputStream, password: String): String {
        val reader = input.bufferedReader()
        val cipherText = reader.readLine().fromBase64()
        val passwordSalt = reader.readLine().fromBase64()
        val initVector = reader.readLine().fromBase64()

        // create the key for aes
        val keySpec = PBEKeySpec(password.toCharArray(), passwordSalt, ITERATION_COUNT, AES_KEY_LEN)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(keySpec)

        // create init vector from the one read from file
        val ivSpec = IvParameterSpec(initVector)

        // create the cipher and initialise it with key
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun encryptStringAndGetBase64(inputString: String, password: String): String {
        // create the salt
        val sr = SecureRandom.getInstanceStrong()
        val passwordSalt = ByteArray(16)
        sr.nextBytes(passwordSalt)

        // create the key for aes
        val keySpec = PBEKeySpec(password.toCharArray(), passwordSalt, ITERATION_COUNT, AES_KEY_LEN)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(keySpec)

        // create the cipher and initialise it with key
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val bytes = inputString.toByteArray()
        val result = cipher.doFinal(bytes)

        // return the encrypted data in base64
        return appendCipherDataToBase64(result, passwordSalt, cipher.iv)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decryptBase64String(inputString: String, password: String, salt: String, initVector: String): String {
        val cipherText = inputString.fromBase64()
        val passwordSalt = salt.fromBase64()
        val iv = initVector.fromBase64()

        // create the key for aes
        val keySpec = PBEKeySpec(password.toCharArray(), passwordSalt, ITERATION_COUNT, AES_KEY_LEN)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(keySpec)

        // create init vector from the one read from file
        val ivSpec = IvParameterSpec(iv)

        // create the cipher and initialise it with key
        val cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun ByteArray.toBase64(): String {
        return String(Base64.encode(this, Base64.NO_WRAP))
    }

    private fun String.fromBase64(): ByteArray? {
        return android.util.Base64.decode(this, Base64.NO_WRAP)
    }

    private fun appendCipherDataToBase64(
        cipherText: ByteArray,
        passwordSalt: ByteArray,
        initVector: ByteArray
    ): String {
        val cipherTextBase64 = cipherText.toBase64().plus("\n")
        val passwordSaltBase64 = passwordSalt.toBase64().plus("\n")
        val initVectorBase64 = initVector.toBase64().plus("\n")

        return cipherTextBase64.plus(passwordSaltBase64 + initVectorBase64)
    }
}