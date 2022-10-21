package com.vcard.vchat.mesh.data

import com.google.gson.annotations.SerializedName
import com.vcard.vchat.mesh.Constants

class MeshAddressData {

    @SerializedName("prefix", alternate = ["p"])
    var prefix: String = ""

    @SerializedName("address", alternate = ["a"])
    var address: ByteArray = ByteArray(Constants.MaximumAddressBytesLength)

    @SerializedName("checksum", alternate = ["c"])
    var checksum: ByteArray = ByteArray(Constants.MaximumAddressChecksumBytesLength)

}
