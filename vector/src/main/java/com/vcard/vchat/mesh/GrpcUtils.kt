package com.vcard.vchat.mesh

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

object GrpcUtils {

    private const val localhost = "localhost"
    private const val localPort = 8080

    fun getChannel(): ManagedChannel? {
        var channel: ManagedChannel? = null
        return try {
            channel = ManagedChannelBuilder
                    .forAddress("54.169.3.92", 6810)
                    .idleTimeout(30, TimeUnit.SECONDS)
                    .usePlaintext()
                    .build()
            channel
        } catch (ex: Exception) {
            channel
        }

    }
}
