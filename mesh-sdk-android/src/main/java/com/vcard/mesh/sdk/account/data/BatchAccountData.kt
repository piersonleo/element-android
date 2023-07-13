//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.account.data

import com.google.gson.annotations.SerializedName

class BatchAccountData {

    @SerializedName("n", alternate = ["name"])
    var name: String? = null

    @SerializedName("a", alternate = ["address"])
    var address: String? = null

    @SerializedName("e", alternate = ["encryptedKey"])
    var encryptedKey: String? = null
}
