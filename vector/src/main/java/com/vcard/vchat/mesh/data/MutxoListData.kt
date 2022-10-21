package com.vcard.vchat.mesh.data

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class MutxoListData {

    @SerializedName("ownerAddress", alternate = ["o"])
    var ownerAddress: MeshAddressData = MeshAddressData()

    @SerializedName("total", alternate = ["t"])
    var total: String = "0"

    @SerializedName("mutxoList", alternate = ["l"])
    var mutxoList: Map<String, MUnspentTransactionObjectData> = emptyMap()
}
