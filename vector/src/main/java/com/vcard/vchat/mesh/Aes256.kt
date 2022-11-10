package com.vcard.vchat.mesh

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Aes256 {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16

    fun encryptGcm(plaintext: ByteArray?, passphrase: String): ByteArray {

        val keyBytes = ByteArray(32)
        System.arraycopy(passphrase.toByteArray(), 0, keyBytes, 0, passphrase.toByteArray().size)

        val key = SecretKeySpec(keyBytes, "AES")

        //Get Cipher Instance
        //BC provider is deprecated, so we don't put the params in
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val iv = ByteArray(GCM_IV_LENGTH)
        val random = SecureRandom()
        random.nextBytes(iv)

        // Create GCMParameterSpec
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        // Initialize Cipher for ENCRYPT_MODE
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec)

        val cipherText = cipher.doFinal(plaintext)

        val cipherWithIv = ByteBuffer.allocate(iv.size + cipherText.size)
                .put(iv)
                .put(cipherText)
                .array()

        // Perform Encryption
        return cipherWithIv
    }

    // prefix IV length + IV bytes to cipher text
    fun encryptGcmWithPrefixIv(pText: ByteArray, passphrase: String): ByteArray {
        val cipherText = encryptGcm(pText, passphrase)
        val iv = ByteArray(GCM_IV_LENGTH)

        val cipherWithIv = ByteBuffer.allocate(iv.size + cipherText.size)
                .put(iv)
                .put(cipherText)
                .array()

        return cipherWithIv
    }

    fun decryptGcm(encryptedText: ByteArray, passphrase: String): String {

        val bb = ByteBuffer.wrap(encryptedText)
        val iv = ByteArray(GCM_IV_LENGTH)
        bb[iv]

        val cipherText = ByteArray(bb.remaining())
        bb[cipherText]

        val keyBytes = ByteArray(32)
        System.arraycopy(passphrase.toByteArray(), 0, keyBytes, 0, passphrase.toByteArray().size)

        val key = SecretKeySpec(keyBytes, "AES")

        // Get Cipher Instance
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // Create GCMParameterSpec
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        // Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)

        return try {
            // Perform Decryption
            val decryptedText = cipher.doFinal(cipherText)
            NumberUtil.bytesToHexStr(decryptedText)
        }catch (e: javax.crypto.AEADBadTagException){
            "invalid passphrase"
        }
    }
}
