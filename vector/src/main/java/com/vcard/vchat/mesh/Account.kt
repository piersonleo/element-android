package com.vcard.vchat.mesh

import com.google.gson.GsonBuilder
import com.vcard.vchat.mesh.data.BatchAccountData
import com.vcard.vchat.mesh.data.EncryptedKeyData
import com.vcard.vchat.mesh.data.EncryptedKeyDataSerializer
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import org.apache.tuweni.crypto.SECP256K1
import timber.log.Timber

object Account {

    fun generateAccount(passphrase: String, accountName: String): String{

        val newKey = SECP256K1.KeyPair.random()

        val addressData = Address.computeAddressHash(newKey.publicKey())
        val addressFromPublic = Address.setAddress(PrefixEnum.DefaultPrefix, addressData)

        val address = "${addressFromPublic.p}${NumberUtil.bytesToHexStr(addressFromPublic.a)}${NumberUtil.bytesToHexStr(addressFromPublic.c)}"
        val isValid = Address.isValidMeshAddressString(address)

        var ekJson = ""
        if (isValid){
            val ek = Aes256.encryptGcm(newKey.secretKey().bytesArray(), passphrase)
            val ekString = NumberUtil.bytesToHexStr(ek)

            val ekData = EncryptedKeyData()
            ekData.fullAddress = address
            ekData.prefix = addressFromPublic.p
            ekData.address = NumberUtil.bytesToHexStr(addressFromPublic.a)
            ekData.checksum = NumberUtil.bytesToHexStr(addressFromPublic.c)
            ekData.encryptedKey.cipher = Constants.DefaultKeystoreCipher
            ekData.encryptedKey.encryptedText = ekString
            ekData.encryptedKey.keyChecksum = NumberUtil.bytesToHexStr(HashUtils.computeChecksum(ek))

            val gson = GsonBuilder().registerTypeAdapter(EncryptedKeyData::class.java, EncryptedKeyDataSerializer()).setPrettyPrinting().create()
            ekJson = gson.toJson(ekData)
            val eJson = encryptJsonString(passphrase, ekJson)
            val accountEntity = AccountEntity()
            accountEntity.name = accountName
            accountEntity.address = address
            accountEntity.encryptedKey = ekString
            accountEntity.encryptedJson = eJson
            RealmExec().addUpdateAccount(accountEntity)

            //val testDecrypt = NumberUtil.hexStrToBytes("e128c5c9e70537dc69835df0c10a79182435fb11dc89e07ae94f186b86cf6c2005c9717c884ac12df38788cef54107dd2630c85444995c61e58d9b84")
//            Timber.d("new key: ${NumberUtil.bytesToHexStr(newKey.secretKey().bytesArray())}")
//            Timber.d("decrypt: ${Aes256.decryptGcm(encryptedKey, passphrase)}")
//            Timber.d("encryptedKey: $encryptedKeyString")
//            Timber.d("key json: $encryptedKeyJson")
        }

        return ekJson
    }

    fun generateAccountEkJson(address: String, ek: String): String{
        val isValid = Address.isValidMeshAddressString(address)

        val ekJson: String
        if (isValid){
            val meshAddress = Address.getMeshAddressFromString(address)

            val ekData = EncryptedKeyData()
            ekData.fullAddress = address
            ekData.prefix = meshAddress.p
            ekData.address = NumberUtil.bytesToHexStr(meshAddress.a)
            ekData.checksum = NumberUtil.bytesToHexStr(meshAddress.c)
            ekData.encryptedKey.cipher = Constants.DefaultKeystoreCipher
            ekData.encryptedKey.encryptedText = ek
            ekData.encryptedKey.keyChecksum = NumberUtil.bytesToHexStr(HashUtils.computeChecksum(NumberUtil.hexStrToBytes(ek)))

            val gson = GsonBuilder().registerTypeAdapter(EncryptedKeyData::class.java, EncryptedKeyDataSerializer()).setPrettyPrinting().create()
            ekJson = gson.toJson(ekData)
        }else{
            throw Exception("invalid address")
        }

        return ekJson
    }

    fun changeAccountPp(address:String, ek: String, pp: String, newPp: String): String{

        val isValid = Address.isValidMeshAddressString(address)

        if (isValid) {
            val kBytes = NumberUtil.hexStrToBytes(ek)
            val dk = Aes256.decryptGcm(kBytes, pp)

            if (dk == "invalid passphrase") {
                throw Exception("invalid passphrase")
            } else {
                val newEk = Aes256.encryptGcm(NumberUtil.hexStrToBytes(dk), newPp)
                val accountEntity = AccountEntity()
                accountEntity.address = address
                accountEntity.encryptedKey = NumberUtil.bytesToHexStr(newEk)

                Thread {
                    RealmExec().addUpdateAccountEk(accountEntity)
                }.start()

                return accountEntity.encryptedKey
            }
        }else{
            throw Exception("invalid address")
        }
    }

    fun addAccountFromEncryptedKeyData(name: String, encryptedKeyData: EncryptedKeyData){
        val accountEntity = AccountEntity()
        accountEntity.name = name
        accountEntity.address = encryptedKeyData.fullAddress
        accountEntity.encryptedKey = encryptedKeyData.encryptedKey.encryptedText
        RealmExec().addUpdateAccount(accountEntity)
    }

    fun addAccountFromBatchAccountData(batchAccountData: BatchAccountData){
        val accountEntity = AccountEntity()
        accountEntity.name = batchAccountData.name
        accountEntity.address = batchAccountData.address
        accountEntity.encryptedKey = batchAccountData.encryptedKey
        RealmExec().addUpdateAccount(accountEntity)
    }

    fun generateEncryptedKeyFromAccountEntity(accountEntity: AccountEntity): BatchAccountData{
        val isValid = Address.isValidMeshAddressString(accountEntity.address)
        val batchAccountData = BatchAccountData()

        if (isValid) {
            val encryptedKey = accountEntity.encryptedKey

            batchAccountData.name = accountEntity.name
            batchAccountData.address = accountEntity.address
            batchAccountData.encryptedKey = encryptedKey
        }

        return batchAccountData
    }

    private fun encryptJsonString(passphrase: String, jsonString: String): String{
        val encryptedJsonString = Aes256.encryptGcm(jsonString.toByteArray(), passphrase)

        return NumberUtil.bytesToHexStr(encryptedJsonString)
    }

//    fun generateEncryptedKeyFile(newAddress: MeshAddress, newPrivateKey: SECP256K1.SecretKey, passphrase: String){
//
//    }
//
//    private fun createEncryptedKeyFileData(address: MeshAddress, privateKey: SECP256K1.SecretKey, cipher: String, passphrase: ByteArray){
//
//    }
}
