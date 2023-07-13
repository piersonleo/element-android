//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.address.data

import com.google.gson.annotations.SerializedName
import com.vcard.mesh.sdk.MeshConstants

class MeshAddressData {

    @SerializedName("prefix", alternate = ["p"])
    var prefix: String = ""

    @SerializedName("address", alternate = ["a"])
    var address: ByteArray = ByteArray(MeshConstants.MaximumAddressBytesLength)

    @SerializedName("checksum", alternate = ["c"])
    var checksum: ByteArray = ByteArray(MeshConstants.MaximumAddressChecksumBytesLength)

}
