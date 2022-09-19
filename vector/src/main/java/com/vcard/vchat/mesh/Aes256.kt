package com.vcard.vchat.mesh

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import com.facebook.common.util.Hex.hexStringToByteArray
import org.bouncycastle.jce.provider.BouncyCastleProvider
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Aes256 {

    const val PROVIDER = "BC"
    const val SALT_LENGTH = 20
    const val IV_LENGTH = 16
    const val PBE_ITERATION_COUNT = 100

    private const val RANDOM_ALGORITHM = "SHA1PRNG"
    private const val PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC"
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    const val SECRET_KEY_ALGORITHM = "AES"

    const val AES_KEY_SIZE = 256
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 16

    fun encrypt(strToEncrypt: String, passphrase: String): ByteArray {
        val plainText = strToEncrypt.toByteArray(Charsets.UTF_8)

        val key = getSecretKey(passphrase, generateSalt())
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)

        val iv = ByteArray(IV_LENGTH)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val cipherText = cipher.doFinal(plainText)

        val sb = StringBuilder()
        for (b in cipherText) {
            sb.append(b.toInt().toChar())
        }

        Timber.d("iv: ${NumberUtil.bytesToHexStr(cipher.iv)} - ${cipher.iv.contentToString()}")
        Timber.d("string to  encrypt: $strToEncrypt")
        Timber.d("encrypted: $sb")

        return cipherText
    }

    fun decrypt(dataToDecrypt: String, passphrase: String): ByteArray {

        val toBytes = Base64.decode(dataToDecrypt, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        //val ivSpec = IvParameterSpec(getSavedInitializationVector(context))
        val secretKey = getSecretKey(passphrase, generateSalt())
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ByteArray(IV_LENGTH)))
        val cipherText = cipher.doFinal(toBytes)

        val sb = StringBuilder()
        for (b in cipherText) {
            sb.append(b.toInt().toChar())
        }
        Timber.d("aes decrypted: $sb")

        return cipherText
    }

    fun encryptGcm(plaintext: ByteArray?, passphrase: String): ByteArray {

        val keyBytes = ByteArray(32)
        System.arraycopy(passphrase.toByteArray(), 0, keyBytes, 0, passphrase.toByteArray().size)

        val key = SecretKeySpec(keyBytes, "AES")
        // Get Cipher Instance
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")

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
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")

        // Create GCMParameterSpec
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

        // Initialize Cipher for DECRYPT_MODE
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)

        try {
            // Perform Decryption
            val decryptedText = cipher.doFinal(cipherText)
            return NumberUtil.bytesToHexStr(decryptedText)
        }catch (e: javax.crypto.AEADBadTagException){
            return "invalid passphrase"
        }
    }

    fun decryptGcmWithPrefixIv(encryptedText: ByteArray, passphrase: String): String {
        val bb = ByteBuffer.wrap(encryptedText)
        val iv = ByteArray(GCM_IV_LENGTH)

        bb[iv]
        val cipherText = ByteArray(bb.remaining())
        bb[cipherText]

        return decryptGcm(cipherText, passphrase)
    }

    fun encryptX(strToEncrypt: String, secret_key: String): String {

        Security.addProvider(BouncyCastleProvider())

        val keyBytes = ByteArray(32)
        System.arraycopy(secret_key.toByteArray(charset("UTF8")), 0, keyBytes, 0, secret_key.toByteArray(charset("UTF8")).size)

        val secretkey = SecretKeySpec(keyBytes, "AES")
            val input = strToEncrypt.toByteArray(charset("UTF8"))

            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
                cipher.init(Cipher.ENCRYPT_MODE, secretkey)

                val cipherText = ByteArray(cipher.getOutputSize(input.size))
                var ctLength = cipher.update(
                        input, 0, input.size,
                        cipherText, 0
                )
                ctLength += cipher.doFinal(cipherText, ctLength)
                return NumberUtil.bytesToHexStr(cipherText)
            }
    }

    private fun saveSecretKey(context:Context, secretKey: SecretKey) {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(secretKey)
        val strToSave = String(android.util.Base64.encode(baos.toByteArray(), android.util.Base64.DEFAULT))
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putString("secret_key", strToSave)
        editor.apply()
    }

    private fun getSavedSecretKey(context: Context): SecretKey {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val strSecretKey = sharedPref.getString("secret_key", "")
        val bytes = android.util.Base64.decode(strSecretKey, android.util.Base64.DEFAULT)
        val ois = ObjectInputStream(ByteArrayInputStream(bytes))
        val secretKey = ois.readObject() as SecretKey
        return secretKey
    }

    private fun saveInitializationVector(context: Context, initializationVector: ByteArray) {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(initializationVector)
        val strToSave = String(android.util.Base64.encode(baos.toByteArray(), android.util.Base64.DEFAULT))
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putString("initialization_vector", strToSave)
        editor.apply()
    }

    private fun getSavedInitializationVector(context: Context) : ByteArray {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val strInitializationVector = sharedPref.getString("initialization_vector", "")
        val bytes = android.util.Base64.decode(strInitializationVector, android.util.Base64.DEFAULT)
        val ois = ObjectInputStream(ByteArrayInputStream(bytes))
        val initializationVector = ois.readObject() as ByteArray
        return initializationVector
    }

    @Throws(Exception::class)
    fun getSecretKey(passphrase: String, salt: String?): SecretKey? {
        return try {
            val pbeKeySpec =
                    PBEKeySpec(passphrase.toCharArray(), hexStringToByteArray(salt!!), PBE_ITERATION_COUNT, 256)
            val factory: SecretKeyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER)
            val tmp: SecretKey = factory.generateSecret(pbeKeySpec)
            SecretKeySpec(tmp.encoded, SECRET_KEY_ALGORITHM)
        } catch (e: Exception) {
            throw Exception("Unable to get secret key", e)
        }
    }

    @Throws(java.lang.Exception::class)
    fun generateSalt(): String? {
        return try {
            //val random: SecureRandom = SecureRandom.getInstance(RANDOM_ALGORITHM)
            val salt = ByteArray(SALT_LENGTH)
            //random.nextBytes(salt)
            NumberUtil.bytesToHexStr(salt)
        } catch (e: java.lang.Exception) {
            throw java.lang.Exception("Unable to generate salt", e)
        }
    }
}
