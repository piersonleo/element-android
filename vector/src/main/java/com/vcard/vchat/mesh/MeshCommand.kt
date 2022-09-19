package com.vcard.vchat.mesh

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import com.vcard.vchat.mesh.Constants.MeshGoldCurrency
import com.vcard.vchat.mesh.Secp256k1.recoverPublicKey
import com.vcard.vchat.mesh.Secp256k1.signPayload
import com.vcard.vchat.mesh.data.MeshAccount
import com.vcard.vchat.mesh.data.MeshServicePayload
import com.vcard.vchat.mesh.data.MeshTransactionData
import com.vcard.vchat.mesh.data.MeshTransactionDataSerializer
import com.vcard.vchat.mesh.data.MeshTransactionElection
import com.vcard.vchat.mesh.data.MeshTransactionReceipt
import com.vcard.vchat.mesh.database.NodeEntity
import com.vcard.vchat.mesh.database.RealmExec
import io.grpc.StatusRuntimeException
import org.apache.tuweni.crypto.SECP256K1
import org.json.JSONObject
import rpc.Mesh
import rpc.MeshServiceGrpc
import timber.log.Timber
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.TreeMap

object MeshCommand {

    fun getAccount(address: String, unencryptedKey: String): MeshServicePayload {
        val channel = GrpcUtils.getChannel()

        val message = Mesh.MeshMessage.newBuilder()

        message.sourceId = address
        //message.sourceId = "m14075abe6d0722614b2a45017443f696dd339c9bf1a4e1909"
        message.network = "meshtest"

        val privateKey = BigInteger(unencryptedKey, 16)

        //val privateKey = BigInteger("2354161917356255589292793974672092891942487462967551788533302831359367298795")

        //val senderAddress = "m1ca040703ee3c8fe542611412191e84b209fc0ef5ff3316dc"
        //val receiverAddress = "m17b1f1178662c449d9f2290c7911da4b4bfc4f89d904b2d9e"

        val account = MeshAccount(
                address,
                "au79"
        )

        val dataBytesJson = Gson().toJson(account)

        val payload = MeshServicePayload(Constants.ServiceCommandGetAccount, dataBytesJson.toByteArray())

        val payloadJson = Gson().toJson(payload)

        Timber.d("payloadJson: $payloadJson")

        val payloadBytes = payloadJson.toByteArray()

        //hash is done in the signPayload function
        val signedPayload = signPayload(privateKey, payloadBytes)
        Timber.d("signedPayload: ${signedPayload.contentToString()}")

        message.payload = ByteString.copyFrom(payloadBytes)
        message.signature = ByteString.copyFrom(signedPayload)

        val txnRequest = Mesh.MeshRequest.newBuilder()

        txnRequest.requestMessage = message.build()

        //try to recover public key and create address
        val keyPair = SECP256K1.KeyPair.fromSecretKey(
                SECP256K1.SecretKey.fromInteger(
                        privateKey
                )
        )


        val hashMessage = HashUtils.meshHash(payloadBytes)

        val signature = SECP256K1.signHashed(hashMessage, keyPair)

        Timber.d("hashMessage: ${hashMessage.contentToString()}")
        val pubFromSecret = SECP256K1.PublicKey.fromSecretKey(SECP256K1.SecretKey.fromInteger(privateKey)).bytesArray()
        Timber.d("publicKeyBytes: ${pubFromSecret.contentToString()}\nsize: ${pubFromSecret.size}")

        val recoverPublicKey = recoverPublicKey(hashMessage, signature)

        val addressData = Address.computeAddressHash(recoverPublicKey!!)
        val addressFromPublic = Address.setAddress(PrefixEnum.DefaultPrefix, addressData)

        val fullAddress = "${addressFromPublic.prefix}${NumberUtil.bytesToHexStr(addressFromPublic.addressData)}${NumberUtil.bytesToHexStr(addressFromPublic.addressCheckSum)}"
        Timber.d("source: ${message.sourceId}\naddressFromPublic: $fullAddress")

        try {
            val stub = MeshServiceGrpc.newBlockingStub(channel)
            val response = stub.call(txnRequest.build())

            val decodedPayloadResp = response.payload.toByteArray().decodeToString()
            val parseJson = JSONObject(decodedPayloadResp)
            val responseParams = parseJson.getString("d")
            val responseParamsToBytes =responseParams.toByteArray()
            val responsePayload = MeshServicePayload(parseJson.getString("c"), responseParamsToBytes)
            channel?.shutdown()

            return responsePayload
        } catch (e: StatusRuntimeException) {
            Timber.d("error grpc: $e")
            channel?.shutdown()
            return if (e.status.code.equals(io.grpc.Status.UNAVAILABLE.code)) {
                MeshServicePayload("unavailable", byteArrayOf())
            }else{
                MeshServicePayload("fail", byteArrayOf())
            }
        }
    }

