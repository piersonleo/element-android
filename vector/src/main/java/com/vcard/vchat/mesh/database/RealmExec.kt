package com.vcard.vchat.mesh.database

import com.google.gson.Gson
import com.vcard.vchat.mesh.Constants
import com.vcard.vchat.mesh.data.MeshAccountData
import com.vcard.vchat.mesh.data.NodeData
import io.realm.Realm
import timber.log.Timber

class RealmExec {

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
                    accountEntity.privateKey = account.privateKey
                    accountEntity.balance = account.balance
                    accountEntity.currency = account.currency
                    accountEntity.moduleHash = account.moduleHash
                    accountEntity.nonce = account.nonce
                    accountEntity.moduleHash = account.moduleHash
                    accountEntity.rootHash = account.rootHash
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
                    accountEntity.moduleHash = account.moduleHash
                    accountEntity.nonce = account.nonce
                    accountEntity.moduleHash = account.moduleHash
                    accountEntity.rootHash = account.rootHash
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

                    for (i in 1..Constants.ElectionSize){
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

    fun clearRealm() {
        Realm.getDefaultInstance().use { realm -> realm.executeTransaction { r ->
            r.deleteAll()
        }}
    }
}
