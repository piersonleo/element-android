package im.vector.app.features.wallet

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import arrow.core.Try
import com.airbnb.mvrx.args
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.vcard.vchat.mesh.Account
import com.vcard.vchat.mesh.Address
import com.vcard.vchat.mesh.Aes256
import com.vcard.vchat.mesh.Constants
import com.vcard.vchat.mesh.CurrencyEnum
import com.vcard.vchat.mesh.HashUtils
import com.vcard.vchat.mesh.MeshCommand
import com.vcard.vchat.mesh.NumberUtil
import com.vcard.vchat.mesh.QrCode
import com.vcard.vchat.mesh.data.EncryptedKeyData
import com.vcard.vchat.mesh.data.EncryptedKeyDataSerializer
import com.vcard.vchat.mesh.data.MUnspentTransactionObjectData
import com.vcard.vchat.mesh.data.MutxoListData
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import com.vcard.vchat.utils.MeshSharedPref
import com.vcard.vchat.utils.StringUtil
import com.vcard.vchat.utils.Utils
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.safeOpenOutputStream
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.ui.views.QrCodeImageView
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.shareMedia
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.DialogBaseInputPassphraseBinding
import im.vector.app.databinding.DialogMeshChangePassphraseBinding
import im.vector.app.databinding.FragmentWalletDetailBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.qrcode.QrCodeScannerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class WalletDetailFragment @Inject constructor(
): VectorBaseFragment<FragmentWalletDetailBinding>() {

    private val fragmentArgs: WalletDetailsArgs by args()

    private var accountBalance = BigDecimal.ZERO
    private var displayUnit = Constants.gramUnit
    private var ekfJson = ""
    private var ek = ""


    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOpenQRCodeScanner()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(R.string.denied_permission_camera)
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletDetailBinding {
        setHasOptionsMenu(true)

        return FragmentWalletDetailBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupAccount()
        setScanButtonState()
        setupSubmitButton()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val changePassphrase = menu.findItem(R.id.change_passphrase)
        if (changePassphrase != null && fragmentArgs.type == Constants.test) {
            changePassphrase.isVisible = false
        }

        val saveAccount = menu.findItem(R.id.save_account)
        if (changePassphrase != null && fragmentArgs.type == Constants.test) {
            saveAccount.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.save_account -> {
                showSaveBottomDialog()
            }
            R.id.change_name -> {
                showChangeNameDialog(views.walletDetailHeaderTitle.text.toString())
            }
            R.id.change_passphrase ->{
                showChangePpDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupViews(){
        setupToolbar(views.walletDetailToolBar)
                .setTitle(fragmentArgs.name)
                .allowBack(useCross = false)

        views.walletDetailHeaderTitle.text = fragmentArgs.name

        val assetManager = requireContext().resources.assets
        val inputStream = assetManager.open("wallet_avatars/ic_card_placeholder.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)

        GlideApp.with(requireContext()).load(bitmap).into(views.walletImageView)

    }

    private fun setupAccount(){
        val accountData = RealmExec().getAccountByAddress(fragmentArgs.address)
        if (accountData != null) {
            accountBalance = BigDecimal(accountData.balance)
            ek = accountData.encryptedKey
            displayBalance(accountBalance)
        }else{
            views.btnRefreshAccount.performClick()
        }
    }

    private fun setScanButtonState(){
        views.btnScanQRCode.isEnabled = accountBalance.signum() != 0
    }


    private fun setupSubmitButton() {
        views.btnScanQRCode.debouncedClicks {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
                doOpenQRCodeScanner()
            }

        }

        views.btnShowQRCode.debouncedClicks {
            WalletCodeActivity.newIntent(requireContext(), fragmentArgs.name, fragmentArgs.address).let {
                val options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                views.btnShowQRCode,
                                ViewCompat.getTransitionName(views.btnShowQRCode) ?: ""
                        )
                startActivity(it, options.toBundle())
            }
        }

        views.btnRefreshAccount.debouncedClicks{

            if (fragmentArgs.type == Constants.test) {
                views.walletDetailHeaderSubtitle.visibility = View.INVISIBLE
                views.simpleActivityWaitingView.visibility = View.VISIBLE

                Thread {
                    val refreshAccount = MeshCommand.getAccount(fragmentArgs.address, fragmentArgs.privateKey)

                    when (refreshAccount.c) {
                        "unavailable" -> {

                            activity?.runOnUiThread(Runnable {
                                views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                                views.simpleActivityWaitingView.visibility = View.INVISIBLE

                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_unavailable), Toast.LENGTH_SHORT).show()
                            })
                        }
                        "ok"          -> {
                            activity?.runOnUiThread(Runnable {
                                views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                                views.simpleActivityWaitingView.visibility = View.INVISIBLE

                                val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
                                val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                                val gson = GsonBuilder().disableHtmlEscaping().create()
                                val accountData = gson.fromJson(decodeParamsUtf8, MutxoListData::class.java)

                                val address = Address.createFullAddress(accountData.ownerAddress.prefix, accountData.ownerAddress.address, accountData.ownerAddress.checksum)

                                val accountEntity = AccountEntity()
                                accountEntity.address = address
                                accountEntity.balance = accountData.total
                                accountEntity.currency = CurrencyEnum.MeshGold.name
//
                                RealmExec().addUpdateAccountBalance(accountEntity)

                                //TODO: for int and long values they needed to be accessed first before they can be inserted to realm. Need to investigate
//                              RealmExec().addAccountMutxoFromMap(address, accountData.mutxoList)

                                //Temp function to insert the data manually
                                RealmExec().addAccountMutxoFromMapManual(address, accountData.mutxoList)

                                accountBalance = BigDecimal(accountData.total)

                                displayBalance(accountBalance)

                                if (BigDecimal(accountData.total).signum() == 0){
                                    Toast.makeText(requireContext(), getString(R.string.vchat_account_no_gold), Toast.LENGTH_SHORT).show()
                                }

                                setScanButtonState()
                            })


                        }
                        "busy"        -> {
                            activity?.runOnUiThread(Runnable {
                                views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                                views.simpleActivityWaitingView.visibility = View.INVISIBLE

                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_busy), Toast.LENGTH_SHORT).show()
                            })
                        }
                        else          -> {
                            activity?.runOnUiThread(Runnable {
                                views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                                views.simpleActivityWaitingView.visibility = View.INVISIBLE

//                                val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
//                                val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)
//
//                                Timber.d("decodeParams: $decodeParamsUtf8")

                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_update_account_fail), Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }.start()
            }else{
                val msp = MeshSharedPref(requireContext())
                val pp = msp.getPp(fragmentArgs.address)
                if (pp == "") {
                    inputPassphrase(ek)
                }else {
                    try {
                        getBalance(ek, pp)
                    }catch (e: java.lang.Exception){
                        if (e.message == "invalid passphrase"){
                            Toast.makeText(requireContext(), getString(R.string.vchat_error_invalid_pass), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        views.walletDetailHeaderSubtitle.debouncedClicks {

            //switch display
            displayUnit = if (displayUnit == Constants.gramUnit)  Constants.milligramUnit else Constants.gramUnit
            displayBalance(accountBalance)

        }
    }

    private fun doOpenQRCodeScanner() {
        QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
    }

    private val scanActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val scannedQrCode = QrCodeScannerActivity.getResultText(activityResult.data)
            val wasQrCode = QrCodeScannerActivity.getResultIsQrCode(activityResult.data)

            if (wasQrCode && !scannedQrCode.isNullOrBlank()) {

                try{
                    val meshQr = QrCode.parseQrCodeContent(scannedQrCode)

                    if (meshQr.address == fragmentArgs.address){
                        Toast.makeText(requireContext(), getString(R.string.vchat_wallet_account_scan_qr_error_yourself), Toast.LENGTH_SHORT).show()
                    }else{
                        val intent = WalletTransferActivity.newIntent(this.requireContext(), fragmentArgs.address, meshQr.address, fragmentArgs.privateKey, ek, fragmentArgs.type)
                        startActivity(intent)
                    }

                }catch (e: IllegalArgumentException){
                    Toast.makeText(requireContext(), getString(R.string.vchat_wallet_account_scan_qr_error_invalid), Toast.LENGTH_SHORT).show()
                }
            } else {
                Timber.w("It was not a QR code, or empty result")
            }
        }
    }

    private fun inputPassphrase(ek: String) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_input_passphrase, null)
        val dialogViews = DialogBaseInputPassphraseBinding.bind(layout)

        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.vchat_enter_passphrase))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create()

        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            confirmButton.debouncedClicks {
                try {
                    val pp = dialogViews.editText.text.toString()
                    getBalance(ek, pp)
                    dialog.dismiss()
                }catch (e: java.lang.Exception){
                    if (e.message == "invalid passphrase"){
                        dialogViews.baseTil.error = getString(R.string.vchat_error_invalid_pass)
                    }
                }

            }
        }
        dialog.show()
    }

    private fun getBalance(ek: String, pp: String){
        val kBytes = NumberUtil.hexStrToBytes(ek)
        val dk = Aes256.decryptGcm(kBytes, pp)

        if (dk == "invalid passphrase"){
            throw Exception("invalid passphrase")
        }else {
            views.walletDetailHeaderSubtitle.visibility = View.INVISIBLE
            views.simpleActivityWaitingView.visibility = View.VISIBLE
            val msp = MeshSharedPref(requireContext())
            msp.storePp(fragmentArgs.address, pp)

            Thread {
                Timber.d("address: ${fragmentArgs.address}")

                val refreshAccount = MeshCommand.getAccount(fragmentArgs.address, dk)
                Timber.d("account data: $refreshAccount")

                when (refreshAccount.c) {
                    "unavailable" -> {

                        activity?.runOnUiThread(Runnable {
                            views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                            views.simpleActivityWaitingView.visibility = View.INVISIBLE

                            Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_unavailable), Toast.LENGTH_SHORT).show()
                        })
                    }
                    "ok"          -> {
                        activity?.runOnUiThread(Runnable {
                            views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                            views.simpleActivityWaitingView.visibility = View.INVISIBLE

                            val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
                            val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                            Timber.d("decodeParams: $decodeParamsUtf8\n")

                            val gson = GsonBuilder().disableHtmlEscaping().create()
                            val accountData = gson.fromJson(decodeParamsUtf8, MutxoListData::class.java)

                            val address = Address.createFullAddress(accountData.ownerAddress.prefix, accountData.ownerAddress.address, accountData.ownerAddress.checksum)

                            val accountEntity = AccountEntity()
                            accountEntity.address = address
                            accountEntity.balance = accountData.total
                            accountEntity.currency = CurrencyEnum.MeshGold.name

                            RealmExec().addUpdateAccountBalance(accountEntity)
                            RealmExec().addAccountMutxoFromMap(address, accountData.mutxoList)

                            accountBalance = BigDecimal(accountData.total)

                            displayBalance(accountBalance)

                            if (BigDecimal(accountData.total).signum() == 0){
                                Toast.makeText(requireContext(), getString(R.string.vchat_account_no_gold), Toast.LENGTH_SHORT).show()
                            }

                            setScanButtonState()
                        })
                    }
                    "busy"        -> {
                        activity?.runOnUiThread(Runnable {
                            views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                            views.simpleActivityWaitingView.visibility = View.INVISIBLE

                            Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_busy), Toast.LENGTH_SHORT).show()
                        })
                    }
                    else          -> {
                        activity?.runOnUiThread(Runnable {
                            views.walletDetailHeaderSubtitle.visibility = View.VISIBLE
                            views.simpleActivityWaitingView.visibility = View.INVISIBLE

//                            val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
//                            val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)
//
//                            Timber.d("decodeParams: $decodeParamsUtf8")

                            Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_update_account_fail), Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }.start()
        }
    }

    private fun displayBalance(balance: BigDecimal){

        val conversionRate = if (displayUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate

        val convertedBalance = balance.divide(BigDecimal(conversionRate))
        val formattedBalance = StringUtil.formatBalanceForDisplayBigDecimal(convertedBalance)
        val currentBalance = "$formattedBalance $displayUnit"
        views.walletDetailHeaderSubtitle.text = currentBalance
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
        try {
            ekfJson = Account.generateAccountEkJson(fragmentArgs.address, ek)
        }catch (e: java.lang.Exception){
            if (e.message == "invalid address"){
                return Toast.makeText(requireContext(), getString(R.string.vchat_error_invalid_account_address), Toast.LENGTH_SHORT).show()
            }
        }
        accountQRImage.setData2("${Constants.meshEncryptedAccountQrIdentifier}$ekfJson", icon)

        shareAccountView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.name}-${timestamp}"

            //encrypted key file don't use identifier prefix
            val file = Utils.createJsonFile(requireContext(), ekfJson, filename)
            shareMedia(requireContext(), file, getMimeTypeFromUri(requireContext(), file.toUri()))

            Utils.deleteJsonFile(requireContext(), filename)
            dialog.dismiss()
        }

        saveAccountView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.name}-${timestamp}"
            Utils.saveJsonFile(
                    activity = requireActivity(),
                    activityResultLauncher = saveRecoveryActivityResultLauncher,
                    defaultFileName = filename,
                    chooserHint = getString(R.string.vchat_wallet_account_save_account)
            )
            dialog.dismiss()
        }

        saveQrView.debouncedClicks {
            val timestamp = Utils.getTime()
            val filename = "mesh-account-${fragmentArgs.name}-${timestamp}"
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
            exportRecoveryKeyToFile(uri, ekfJson)
        }
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

    private fun showChangeNameDialog(currentName: String) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val dialogViews = DialogBaseEditTextBinding.bind(layout)
        dialogViews.editText.setText(currentName)
        dialogViews.baseTil.hint = getString(R.string.vchat_account_name)


        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.vchat_change_account_name))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.vchat_change_account_name, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create()

        dialog.setOnShowListener {
            val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            updateButton.debouncedClicks {
                val newName = dialogViews.editText.text.toString()
                if (newName.isEmpty()) {
                    dialogViews.editText.error = getString(R.string.vchat_error_name_empty)
                }else{
                    changeName(dialogViews.editText.text.toString())
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun changeName(newName: String){
        val accountEntity = AccountEntity()
        accountEntity.address = fragmentArgs.address
        accountEntity.name = newName

        Thread{
            RealmExec().addUpdateAccountName(accountEntity)
        }.start()

        views.walletDetailHeaderTitle.text = newName
        views.walletDetailToolBar.title = newName
    }

    private fun showChangePpDialog() {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_mesh_change_passphrase, null)
        val dialogViews = DialogMeshChangePassphraseBinding.bind(layout)


        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.vchat_change_account_passphrase))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.vchat_change_account_passphrase, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create()

        dialog.setOnShowListener {
            val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            updateButton.debouncedClicks {
                when {
                    dialogViews.etCurrentPp.text.toString() == "" -> {
                        dialogViews.currentPpTil.error = getString(R.string.vchat_error_current_passphrase_empty)
                    }
                    dialogViews.etNewPp.text.toString() == "" -> {
                        dialogViews.newPpTil.error = getString(R.string.vchat_error_new_passphrase_empty)
                    }
                    dialogViews.etRetypeNewPp.text.toString() == "" -> {
                        dialogViews.retypeNewPpTil.error = getString(R.string.vchat_error_retype_new_passphrase_empty)
                    }
                    dialogViews.etNewPp.text.toString() != dialogViews.etRetypeNewPp.text.toString() -> {
                        dialogViews.newPpTil.error = getString(R.string.vchat_error_retype_new_passphrase_not_match)
                        dialogViews.retypeNewPpTil.error = getString(R.string.vchat_error_retype_new_passphrase_not_match)
                    }
                    else -> {
                        try {
                            ek = Account.changeAccountPp(fragmentArgs.address, ek, dialogViews.etCurrentPp.text.toString(), dialogViews.etRetypeNewPp.text.toString())
                            val msp = MeshSharedPref(requireContext())
                            msp.storePp(fragmentArgs.address, dialogViews.etRetypeNewPp.text.toString())

                            Toast.makeText(requireContext(), getString(R.string.vchat_passphrase_updated), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }catch (e: java.lang.Exception){
                            if (e.message == "invalid passphrase"){
                                Toast.makeText(requireContext(), getString(R.string.vchat_error_current_passphrase_invalid), Toast.LENGTH_SHORT).show()
                            }else if (e.message == "invalid address"){
                                Toast.makeText(requireContext(), getString(R.string.vchat_error_invalid_account_address), Toast.LENGTH_SHORT).show()

                            }
                        }
                    }
                }

            }
        }

        dialog.show()
    }
}