    fun sendTransaction(senderAddress: String, senderPrivateKey: String, recipientAddress: String, amount: Long, reference: String, nonce: Long): MeshServicePayload{
        val channel = GrpcUtils.getChannel()

        val message = Mesh.MeshMessage.newBuilder()

        message.sourceId = senderAddress
        //message.sourceId = "m14075abe6d0722614b2a45017443f696dd339c9bf1a4e1909"
        message.network = "meshtest"

        //val privateKey = BigInteger("2354161917356255589292793974672092891942487462967551788533302831359367298795")
        val privateKey = BigInteger(senderPrivateKey, 16)
        Timber.d("sender private: $privateKey")

        val transactionReceipt = getTransactionReceipt(senderAddress, senderPrivateKey, recipientAddress, amount, reference, nonce)

        val transactionReceiptJson = Gson().toJson(transactionReceipt)

        val transactionReceiptBytes = transactionReceiptJson.toByteArray()

        val payload = MeshServicePayload("txn", transactionReceiptBytes)

        val payloadJson = Gson().toJson(payload)

        val payloadBytes = payloadJson.toByteArray()

        //hash is done in the signPayload function
        val signedPayload = signPayload(privateKey, payloadBytes)

        Timber.d("signedPayload:  ${signedPayload.contentToString()}")
        message.payload = ByteString.copyFrom(payloadBytes)
        message.signature = ByteString.copyFrom(signedPayload)

        val txnRequest = Mesh.MeshRequest.newBuilder()

        txnRequest.requestMessage = message.build()

        try {
            val stub = MeshServiceGrpc.newBlockingStub(channel)
            val response = stub.call(txnRequest.build())

            Timber.d("response: $response")

            val decodedPayloadResp = response.payload.toByteArray().decodeToString()
            val parseJson = JSONObject(decodedPayloadResp)
            val responseParams = parseJson.getString("d")
            val responseParamsToBytes =responseParams.toByteArray()
            val responsePayload = MeshServicePayload(parseJson.getString("c"), responseParamsToBytes)
            val decodeParams = Base64.decode(responseParams, Base64.DEFAULT)
            val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

            Timber.d("decodeParams: $decodeParamsUtf8")

            channel?.shutdown()

            return responsePayload
        } catch (e: StatusRuntimeException) {
            channel?.shutdown()
            return if (e.status.code.equals(io.grpc.Status.UNAVAILABLE.code)) {
                MeshServicePayload("unavailable", byteArrayOf())
            }else{
                MeshServicePayload("fail", byteArrayOf())
            }
        }
    }

