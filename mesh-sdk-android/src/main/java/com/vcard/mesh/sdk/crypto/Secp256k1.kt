//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.crypto

import com.vcard.mesh.sdk.utils.HashUtil
import com.vcard.mesh.sdk.utils.NumberUtil
import org.apache.tuweni.crypto.SECP256K1
import java.io.ByteArrayOutputStream
import java.math.BigInteger

object Secp256k1 {

    fun  signPayload(privateKey: BigInteger, payloadBytes: ByteArray): ByteArray{

        val keyPair = SECP256K1.KeyPair.fromSecretKey(
                SECP256K1.SecretKey.fromInteger(
                        privateKey
                )
        )

        val hashMessage = HashUtil.meshHash(payloadBytes)

        val signature = SECP256K1.signHashed(hashMessage, keyPair)

        return signature.bytes().toArray()

        //the code below has been found to cause inconsistent signature length. We'll use tuweni's signature to byteArray instead as it produce the desired result
//        val outputStream = ByteArrayOutputStream()
//
//        val r = signature.r()
//        val s = signature.s()
//        val v = signature.v()

//        val rBytes = r.toByteArray()
//        val sBytes = s.toByteArray()
//        val vBytes = byteArrayOf(v)
//
//        Timber.d("signPayload sig: ${signature.bytes().toArray().contentToString()}\nlength: ${signature.bytes().size()}")
//        Timber.d("signedPayload, rBytes: ${rBytes.contentToString()}\nrLength: ${rBytes.size}\n sBytes: ${sBytes.contentToString()}\nsLength: ${sBytes.size}")
//        outputStream.write(rBytes)
//        outputStream.write(sBytes)
//        outputStream.write(vBytes)
//
//        return outputStream.toByteArray()
    }

    fun signCandidateList(privateKey: BigInteger, transactionDataBytes: ByteArray, candidateListBytes: ByteArray): ByteArray{
        val keyPair = SECP256K1.KeyPair.fromSecretKey(
                SECP256K1.SecretKey.fromInteger(
                        privateKey
                )
        )

        val hashTxd = HashUtil.meshHash(transactionDataBytes)
        val hashCandidates = HashUtil.meshHash(candidateListBytes)
        val mergedHash = ByteArrayOutputStream()
        mergedHash.write(hashTxd)
        mergedHash.write(hashCandidates)

        val hashMessage = HashUtil.meshHash(mergedHash.toByteArray())

        val signature = SECP256K1.signHashed(hashMessage, keyPair)

        val outputStream = ByteArrayOutputStream()

        val r = signature.r()
        val s = signature.s()
        val v = signature.v()

        val rBytes = NumberUtil.bigIntToBytes(r)
        val sBytes = NumberUtil.bigIntToBytes(s)
        val vBytes = byteArrayOf(v)

        outputStream.write(rBytes)
        outputStream.write(sBytes)
        outputStream.write(vBytes)

        return outputStream.toByteArray()
    }

    fun recoverPublicKey(hashMessage: ByteArray, signature: SECP256K1.Signature): SECP256K1.PublicKey?{
        val recoveredPublicKey = SECP256K1.PublicKey.recoverFromHashAndSignature(hashMessage, signature)

        return recoveredPublicKey
    }

    fun isMessageValid(hashMessage: ByteArray, signature: SECP256K1.Signature, publicKey: SECP256K1.PublicKey): Boolean{
        val verifyMessage = SECP256K1.verifyHashed(hashMessage, signature, publicKey)

        return verifyMessage
    }
}
