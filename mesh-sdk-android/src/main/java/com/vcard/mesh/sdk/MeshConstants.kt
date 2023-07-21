//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk

object MeshConstants {
    const val MaximumFullAddressHexLength = 50
    const val MaximumPrefixByteLength = 2
    const val MaximumPrefixHexLength = 2
    const val MaximumAddressBytesLength = 20
    const val MaximumAddressHexLength = 40
    const val MaximumAddressChecksumBytesLength = 4
    const val MaximumAddressChecksumHexLength = 8

    const val ServiceCommandGetAccount = "ga1"
    const val ServiceCommandTxn = "txn"
    const val ServiceCommandTxnReq = "txnreq"
    const val ServiceCommandGetMutxoByAddress = "gma"

    const val ElectionSize = 16

    const val MeshSystemAddress = "m1c452dfbe4e0021bd5e8f3978f2bed2930c34f3ce29fe9724"
    const val MeshCommunityAddress = "m19f8b249d2b4d95a28d8e2868ec1b0a71a9ee77f7e0b8b987"

    const val MeshGoldCurrency = "au79"
    const val MeshCoinCurrency = "mccm"

    const val DefaultKeystoreCipher = "aes256-256"

    //1 trillion
    const val kilogramUnit = "kg"
    const val kilogramRate = 1000000000000

    //1 billion
    const val gramUnit = "g"
    const val gramRate = 1000000000

    //1 million
    const val milligramUnit = "mg"
    const val milligramRate = 1000000

    const val microgramUnit = "µg"
    const val microgramRate = 1000

    const val nanogramUnit = "ng"
    const val nanogramRate = 1

    //to use as prefix for qr code
    const val meshWalletQrIdentifier = "MESHW|"
    const val meshEncryptedAccountQrIdentifier = "KEYE|"

    const val test = "test"

}
