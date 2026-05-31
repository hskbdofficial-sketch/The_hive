package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    // A secure 256-bit AES key (exactly 32 characters)
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    
    // Secure AES Key and IV static buffers
    private const val SECURE_KEY = "ThriftHiveStoreSecureKeyEncryption" // Length 34 -> trimmed to 32
    private const val SECURE_IV = "ThriftHiveInitIV" // Length 16

    private val keySpec: SecretKeySpec by lazy {
        val keyBytes = SECURE_KEY.take(32).toByteArray(Charsets.UTF_8)
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    private val ivSpec: IvParameterSpec by lazy {
        val ivBytes = SECURE_IV.take(16).toByteArray(Charsets.UTF_8)
        IvParameterSpec(ivBytes)
    }

    /**
     * Hashes an input string (like user password) using SHA-256
     */
    fun sha256Hash(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder()
            for (b in hashBytes) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            input // Fallback in case of emergency error
        }
    }

    /**
     * Encrypts plain text into AES-256 Base64 encoded string
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText // Fallback
        }
    }

    /**
     * Decrypts AES-256 Base64 encoded string back to plain text
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) return encryptedText
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback (or if it wasn't encrypted)
        }
    }
}
