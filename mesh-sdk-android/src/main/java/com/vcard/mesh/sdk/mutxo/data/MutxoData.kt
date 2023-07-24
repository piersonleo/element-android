//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.mutxo.data

import com.google.gson.annotations.SerializedName
import com.vcard.mesh.sdk.address.data.MeshAddressData

class MutxoData {

    @SerializedName("mutxoKey", alternate = ["k"])
    var mutxoKey :String = ""

    @SerializedName("ownerAddress", alternate = ["o"])
    var ownerAddress: MeshAddressData = MeshAddressData()

    var fullAddress = ""

    @SerializedName("currency", alternate = ["c"])
    var currency: String = ""

    @SerializedName("amount", alternate = ["a"])
    var amount: Long = 0L

    @SerializedName("source", alternate = ["s"])
    var source: String = ""

    @SerializedName("sourceType", alternate = ["st"])
    var sourceType: Int = 0

    @SerializedName("reference", alternate = ["r"])
    var reference: String = ""
}
