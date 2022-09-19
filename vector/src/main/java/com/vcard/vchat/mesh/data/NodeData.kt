package com.vcard.vchat.mesh.data

import com.google.gson.annotations.SerializedName

class NodeData {
    @SerializedName("id", alternate = ["Id"])
    val id: String = ""

    @SerializedName("ipAddress", alternate = ["IpAddress"])
    var ipAddress: String = ""

    @SerializedName("port", alternate = ["Port"])
    var port: String = ""
}
