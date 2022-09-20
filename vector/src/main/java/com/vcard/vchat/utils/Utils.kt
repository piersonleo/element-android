package com.vcard.vchat.utils

class Utils {
    companion object{
        fun removeUrlSuffix(param: String?): String?{
            if (param == null){
                return null
            }
            return param.removeSuffix(Constants.URL_SUFFIX)
        }
    }
}
