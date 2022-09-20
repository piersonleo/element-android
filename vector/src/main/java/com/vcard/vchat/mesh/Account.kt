package com.vcard.vchat.mesh

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vcard.vchat.mesh.data.EncryptedKeyData
import com.vcard.vchat.mesh.data.EncryptedKeyDataSerializer
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import org.apache.tuweni.crypto.SECP256K1
import timber.log.Timber
import java.io.ByteArrayOutputStream

object Account {

    fun generateAccount(passphrase: String, accountName: String): String{

        val newKey = SECP256K1.KeyPair.random()

        val addressData = Address.computeAddressHash(newKey.publicKey())
        val addressFromPublic = Address.setAddress(PrefixEnum.DefaultPrefix, addressData)

        val address = "${addressFromPublic.prefix}${NumberUtil.bytesToHexStr(addressFromPublic.addressData)}${NumberUtil.bytesToHexStr(addressFromPublic.addressCheckSum)}"
        val isValid = Address.isValidMeshAddressString(address)
        Timber.d("address: $address isValid: $isValid pass: $passphrase")

        var encryptedKeyJson = ""
        if (isValid){
            val encryptedKey = Aes256.encryptGcm(newKey.secretKey().bytesArray(), passphrase)
            val encryptedKeyString = NumberUtil.bytesToHexStr(encryptedKey)
            val accountEntity = AccountEntity()
            accountEntity.name = accountName
            accountEntity.address = address
            accountEntity.encryptedKey = encryptedKeyString
            RealmExec().addUpdateAccount(accountEntity)

            val encryptedKeyData = EncryptedKeyData()
            encryptedKeyData.fullAddress = address
            encryptedKeyData.prefix = addressFromPublic.prefix
            encryptedKeyData.address = NumberUtil.bytesToHexStr(addressFromPublic.addressData)
            encryptedKeyData.checksum = NumberUtil.bytesToHexStr(addressFromPublic.addressCheckSum)
            encryptedKeyData.encryptedKey.cipher = Constants.DefaultKeystoreCipher
            encryptedKeyData.encryptedKey.encryptedText = encryptedKeyString
            encryptedKeyData.encryptedKey.keyChecksum = NumberUtil.bytesToHexStr(HashUtils.computeChecksum(encryptedKey))

            val gson = GsonBuilder().registerTypeAdapter(EncryptedKeyData::class.java, EncryptedKeyDataSerializer()).setPrettyPrinting().create()
            encryptedKeyJson = gson.toJson(encryptedKeyData)

            //val testDecrypt = NumberUtil.hexStrToBytes("e128c5c9e70537dc69835df0c10a79182435fb11dc89e07ae94f186b86cf6c2005c9717c884ac12df38788cef54107dd2630c85444995c61e58d9b84")
            Timber.d("new key: ${NumberUtil.bytesToHexStr(newKey.secretKey().bytesArray())}")
            Timber.d("decrypt: ${Aes256.decryptGcm(encryptedKey, passphrase)}")
            Timber.d("encryptedKey: $encryptedKeyString")
            Timber.d("key json: $encryptedKeyJson")
        }

        return encryptedKeyJson
    }

    fun addAccountFromEncryptedKeyData(name: String, encryptedKeyData: EncryptedKeyData){
        val accountEntity = AccountEntity()
        accountEntity.name = name
        accountEntity.address = encryptedKeyData.fullAddress
        accountEntity.encryptedKey = encryptedKeyData.encryptedKey.encryptedText
        RealmExec().addUpdateAccount(accountEntity)
    }

//    fun generateEncryptedKeyFile(newAddress: MeshAddress, newPrivateKey: SECP256K1.SecretKey, passphrase: String){
//
//    }
//
//    private fun createEncryptedKeyFileData(address: MeshAddress, privateKey: SECP256K1.SecretKey, cipher: String, passphrase: ByteArray){
//
//    }
}
