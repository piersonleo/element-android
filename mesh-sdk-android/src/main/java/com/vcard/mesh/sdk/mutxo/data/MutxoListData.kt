//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.mutxo.data

import com.google.gson.annotations.SerializedName
import com.vcard.mesh.sdk.address.data.MeshAddressData
import java.math.BigInteger

class MutxoListData {

    @SerializedName("ownerAddress", alternate = ["o"])
    var ownerAddress: MeshAddressData = MeshAddressData()

    @SerializedName("total", alternate = ["t"])
    var total: String = "0"

    @SerializedName("mutxoList", alternate = ["l"])
    var mutxoList: Map<String, MutxoData> = emptyMap()
}
