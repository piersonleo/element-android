//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.account

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vcard.mesh.sdk.MeshConstants
import com.vcard.mesh.sdk.account.data.BatchAccountData
import com.vcard.mesh.sdk.account.data.EncryptedAccountData
import com.vcard.mesh.sdk.account.data.EncryptedAccountDataSerializer
import com.vcard.mesh.sdk.address.MeshAddressUtil
import com.vcard.mesh.sdk.address.PrefixEnum
import com.vcard.mesh.sdk.crypto.Aes256
import com.vcard.mesh.sdk.database.MeshRealm
import com.vcard.mesh.sdk.database.entity.AccountEntity
import com.vcard.mesh.sdk.utils.CommonUtil
import com.vcard.mesh.sdk.utils.HashUtil
import com.vcard.mesh.sdk.utils.NumberUtil
import org.apache.tuweni.crypto.SECP256K1

object Account {

    /**
     * Generate a mesh account. Returns the json string of the account
     * @param pp The passphrase of the new account
     * @param accountName Name of the account
     */
    fun generateMeshAccount(pp: String, accountName: String): String{

        val newKey = SECP256K1.KeyPair.random()

        val addressData = MeshAddressUtil.computeAddressHash(newKey.publicKey())
        val addressFromPublic = MeshAddressUtil.setAddress(PrefixEnum.DefaultPrefix, addressData)

        val address = "${addressFromPublic.p}${NumberUtil.bytesToHexStr(addressFromPublic.a)}${NumberUtil.bytesToHexStr(addressFromPublic.c)}"
        val isValid = MeshAddressUtil.isValidMeshAddressString(address)

        var ekJson = ""
        if (isValid){
            val ek = Aes256.encryptGcm(newKey.secretKey().bytesArray(), pp)
            val ekString = NumberUtil.bytesToHexStr(ek)

            val ekData = EncryptedAccountData()
            ekData.fullAddress = address
            ekData.prefix = addressFromPublic.p
            ekData.address = NumberUtil.bytesToHexStr(addressFromPublic.a)
            ekData.checksum = NumberUtil.bytesToHexStr(addressFromPublic.c)
            ekData.encryptedKey.cipher = MeshConstants.DefaultKeystoreCipher
            ekData.encryptedKey.encryptedText = ekString
            ekData.encryptedKey.keyChecksum = NumberUtil.bytesToHexStr(HashUtil.computeChecksum(ek))

            val gson = GsonBuilder().registerTypeAdapter(EncryptedAccountData::class.java, EncryptedAccountDataSerializer()).setPrettyPrinting().create()
            ekJson = gson.toJson(ekData)
            val eJson = encryptPayloadString(ekJson, pp)

            //Add new account to db
            val accountEntity = AccountEntity()
            accountEntity.name = accountName
            accountEntity.address = address
            accountEntity.encryptedKey = ekString
            accountEntity.encryptedJson = eJson
            MeshRealm().addUpdateAccount(accountEntity)
        }

        return ekJson
    }

    /**
     * Generate account json from an existing account. Returns the json string of the account
     * @param address The address of the account
     * @param ek The encrypted key of the account
     */
    fun generateAccountEkJson(address: String, ek: String): String{
        val isValid = MeshAddressUtil.isValidMeshAddressString(address)

        val ekJson: String
        if (isValid){
            val meshAddress = MeshAddressUtil.getMeshAddressFromString(address)

            val ekData = EncryptedAccountData()
            ekData.fullAddress = address
            ekData.prefix = meshAddress.p
            ekData.address = NumberUtil.bytesToHexStr(meshAddress.a)
            ekData.checksum = NumberUtil.bytesToHexStr(meshAddress.c)
            ekData.encryptedKey.cipher = MeshConstants.DefaultKeystoreCipher
            ekData.encryptedKey.encryptedText = ek
            ekData.encryptedKey.keyChecksum = NumberUtil.bytesToHexStr(HashUtil.computeChecksum(NumberUtil.hexStrToBytes(ek)))

            val gson = GsonBuilder().registerTypeAdapter(EncryptedAccountData::class.java, EncryptedAccountDataSerializer()).setPrettyPrinting().create()
            ekJson = gson.toJson(ekData)
        }else{
            throw Exception("invalid address")
        }

        return ekJson
    }

