package com.vcard.vchat.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object StringUtil {

    fun formatBalanceForDisplay(value: Double): String {
        val df = DecimalFormat("###,###", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        df.maximumFractionDigits = 340 //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

        return df.format(value)
    }
}
