//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.utils

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

object GrpcUtil {

    //For local testing
    private const val localhost = "localhost"
    private const val localPort = 8080

    //Some node ip examples that can be used
    //54.169.3.92, 3.91.253.117 -> US
    //18.181.249.151 -> Tokyo
    //3.26.229.98 -> Australia

    fun getChannel(): ManagedChannel? {
        var channel: ManagedChannel? = null
        return try {
            channel = ManagedChannelBuilder
                    .forAddress("18.181.249.151", 6810)
                    .idleTimeout(30, TimeUnit.SECONDS)
                    .usePlaintext()
                    .build()
            channel
        } catch (ex: Exception) {
            channel
        }

    }
}