    private fun getTransactionReceipt(senderAddress: String, senderPrivateKey: String, recipientAddress: String, amount: Long, reference: String, nonce: Long): MeshTransactionReceipt {
        val privateKeyBigInt = BigInteger(senderPrivateKey, 16)
        Timber.d("sender private string: $senderPrivateKey\nsender private big int: $privateKeyBigInt")

        val totalFee = TxnFee.calculateTotalFee(CurrencyEnum.MeshGold, amount)
        Timber.d("totalFee: $totalFee")

        val txd = MeshTransactionData(
                senderAddress,
                MeshGoldCurrency,
                nonce+1,
                1,
                recipientAddress,
                amount,
                totalFee,
                reference
        )

        val txdGson = GsonBuilder().registerTypeAdapter(MeshTransactionData::class.java, MeshTransactionDataSerializer()).create()
        val txdJson = txdGson.toJson(txd)
        Timber.d("txdJson: $txdJson")

        val txdBytes = txdJson.toString().toByteArray()

        val signedTxd = signPayload(privateKeyBigInt, txdBytes)

        Timber.d("signedTxd: ${signedTxd.contentToString()}")

//        val signedTxdUint =  ArrayList<UInt>()
//        for (byte in signedTxd){
//            val  uInt = byte.toUInt()
//            signedTxdUint.add(uInt)
//        }

        val unsignedIntTxd = IntArray(signedTxd.size)

        for ((index, byte) in signedTxd.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntTxd[index] = unsignedInt
        }

        Timber.d("unsigned: ${unsignedIntTxd.contentToString()}")

//        val nodeAddress = "m1ff42d265bc16bdd514f5e74e7393495f3311185d3c52d1b9"
//        val nodeKey = BigInteger("34b318c9b3e80df98298b10f72ba95a6d982afde0ff2d7cde728cb74961d21cc", 16)

//        val timestamp = Utils.getTime()

//        val txf = MeshTransactionFee(
//                Constants.MeshSystemAddress,
//                BigInteger("12"),
//                Constants.MeshCommunityAddress,
//                BigInteger("12"),
//                nodeAddress,
//                BigInteger("12"),
//                timestamp
//        )
//
//        val txfBytes = txf.toString().toByteArray()
//
//        val signedTxf = signPayload(nodeKey, txfBytes)

        //val candidateList = RealmExec().getNodesForElection(senderAddress, recipientAddress)
        val getNodes = RealmExec().getNodesForElection2(senderAddress, recipientAddress)
        val candidateList: ArrayList<NodeEntity> = ArrayList()

        if (getNodes != null) {

            //Shuffle int position to get unique random
            val randomInt = ArrayList<Int>()
            Timber.d("node to randomize: ${getNodes.size}")
            for (i in 1..getNodes.size) randomInt.add(i)
            randomInt.shuffle()

            for (i in 1..Constants.ElectionSize){
                val selectedNode = getNodes[randomInt[i]]

                Timber.d("selected node[$i]: ${selectedNode.id}")
                candidateList.add(selectedNode)
            }
        }

        val candidateMap = HashMap<String, String>()
        for (candidate in candidateList){
            candidateMap[candidate.id] = "${candidate.ipAddress}:${candidate.port}"
        }


        Timber.d("candidateMap: $candidateMap")

        //need to create sorted jso
        val candidateJson = Gson().toJson(candidateMap)
        val map = Gson().fromJson(candidateJson, TreeMap::class.java)
        val sortedJson: String = Gson().toJson(map)
        Timber.d("candidateJson: $sortedJson")

        val candidateBytes = sortedJson.toByteArray()
        //val signedCandidates = Secp256k1.signCandidateList(privateKeyBigInt, txdBytes, candidateBytes)
        val signedCandidates = signPayload(privateKeyBigInt, candidateBytes)

        val testHash = HashUtils.meshHash(candidateBytes)
        Timber.d("testHash: ${testHash.contentToString()}")

        val unsignedIntCandidates = IntArray(signedCandidates.size)
        for ((index, byte) in signedCandidates.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntCandidates[index] = unsignedInt
        }

        Timber.d("signedCandidates: ${signedCandidates.contentToString()}")

        val election = MeshTransactionElection(
                candidateMap,
                unsignedIntCandidates,
                emptyMap(),
                byteArrayOf()
        )
        val electionJson = Gson().toJson(election)

        val electionBytes = electionJson.toString().toByteArray()

        val unsignedIntElection = IntArray(electionBytes.size)

        for ((index, byte) in electionBytes.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntElection[index] = unsignedInt
        }

        return MeshTransactionReceipt(
                1,
                txdBytes,
                unsignedIntTxd,
                byteArrayOf(),
                byteArrayOf(),
                unsignedIntElection,
                byteArrayOf()
        )
    }

    private fun toUnsigned(b: Byte): Int {
        return (if (b >= 0) b else 256 + b).toInt()
    }

}
