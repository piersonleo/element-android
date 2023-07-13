package com.vcard.vchat.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.utils.toast
import org.matrix.android.sdk.api.util.MimeTypes
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class Utils {
    companion object{
        fun removeUrlSuffix(param: String?): String?{
            if (param == null){
                return null
            }
            return param.removeSuffix(Constants.URL_SUFFIX)
        }

        fun getJsonDataFromAsset(context: Context, fileName: String): String {
            val jsonString: String
            try {
                jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                return ""
            }
            return jsonString
        }

        fun getTime(type: String = "UTC", pattern: String = "yyyy-MM-dd'T'HH:mm:ss'.327Z'"): String {
            val dateFormat = SimpleDateFormat(pattern)

            val timeResult = if (type == "UTC") {
                val time = Calendar.getInstance().time
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                dateFormat.format(time)
            } else {
                dateFormat.format(Calendar.getInstance().time).toString()
            }

            return timeResult
        }

        fun convertTimeToView(time: String?, toPattern: String = "HH:mm:ss EEE, d MMM yyyy"): String {
            return if (time != null && time != "") {
                val dbPattern = "yyyy-MM-dd'T'HH:mm:ss"
                val dbDateFormat = SimpleDateFormat(dbPattern, Locale.getDefault())
                dbDateFormat.timeZone = TimeZone.getTimeZone("UTC")

                val dateDB = dbDateFormat.parse(time)!!
                val outputFormat = SimpleDateFormat(toPattern, Locale.getDefault())
                outputFormat.format(dateDB)
            } else {
                ""
            }
        }

        fun saveJsonFile(
                activity: Activity,
                activityResultLauncher: ActivityResultLauncher<Intent>,
                defaultFileName: String,
                chooserHint: String
        ) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/json"
            intent.putExtra(Intent.EXTRA_TITLE, defaultFileName)
            val chooserIntent = Intent.createChooser(intent, chooserHint)
            try {
                activityResultLauncher.launch(chooserIntent)
            } catch (activityNotFoundException: ActivityNotFoundException) {
                activity.toast(R.string.error_no_external_application_found)
            }
        }

        fun shareJsonFile(fragment: Fragment,
                          activityResultLauncher: ActivityResultLauncher<Intent>?,
                          chooserTitle: String?,
                          text: String,
                          subject: String? = null,
                          extraTitle: String? = null,
                          filename: String) {

            val jsonFile = createJsonFile(fragment.requireContext(), text, filename)

            val share = Intent(Intent.ACTION_SEND)
            share.type = "application/json"
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Add data to the intent, the receiving app will decide what to do with it.
            share.putExtra(Intent.EXTRA_SUBJECT, subject)
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(fragment.requireContext(), BuildConfig.APPLICATION_ID + ".fileProvider", jsonFile))


            extraTitle?.let {
                share.putExtra(Intent.EXTRA_TITLE, it)
            }

            val intent = Intent.createChooser(share, chooserTitle)
            try {
                if (activityResultLauncher != null) {
                    activityResultLauncher.launch(intent)
                } else {
                    fragment.startActivity(intent)
                }
            } catch (activityNotFoundException: ActivityNotFoundException) {
                fragment.activity?.toast(R.string.error_no_external_application_found)
            }
        }

        fun createJsonFile(context: Context, jsonString: String, filename: String):File {
            val rootFolder: File? = context.getExternalFilesDir(null)
            val jsonFile = File(rootFolder, "$filename.json")
            val writer = FileWriter(jsonFile)
            writer.write(jsonString)
            writer.close()

            return jsonFile
            //or IOUtils.closeQuietly(writer);
        }

        fun deleteJsonFile(context: Context, filename: String) {
                val storageDir = File(context.getExternalFilesDir(null), "$filename.json")

                val jsonFile = File(storageDir, filename)
                jsonFile.delete()

                Timber.d("DELETE FILE")
        }

        fun openJsonFileSelection(activity: Activity,
                              activityResultLauncher: ActivityResultLauncher<Intent>?,
                              allowMultipleSelection: Boolean,
                              requestCode: Int) {
            val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
            fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelection)

            fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
            fileIntent.type = "application/json"

            try {
                if (activityResultLauncher != null) {
                    activityResultLauncher.launch(fileIntent)
                } else {
                    activity.startActivityForResult(fileIntent, requestCode)
                }
            } catch (activityNotFoundException: ActivityNotFoundException) {
                activity.toast(R.string.error_no_external_application_found)
            }
        }

        fun mergeBitmapLogoToQrCode(logo: Bitmap, qrCode: Bitmap): Bitmap {
            val combinedBitmap = Bitmap.createBitmap(qrCode.width, qrCode.height, qrCode.config)
            val canvas = Canvas(combinedBitmap)
            val canvasWidth = canvas.width
            val canvasHeight = canvas.height
            canvas.drawBitmap(qrCode, Matrix(), null)

            val resizedLogo = Bitmap.createScaledBitmap(logo, canvasWidth / 4, canvasHeight / 4, true)
            val centerX = (canvasWidth - resizedLogo.width)/2
            val centerY = (canvasHeight - resizedLogo.height)/2
            canvas.drawBitmap(resizedLogo, centerX.toFloat(), centerY.toFloat(), null)
            return combinedBitmap
        }
    }
}
