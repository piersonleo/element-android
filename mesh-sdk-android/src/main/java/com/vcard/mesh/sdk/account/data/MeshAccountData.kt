//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.account.data

import com.google.gson.annotations.SerializedName

class MeshAccountData {

    @SerializedName("address", alternate = ["AccountAddress", "a"])
    val address: String = ""

    @SerializedName("name", alternate = ["n"])
    val name: String = ""

    @SerializedName("encryptedKey", alternate = ["encryptedkey", "e"])
    var encryptedKey: String = ""

    @SerializedName("privateKey", alternate = ["privatekey"])
    var privateKey: String = ""

    @SerializedName("Currency")
    var currency: String = ""

    @SerializedName("Nonce")
    var nonce: Int = 0

    @SerializedName("Balance")
    var balance: Long = 0L

    @SerializedName("RootHash")
    var rootHash: String = ""

    @SerializedName("ModuleHash")
    var moduleHash: String? = ""

    @SerializedName("type", alternate = ["Type"])
    var type: String = ""

}
