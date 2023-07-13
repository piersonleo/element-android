//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.database

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vcard.mesh.sdk.MeshConstants
import com.vcard.mesh.sdk.account.data.BatchAccountData
import com.vcard.mesh.sdk.account.data.MeshAccountData
import com.vcard.mesh.sdk.address.MeshAddressUtil
import com.vcard.mesh.sdk.database.entity.AccountEntity
import com.vcard.mesh.sdk.database.entity.AccountMutxoEntity
import com.vcard.mesh.sdk.database.entity.NodeEntity
import com.vcard.mesh.sdk.mutxo.data.MutxoData
import com.vcard.mesh.sdk.transaction.data.NodeData
import io.realm.Realm
import timber.log.Timber

class MeshRealm {

    fun getAccountByAddress(address: String): AccountEntity?{
        var accountEntity: AccountEntity? = null
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            val accountEntityCopy = r
                    .where(AccountEntity::class.java)
                    .equalTo("address", address)
                    .findFirst()

            if (accountEntityCopy != null) accountEntity = r.copyFromRealm(accountEntityCopy)
        }}

        return accountEntity
    }

    fun getAccountsList(): List<AccountEntity> {

        var accountEntities: List<AccountEntity> = emptyList()
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            val accountEntitiesCopy =  r
                    .where(AccountEntity::class.java)
                    .sort("name")
                    .findAll()

            if (accountEntitiesCopy != null) accountEntities = r.copyFromRealm(accountEntitiesCopy)
        }}

        return accountEntities
    }

    fun getAccountsForBatchSave(): List<AccountEntity> {

        var accountEntities: List<AccountEntity> = emptyList()
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            val accountEntitiesCopy =  r
                    .where(AccountEntity::class.java)
                    .sort("name")
                    .notEqualTo("type", "test")
                    .findAll()

            if (accountEntitiesCopy != null) accountEntities = r.copyFromRealm(accountEntitiesCopy)
        }}

        return accountEntities
    }

    fun getAccountMutxoByAddress(address: String): List<AccountMutxoEntity>{
        var accountEntities: List<AccountMutxoEntity> = emptyList()
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            val accountEntitiesCopy =  r
                    .where(AccountMutxoEntity::class.java)
                    .sort("amount")
                    .equalTo("fullAddress", address)
                    .findAll()

            if (accountEntitiesCopy != null) accountEntities = r.copyFromRealm(accountEntitiesCopy)
        }}

        return accountEntities
    }

    fun addUpdateAccount(account: AccountEntity){
        try {
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                var accountEntity = r.where(AccountEntity::class.java).equalTo("address", account.address).findFirst()
                if (accountEntity == null) accountEntity = r.createObject(AccountEntity::class.java, account.address)

                if (accountEntity != null) {
                    accountEntity.name = account.name
                    accountEntity.type = account.type
                    accountEntity.encryptedKey = account.encryptedKey
                    accountEntity.encryptedJson = account.encryptedJson
                    accountEntity.privateKey = account.privateKey
                    accountEntity.balance = account.balance
                    accountEntity.currency = account.currency
                }
            }}
        } catch (e: Exception) {
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addUpdateAccountBalance(account: AccountEntity){
        try {
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                var accountEntity = r.where(AccountEntity::class.java).equalTo("address", account.address).findFirst()
                if (accountEntity == null) accountEntity = r.createObject(AccountEntity::class.java, account.address)

                if (accountEntity != null) {
                    accountEntity.balance = account.balance
                    accountEntity.currency = account.currency
                }
            }}
        } catch (e: Exception) {
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addUpdateAccountName(account: AccountEntity){
        try {
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                var accountEntity = r.where(AccountEntity::class.java).equalTo("address", account.address).findFirst()
                if (accountEntity == null) accountEntity = r.createObject(AccountEntity::class.java, account.address)

                if (accountEntity != null) {
                    accountEntity.name = account.name
                }
            }}
        } catch (e: Exception) {
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addUpdateAccountEk(account: AccountEntity){
        try {
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                var accountEntity = r.where(AccountEntity::class.java).equalTo("address", account.address).findFirst()
                if (accountEntity == null) accountEntity = r.createObject(AccountEntity::class.java, account.address)

                if (accountEntity != null) {
                    accountEntity.encryptedKey = account.encryptedKey
                }
            }}
        } catch (e: Exception) {
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addAccountsFromJson(accounts: String){
        try{
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                val accountEntity = r.where(AccountEntity::class.java).findFirst()

                if (accountEntity == null) {
                    val accountsArray = Gson().fromJson(accounts, Array<MeshAccountData>::class.java)
                    val accountJson = Gson().toJson(accountsArray)
                    Timber.d("accountsJson: $accountJson")

                    r
                            .createOrUpdateAllFromJson(AccountEntity::class.java, accountJson)
                }
            }}
        }catch (e: java.lang.Exception){
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addBatchAccountsFromArray(accounts: Array<BatchAccountData>){
        try{
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
                val accountJson = Gson().toJson(accounts)
                val toAccountData = Gson().fromJson(accountJson, Array<MeshAccountData>::class.java)
                val toAccountJson = Gson().toJson(toAccountData)
                Timber.d("accountsJson: $toAccountJson")

                r
                        .createOrUpdateAllFromJson(AccountEntity::class.java, toAccountJson)
            }}
        }catch (e: java.lang.Exception){
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun addAccountMutxoFromMap(address: String, map: Map<String, MutxoData>){
        map.map {
            it.value.fullAddress = MeshAddressUtil.createFullAddress(it.value.ownerAddress.prefix, it.value.ownerAddress.address, it.value.ownerAddress.checksum)
        }

        //map.map { it.value.mutxoKey = CommonUtil().base64StringToHex(it.value.mutxoKey) }
        //map.map { it.value.mutxoKeyBytes = it.value.mutxoKey }
        //map.map { it.value.source = CommonUtil().base64StringToHex(it.value.source) }
        //map.map { it.value.sourceByteArray = it.value.source }
        val gson = GsonBuilder().disableHtmlEscaping().create()

        val mapValues = map.values
        val mapJson = gson.toJson(mapValues.toList())
        val mutxoValues = gson.fromJson(mapJson, Array<MutxoData>::class.java)
        val mutxoJson = gson.toJson(mutxoValues)
        Timber.d("mutxoJson: $mutxoJson")

        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction { r ->

                //clear address' mutxo before insert
                val accountMutxo = r
                        .where(AccountMutxoEntity::class.java)
                        .equalTo("fullAddress", address)
                        .findAll()

                accountMutxo.deleteAllFromRealm()

                r
                        .createOrUpdateAllFromJson(AccountMutxoEntity::class.java, mutxoJson)
            }
        }
    }

    fun addAccountMutxoFromMapManual(address: String, map: Map<String, MutxoData>){
        map.map {
            it.value.fullAddress = MeshAddressUtil.createFullAddress(it.value.ownerAddress.prefix, it.value.ownerAddress.address, it.value.ownerAddress.checksum)
        }

        val mapValues = map.values

        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction { r ->

                //clear address mutxo before insert
                val accountMutxo = r
                        .where(AccountMutxoEntity::class.java)
                        .equalTo("fullAddress", address)
                        .findAll()

                accountMutxo.deleteAllFromRealm()

                for (mapValue in mapValues){
                    var accountEntity = r.where(AccountMutxoEntity::class.java).equalTo("mutxoKey", mapValue.mutxoKey).findFirst()
                    if (accountEntity == null) accountEntity = r.createObject(AccountMutxoEntity::class.java, mapValue.mutxoKey)

                    if (accountEntity != null){
                        accountEntity.fullAddress = mapValue.fullAddress
                        accountEntity.currency = mapValue.currency
                        accountEntity.amount = mapValue.amount
                        accountEntity.reference = mapValue.reference
                        accountEntity.source = mapValue.source
                        accountEntity.sourceType = mapValue.sourceType
                    }
                }
            }
        }
    }

    fun addNodesFromJson(nodes: String){
        try{
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                val nodeEntity = r.where(NodeEntity::class.java).findFirst()

                if (nodeEntity == null) {
                    val nodesArray = Gson().fromJson(nodes, Array<NodeData>::class.java)
                    val nodesJson = Gson().toJson(nodesArray)
                    Timber.d("nodesJson: $nodesJson")

                    r
                            .createOrUpdateAllFromJson(NodeEntity::class.java, nodesJson)
                }
            }}
        }catch (e: java.lang.Exception){
            Timber.e("Error Exception: ${e.localizedMessage}")
        }
    }

    fun getNodesForElection(excludeSender: String, excludeRecipient: String): ArrayList<NodeEntity>{
        var nodeEntities: List<NodeEntity>? = null
        val nodesList: ArrayList<NodeEntity> = ArrayList()

        try{
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                val nodeEntitiesCopy = r.where(NodeEntity::class.java)
                        .notEqualTo("id", excludeSender)
                        .notEqualTo("id", excludeRecipient)
                        .findAll()

                if (nodeEntitiesCopy != null) nodeEntities = r.copyFromRealm(nodeEntitiesCopy)

                if (nodeEntities != null) {

                    //Shuffle int position to get unique random
                    val randomInt = ArrayList<Int>()
                    Timber.d("node to randomize: ${nodeEntities!!.size}")
                    for (i in 1..nodeEntities!!.size) randomInt.add(i)
                    randomInt.shuffle()

                    for (i in 1..MeshConstants.ElectionSize){
                        val selectedNode = nodeEntities!![randomInt[i]]

                        Timber.d("selected node[$i]: ${selectedNode.id}")
                        nodesList.add(selectedNode)
                    }
                }
            }}
        }catch (e: java.lang.Exception){
            Timber.e("Error Exception: ${e.localizedMessage}")
        }

        return nodesList
    }

    fun getNodesForElection2(excludeSender: String, excludeRecipient: String): List<NodeEntity>?{
        var nodeEntities: List<NodeEntity>? = null

        try{
            Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->

                val nodeEntitiesCopy = r.where(NodeEntity::class.java)
                        .notEqualTo("id", excludeSender)
                        .notEqualTo("id", excludeRecipient)
                        .findAll()

                if (nodeEntitiesCopy != null) nodeEntities = r.copyFromRealm(nodeEntitiesCopy)

            }}
        }catch (e: java.lang.Exception){
            Timber.e("Error Exception: ${e.localizedMessage}")
        }

        return nodeEntities
    }

    fun deleteMutxoByAddress(address: String) {
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            val accountMutxo = r
                    .where(AccountMutxoEntity::class.java)
                    .equalTo("fullAddress", address)
                    .findAll()

            accountMutxo.deleteAllFromRealm()
        }}
    }


    fun clearRealmDb() {
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            r.deleteAll()
        }}
    }
}
