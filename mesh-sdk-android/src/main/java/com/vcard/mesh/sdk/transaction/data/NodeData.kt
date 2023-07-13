//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.transaction.data

import com.google.gson.annotations.SerializedName

class NodeData {
    @SerializedName("id", alternate = ["Id"])
    val id: String = ""

    @SerializedName("ipAddress", alternate = ["IpAddress"])
    var ipAddress: String = ""

    @SerializedName("port", alternate = ["Port"])
    var port: String = ""
}

