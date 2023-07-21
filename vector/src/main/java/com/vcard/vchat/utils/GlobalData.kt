package com.vcard.vchat.utils

import java.util.UUID

class GlobalData {
    companion object {
        val launchPp = UUID.randomUUID().toString().replace("-", "")
    }
}
