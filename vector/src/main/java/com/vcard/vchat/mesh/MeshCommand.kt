package com.vcard.vchat.mesh

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import com.vcard.vchat.mesh.Constants.MeshGoldCurrency
import com.vcard.vchat.mesh.Secp256k1.recoverPublicKey
import com.vcard.vchat.mesh.Secp256k1.signPayload
import com.vcard.vchat.mesh.data.MUnspentTransactionObjectDataTxd
import com.vcard.vchat.mesh.data.MUnspentTransactionObjectDataTxdSerializer
import com.vcard.vchat.mesh.data.MeshAccount
import com.vcard.vchat.mesh.data.MeshServicePayload
import com.vcard.vchat.mesh.data.MeshTransactionData
import com.vcard.vchat.mesh.data.MeshTransactionDataSerializer
import com.vcard.vchat.mesh.data.MeshTransactionElection
import com.vcard.vchat.mesh.data.MeshTransactionFee
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

    /**
     * Command to get account balance via grpc call
     * @param address The target address of the account to get the balance from
     * @param key The unencrypted key of the account
     * @return MeshServicePayload The response payload containing the command and data
     */
    fun getAccount(address: String, key: String): MeshServicePayload {
        val channel = GrpcUtils.getChannel()

        val message = Mesh.MeshMessage.newBuilder()

        message.sourceId = address
        message.network = "meshtest"

        val privateKey = BigInteger(key, 16)

        val account = MeshAccount(
                address,
                "au79"
        )

        val dataBytesJson = Gson().toJson(account)

        val payload = MeshServicePayload(Constants.ServiceCommandGetMutxoByAddress, dataBytesJson.toByteArray())

        val payloadJson = Gson().toJson(payload)

        val payloadBytes = payloadJson.toByteArray()

        //hash is done in the signPayload function
        val signedPayload = signPayload(privateKey, payloadBytes)

        message.payload = ByteString.copyFrom(payloadBytes)
        message.signature = ByteString.copyFrom(signedPayload)

        val txnRequest = Mesh.MeshRequest.newBuilder()

        txnRequest.requestMessage = message.build()

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
            channel?.shutdown()
            return if (e.status.code.equals(io.grpc.Status.UNAVAILABLE.code)) {
                MeshServicePayload("unavailable", byteArrayOf())
            }else{
                MeshServicePayload("fail", byteArrayOf())
            }
        }
    }

    /**
     * Command to send transaction from sender to recipient via grpc call
     * @param senderAddress The address of the sender
     * @param senderPrivateKey The unencrypted key of the sender
     * @param recipientAddress The address of the recipient
     * @param amount The amount to be sent
     * @param reference The comment/remark/reference inputted by the sender
     * @param time The timestamp of when this transaction occurs. Format example: 2022-10-03T09:27:59.6333756Z
     * @return MeshServicePayload The response payload containing the command and data
     */
    fun sendTransaction(senderAddress: String, senderPrivateKey: String, recipientAddress: String, amount: BigInteger, reference: String, time: String): MeshServicePayload{
        val channel = GrpcUtils.getChannel()

        val message = Mesh.MeshMessage.newBuilder()

        message.sourceId = senderAddress
        message.network = "meshtest"

        val privateKey = BigInteger(senderPrivateKey, 16)

        val transactionReceipt = getTransactionReceipt(senderAddress, senderPrivateKey, recipientAddress, amount, reference, time)

        val transactionReceiptJson = Gson().toJson(transactionReceipt)

        val transactionReceiptBytes = transactionReceiptJson.toByteArray()

        val payload = MeshServicePayload(Constants.ServiceCommandTxn, transactionReceiptBytes)

        val payloadJson = Gson().toJson(payload)

        val payloadBytes = payloadJson.toByteArray()

        //Hash is done in the signPayload function
        val signedPayload = signPayload(privateKey, payloadBytes)

        message.payload = ByteString.copyFrom(payloadBytes)
        message.signature = ByteString.copyFrom(signedPayload)

        val txnRequest = Mesh.MeshRequest.newBuilder()

        txnRequest.requestMessage = message.build()

        try {
            val stub = MeshServiceGrpc.newBlockingStub(channel)
            val response = stub.call(txnRequest.build())

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

    /**
     * Construct a transaction receipt which includes the txd, and candidates for election
     * @param senderAddress The address of the sender
     * @param senderPrivateKey The unencrypted key of the sender
     * @param recipientAddress The address of the recipient
     * @param amount The amount to be sent
     * @param reference The comment/remark/reference inputted by the sender
     * @param time The timestamp of when this transaction occurs. Format example: 2022-10-03T09:27:59.6333756Z
     * @return MeshTransactionReceipt the receipt that'll be send to the server
     */
    private fun getTransactionReceipt(senderAddress: String, senderPrivateKey: String, recipientAddress: String, amount: BigInteger, reference: String, time: String): MeshTransactionReceipt {
        val privateKeyBigInt = BigInteger(senderPrivateKey, 16)

        val totalFee = TxnFee.calculateTotalFeeBigInt(CurrencyEnum.MeshGold, amount)

        val mutxo = RealmExec().getAccountMutxoByAddress(senderAddress)

        val mutxoGson = GsonBuilder().registerTypeAdapter(MUnspentTransactionObjectDataTxd::class.java, MUnspentTransactionObjectDataTxdSerializer()).create()
        val mutxoJson = mutxoGson.toJson(mutxo)
        val listMutxo = mutxoGson.fromJson(mutxoJson, Array<MUnspentTransactionObjectDataTxd>::class.java)

        listMutxo.map { it.mutxoKeyBytes = it.mutxoKey }
        listMutxo.map { it.sourceBytes = it.source }
        listMutxo.map { it.ownerAddress = Address.getMeshAddressTxdFromString(senderAddress) }

        val txd = MeshTransactionData(
                senderAddress,
                MeshGoldCurrency,
                1,
                recipientAddress,
                amount,
                totalFee,
                reference,
                time,
                listMutxo.toList()
        )

        val txdGson = GsonBuilder().disableHtmlEscaping().registerTypeAdapter(MeshTransactionData::class.java, MeshTransactionDataSerializer()).create()
        val txdJson = txdGson.toJson(txd)

        val txdBytes = txdJson.toString().toByteArray()

        val signedTxd = signPayload(privateKeyBigInt, txdBytes)

        val unsignedIntTxd = IntArray(signedTxd.size)

        for ((index, byte) in signedTxd.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntTxd[index] = unsignedInt
        }

        //val candidateList = RealmExec().getNodesForElection(senderAddress, recipientAddress)
        val getNodes = RealmExec().getNodesForElection2(senderAddress, recipientAddress)
        val candidateList: ArrayList<NodeEntity> = ArrayList()

        if (getNodes != null) {

            //Shuffle int position to get unique random
            val randomInt = ArrayList<Int>()
            Timber.d("node to randomize: ${getNodes.size}")
            for (i in getNodes.indices){
                randomInt.add(i)
            }
            randomInt.shuffle()

            for (i in 1..Constants.ElectionSize){
                Timber.d("selected randomInt: $i")
                val selectedNode = getNodes[randomInt[i]]

                Timber.d("selected node[$i]: ${selectedNode.id}")
                candidateList.add(selectedNode)
            }
        }

        val candidateMap = HashMap<String, String>()
        for (candidate in candidateList){
            candidateMap[candidate.id] = "${candidate.ipAddress}:${candidate.port}"
        }

        //need to create sorted json
        val candidateJson = Gson().toJson(candidateMap)
        val map = Gson().fromJson(candidateJson, TreeMap::class.java)
        val sortedJson: String = Gson().toJson(map)

        val candidateBytes = sortedJson.toByteArray()
        val signedCandidates = signPayload(privateKeyBigInt, candidateBytes)

        val unsignedIntCandidates = IntArray(signedCandidates.size)
        for ((index, byte) in signedCandidates.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntCandidates[index] = unsignedInt
        }

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
                txd,
                unsignedIntTxd,
                MeshTransactionFee(),
                byteArrayOf(),
                election,
                byteArrayOf()
        )
    }

    private fun toUnsigned(b: Byte): Int {
        return (if (b >= 0) b else 256 + b).toInt()
    }

    private fun toUnsignedIntArray(bytes: ByteArray): IntArray{
        val unsignedIntArray = IntArray(bytes.size)

        for ((index, byte) in bytes.withIndex()){
            val unsignedInt = toUnsigned(byte)
            unsignedIntArray[index] = unsignedInt
        }

        return unsignedIntArray
    }

}