    /**
     * Change account passphrase. New encrypted key is generated here. Returns the json string of the new key.
     * @param address The address of the account
     * @param ek The current encrypted key of the account
     * @param pp The current passphrase of the account
     * @param newPp The new passphrase of the account
     */
    fun changeAccountPp(address:String, ek: String, pp: String, newPp: String): String{

        val isValid = MeshAddressUtil.isValidMeshAddressString(address)

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
                    MeshRealm().addUpdateAccountEk(accountEntity)
                }.start()

                return accountEntity.encryptedKey
            }
        }else{
            throw Exception("invalid address")
        }
    }


    /**
     * Add account from json payload
     * @param name The name of the account
     * @param payload The account json string
     */
    fun addAccountFromJsonPayload(name: String, payload: String){
        //verify payload contains mesh identifier
        if (payload.startsWith(MeshConstants.meshEncryptedAccountQrIdentifier)) {
            val payloadJson = payload.substringAfter(MeshConstants.meshEncryptedAccountQrIdentifier)
            val isJson = CommonUtil.isValidJson(payload)

            if (!isJson) {
                throw Exception("invalid account payload")
            }else {
                val ekData = Gson().fromJson(payloadJson, EncryptedAccountData::class.java)
                addAccountFromEncryptedAccountData(name, ekData)
            }
        }else{
            throw Exception("invalid account payload")
        }
    }

    /**
     * Add account from EncryptedKeyData class
     * @param name The name of the account
     * @param ekData The encryptedKeyData
     */

    fun addAccountFromEncryptedAccountData(name: String, ekData: EncryptedAccountData){
        val accountEntity = AccountEntity()
        accountEntity.name = name
        accountEntity.address = ekData.fullAddress
        accountEntity.encryptedKey = ekData.encryptedKey.encryptedText
        MeshRealm().addUpdateAccount(accountEntity)
    }

    /**
     * Batch add accounts from json payload
     * @param payload The accounts json string
     */
    fun addBatchAccountsFromJsonPayload(payload: String) {
        //verify payload contains mesh identifier
        if (payload.startsWith(MeshConstants.meshWalletQrIdentifier)) {
            val payloadJson = payload.substringAfter(MeshConstants.meshWalletQrIdentifier)
            val isJsonArray = CommonUtil.isValidJsonArray(payload)

            //confirm if payload is in json array
            if (!isJsonArray) {
                throw Exception("invalid batch account payload")
            } else {
                val gson = GsonBuilder().create()
                val batchData = gson.fromJson(payloadJson, Array<BatchAccountData>::class.java)

                //make sure payload is complete and address is a valid mesh address
                when {
                    batchData.any { it.address == null || it.encryptedKey == null || it.name == null  } -> {
                        throw Exception("invalid batch account payload")
                    }
                    batchData.any {!MeshAddressUtil.isValidMeshAddressString(it.address!!)} -> {
                        throw Exception("invalid batch account payload")
                    }
                    else -> {
                        Thread {
                            MeshRealm().addBatchAccountsFromArray(batchData)
                        }.start()
                    }
                }
            }
        } else {
            throw Exception("invalid batch account payload")
        }
    }

    /**
     * Add account from BatchAccountData class
     * @param batchAccountData The batchAccountData
     */
    fun addAccountFromBatchAccountData(batchAccountData: BatchAccountData){
        val accountEntity = AccountEntity()
        accountEntity.name = batchAccountData.name
        accountEntity.address = batchAccountData.address
        accountEntity.encryptedKey = batchAccountData.encryptedKey
        MeshRealm().addUpdateAccount(accountEntity)
    }

    /**
     * Convert AccountEntity data into BatchAccountData class
     * @param accountEntity The AccountEntity data
     */
    fun generateBatchAccountDataFromAccountEntity(accountEntity: AccountEntity): BatchAccountData{
        val isValid = MeshAddressUtil.isValidMeshAddressString(accountEntity.address)
        val batchAccountData = BatchAccountData()

        if (isValid) {
            val encryptedKey = accountEntity.encryptedKey

            batchAccountData.name = accountEntity.name
            batchAccountData.address = accountEntity.address
            batchAccountData.encryptedKey = encryptedKey
        }else{
            throw Exception("Invalid account address")
        }

        return batchAccountData
    }

    /**
     * Encrypt payload string using Aes256
     * @param payload The string payload to encrypt
     * @param pp The passphrase used to encrypt
     */
    private fun encryptPayloadString(payload: String, pp: String): String{
        val encryptedJsonString = Aes256.encryptGcm(payload.toByteArray(), pp)

        return NumberUtil.bytesToHexStr(encryptedJsonString)
    }
}

