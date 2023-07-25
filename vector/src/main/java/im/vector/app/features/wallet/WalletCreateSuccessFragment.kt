package im.vector.app.features.wallet

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import arrow.core.Try
import com.airbnb.mvrx.args
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcard.mesh.sdk.MeshConstants
import com.vcard.vchat.utils.Utils
import com.vcard.vchat.utils.Utils.Companion.createJsonFile
import com.vcard.vchat.utils.Utils.Companion.deleteJsonFile
import com.vcard.vchat.utils.Utils.Companion.mergeBitmapLogoToQrCode
import com.vcard.vchat.utils.Utils.Companion.saveJsonFile
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.safeOpenOutputStream
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.qrcode.toBitMatrixMesh
import im.vector.app.core.qrcode.toBitmap
import im.vector.app.core.ui.views.QrCodeImageView
import im.vector.app.core.utils.shareMedia
import im.vector.app.databinding.FragmentWalletCreateSuccessBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class WalletCreateSuccessFragment@Inject constructor(
) : VectorBaseFragment<FragmentWalletCreateSuccessBinding>() {

    private val fragmentArgs: WalletCreateSuccessActivity.Args by args()

    private lateinit var accountJsonString: String

    private lateinit var callback: CreateSuccessCallback

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletCreateSuccessBinding {

        return FragmentWalletCreateSuccessBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountJsonString = "${MeshConstants.meshEncryptedAccountQrIdentifier}${fragmentArgs.jsonString}"
        setupViews()
        setupClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as CreateSuccessCallback
    }

    private fun setupViews(){
        setupToolbar(views.walletCreateSuccessToolbar)
                .setTitle(getString(R.string.create_account_title))
                .allowBack(useCross = false)
    }

    private fun setupClick(){
        views.btnWalletAccountSave.debouncedClicks {
            showSaveBottomDialog()
        }
        views.btnCreateAccountSuccessDone.debouncedClicks {
            callback.onDone()
        }

    }

    private fun showSaveBottomDialog(){
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_save_wallet_account, null)

        val shareAccountView = view.findViewById<LinearLayout>(R.id.llShareWallet)
        val saveAccountView = view.findViewById<LinearLayout>(R.id.llSaveWallet)
        val saveQrView = view.findViewById<LinearLayout>(R.id.llSaveQr)

        val  accountQrView = view.findViewById<MaterialCardView>(R.id.cvAccountQr)
        val accountQRImage = view.findViewById<QrCodeImageView>(R.id.accountQRImage)
        val accountQrTitle = view.findViewById<TextView>(R.id.tvAccountTitle)

        accountQrTitle.text = getString(R.string.mesh_account_title)

        val icon = BitmapFactory.decodeResource(resources, R.drawable.vchat_circular_key)
        //for qr code we use mesh identifier prefix
        accountQRImage.setData2(accountJsonString, icon)

        shareAccountView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.accountName}-${timestamp}"

            //encrypted key file don't use identifier prefix
            val file = createJsonFile(requireContext(), fragmentArgs.jsonString, filename)
            shareMedia(requireContext(), file, getMimeTypeFromUri(requireContext(), file.toUri()))

            deleteJsonFile(requireContext(), filename)
            dialog.dismiss()
        }

        saveAccountView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.accountName}-${timestamp}"
            saveJsonFile(
                    activity = requireActivity(),
                    activityResultLauncher = saveRecoveryActivityResultLauncher,
                    defaultFileName = filename,
                    chooserHint = getString(R.string.vchat_wallet_account_save_account)
            )
            dialog.dismiss()
        }

        saveQrView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.accountName}-${timestamp}"
//            val qrBitmap = createQRCodeBitmap(fragmentArgs.jsonString)
            saveBitmap(accountQrView.drawToBitmap(), filename)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun exportRecoveryKeyToFile(uri: Uri, data: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Try {
                withContext(Dispatchers.IO) {
                    requireContext().safeOpenOutputStream(uri)
                            ?.use { os ->
                                os.write(data.toByteArray())
                                os.flush()
                            }
                }
                        ?: throw IOException("Unable to write the file")
            }
                    .fold(
                            { throwable ->
                                activity?.let {
                                    MaterialAlertDialogBuilder(it)
                                            .setTitle(R.string.dialog_title_error)
                                            .setMessage(errorFormatter.toHumanReadable(throwable))
                                }
                            },
                            {
                                activity?.let {
                                    MaterialAlertDialogBuilder(it)
                                            .setTitle(R.string.dialog_title_success)
                                            .setMessage(R.string.vchat_create_mesh_account_export_saved)
                                }
                            }
                    )
                    ?.setCancelable(false)
                    ?.setPositiveButton(R.string.ok, null)
                    ?.show()
        }
    }

    private val saveRecoveryActivityResultLauncher = registerStartForActivityResult { activityResult ->
        val uri = activityResult.data?.data ?: return@registerStartForActivityResult
        if (activityResult.resultCode == Activity.RESULT_OK) {
            //encrypted key file don't use identifier prefix
            exportRecoveryKeyToFile(uri, fragmentArgs.jsonString)
        }
    }

    private fun createQRCodeBitmap(data: String): Bitmap {
        val qrCode = data.toBitMatrixMesh(450).toBitmap()
        val icon = BitmapFactory.decodeResource(resources, R.drawable.img_logo_vbiz_rounded_corner_2)

        return mergeBitmapLogoToQrCode(icon, qrCode)
    }

    @Suppress("DEPRECATION")
    private fun saveBitmap(bitmap: Bitmap, filename: String) {
        try {
            val fileName = "$filename.png"
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/vChat")
                values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + File.separator + "vChat")
                val file = File(directory, fileName)
                values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
            val uri: Uri? = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            requireActivity().contentResolver.openOutputStream(uri!!).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                requireActivity().contentResolver.update(uri, values, null, null)
            }

            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_title_success)
                    .setMessage(R.string.vchat_create_mesh_account_export_saved_qr_code)
                    .show()

        } catch (e: Exception) {
            Timber.d( e.toString()) // java.io.IOException: Operation not permitted
        }
    }

    interface CreateSuccessCallback{
        fun onDone()
    }
}
