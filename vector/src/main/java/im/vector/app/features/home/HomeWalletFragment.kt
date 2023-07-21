package im.vector.app.features.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import arrow.core.Try
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.vcard.mesh.sdk.account.Account
import com.vcard.mesh.sdk.address.MeshAddressUtil
import com.vcard.mesh.sdk.MeshConstants
import com.vcard.mesh.sdk.account.data.BatchAccountData
import com.vcard.mesh.sdk.account.data.EncryptedAccountData
import com.vcard.mesh.sdk.database.entity.AccountEntity
import com.vcard.mesh.sdk.database.MeshRealm
import com.vcard.vchat.utils.StringUtil
import com.vcard.vchat.utils.Utils
import com.vcard.vchat.utils.Utils.Companion.openJsonFileSelection
import com.vcard.vchat.utils.ViewAnimation
import com.vcard.vchat.utils.ViewUtil
import im.vector.app.R
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.safeOpenOutputStream
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.qrcode.toBitMatrix
import im.vector.app.core.qrcode.toBitmap
import im.vector.app.core.utils.shareMedia
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.FragmentWalletHomeBinding
import im.vector.app.features.qrcode.QrCodeScannerActivity
import im.vector.app.features.wallet.WalletCreateActivity
import im.vector.app.features.wallet.WalletDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import reactivecircus.flowbinding.android.widget.textChanges
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

//fragment for vChat's mesh wallet
@Parcelize
data class WalletDetailsArgs(
        val name: String,
        val currency: String,
        val address: String,
        val privateKey: String,
        val type: String
) : Parcelable

class HomeWalletFragment @Inject constructor(
        private val homeWalletController: HomeWalletItemController
): VectorBaseFragment<FragmentWalletHomeBinding>(), HomeWalletItemController.Callback {

    private var accounts: List<AccountEntity> = emptyList()

    private var isFabAddClicked = false
    private var jsonWallet = ""

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletHomeBinding {
        //setHasOptionsMenu(true)

        return FragmentWalletHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTestAccounts()
        setupRecyclerView()
        setupSearchView()
        insertNodesFromJson()
        setupButton()

        views.accountListSearch.setupAsSearch()
    }

    override fun onResume() {
        super.onResume()
        checkDataUpdated()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val homeFilterMenu = menu.findItem(R.id.menu_home_filter)
        if (homeFilterMenu != null) {
            homeFilterMenu.isVisible = false
        }

        val saveWalletsMenu = menu.findItem(R.id.menu_home_save_wallets)
        if (saveWalletsMenu != null) {
            saveWalletsMenu.isVisible = true
        }
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId){
//            R.id.menu_home_save_wallets -> {
//                showSaveWalletBottomDialog()
//            }
//        }
//
//        return super.onOptionsItemSelected(item)
//    }
//
    override fun onItemClick(wallet: AccountEntity) {

        val args = WalletDetailsArgs(
                name = wallet.name,
                currency = wallet.currency,
                address = wallet.address,
                privateKey = wallet.privateKey,
                type = wallet.type
        )
        val intent = WalletDetailActivity.newIntent(this.requireContext(), args)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        startActivity(intent, options.toBundle())
    }

    private fun setupTestAccounts() {
        val walletJson = Utils.getJsonDataFromAsset(this.requireContext(), "wallets.json")
        MeshRealm().addAccountsFromJson(walletJson)
    }

    private fun setupRecyclerView(){
        homeWalletController.callback = this
    }

    private fun setupSearchView() {
        views.accountListSearch
                .textChanges()
                .onEach { text ->
                    val searchValue = text.trim()
                    if (searchValue.isBlank()) {
                        homeWalletController.setData(accounts)
                        views.accountListRecyclerView.configureWith(homeWalletController)
                    } else {
                        val result = ArrayList<AccountEntity>()
                        for (account in accounts) {
                            if (account.name.lowercase().contains(searchValue.toString().lowercase())) {
                                result.add(account)
                            }
                        }
                        homeWalletController.setData(result)
                        views.accountListRecyclerView.configureWith(homeWalletController)
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

    }

    private fun insertNodesFromJson(){
        val nodesJson = Utils.getJsonDataFromAsset(this.requireContext(), "nodes.json")
        Thread {
            MeshRealm().addNodesFromJson(nodesJson)
        }.start()
    }

    private fun setupButton(){

        ViewAnimation.init(views.fabCreateWalletAccount)
        ViewAnimation.init(views.fabImportAccount)
        ViewAnimation.init(views.fabScanImportAccount)


        views.fabAdd.debouncedClicks {
            isFabAddClicked = ViewAnimation.rotateFab(views.fabAdd, !isFabAddClicked)
            if (isFabAddClicked){
                ViewAnimation.showIn(views.fabCreateWalletAccount)
                ViewAnimation.showIn(views.fabImportAccount)
                ViewAnimation.showIn(views.fabScanImportAccount)
            }else{
                ViewAnimation.showOut(views.fabCreateWalletAccount)
                ViewAnimation.showOut(views.fabImportAccount)
                ViewAnimation.showOut(views.fabScanImportAccount)
            }
        }

        views.fabCreateWalletAccount.debouncedClicks {
            val intent = WalletCreateActivity.newIntent(this.requireContext())
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
            startActivity(intent, options.toBundle())
        }

        views.fabImportAccount.debouncedClicks {
            importAccount()
        }

        views.fabScanImportAccount.debouncedClicks {
            doOpenQRCodeScanner()
        }

    }

    @SuppressLint("NewApi")
    private fun importAccount() {
        openJsonFileSelection(
                requireActivity(),
                importAccountActivityResultLauncher,
                false,
                0
        )
    }

    private val importAccountActivityResultLauncher = registerStartForActivityResult { it ->
        val data = it.data?.data ?: return@registerStartForActivityResult
        if (it.resultCode == Activity.RESULT_OK) {
            //val filename = getFilenameFromUri(requireContext(), data)

            val selectedFileJson =  requireContext().contentResolver.openInputStream(data);
            val fileString = selectedFileJson!!.bufferedReader().use { it.readText() }

            //for encryptedKey file there's no identifier so we can validate json immediately
            val isJson = StringUtil.isValidJson(fileString)

            if (!isJson){
                //check for mesh identifier
                if (!fileString.startsWith(MeshConstants.meshWalletQrIdentifier)){
                    Toast.makeText(requireContext(), "Not a valid Mesh account", Toast.LENGTH_SHORT).show()
                }else{
                    val payload = fileString.substringAfter(MeshConstants.meshWalletQrIdentifier)
                    val isJsonArray = StringUtil.isValidJsonArray(payload)

                    //confirm if payload is in json array
                    if (!isJsonArray){
                        Toast.makeText(requireContext(), "Not a valid Mesh account", Toast.LENGTH_SHORT).show()
                    }else{
                        Timber.d("payloadImport: $payload")
                        val gson = GsonBuilder().create()

                        val batchData = gson.fromJson(payload, Array<BatchAccountData>::class.java)

                        //make sure payload is complete and address is valid
                        if (batchData.any { it.address == null || it.encryptedKey == null || it.name == null}) {
                            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
                        }else if (!batchData.any{MeshAddressUtil.isValidMeshAddressString(it.address!!)}){
                            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_address), Toast.LENGTH_SHORT).show()
                        }else{
                            MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.vchat_import_wallet_title))
                                    .setMessage(getString(R.string.vchat_import_wallet_desc))
                                    .setPositiveButton(R.string.vchat_yes) { _, _ ->
                                        Thread{
                                            MeshRealm().addBatchAccountsFromArray(batchData)

                                            activity?.runOnUiThread{
                                                Toast.makeText(requireContext(), getString(R.string.vchat_import_wallet_success, batchData.size), Toast.LENGTH_SHORT).show()
                                                checkDataUpdated()
                                            }
                                        }.start()
                                    }
                                    .setNegativeButton(getString(R.string.vchat_cancel), null)
                                    .show()
                        }
                    }

                }
            }else{
                if (validateAccount(fileString)){
                    val toJson = Gson().fromJson(fileString, EncryptedAccountData::class.java)
                    accountNameDialog(toJson)
                }
            }
        }
    }

    private fun validateAccount(content: String): Boolean{

        val toJson: EncryptedAccountData?

        try {
             toJson = Gson().fromJson(content, EncryptedAccountData::class.java)
        }catch (e: JsonSyntaxException){
            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
            return false
        }
        //check the validity of json file
        if (toJson == null ||
                toJson.address.isEmpty() ||
                toJson.checksum.isEmpty() ||
                toJson.fullAddress.isEmpty() ||
                toJson.prefix.isEmpty() ||
                toJson.encryptedKey.cipher.isEmpty() ||
                toJson.encryptedKey.encryptedText.isEmpty() ||
                toJson.encryptedKey.keyChecksum.isEmpty()){

            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
        }else if (!MeshAddressUtil.isValidMeshAddressString(toJson.fullAddress)){
            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_address), Toast.LENGTH_SHORT).show()
        }else if (MeshRealm().getAccountByAddress(toJson.fullAddress) != null){
            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_duplicate_account), Toast.LENGTH_SHORT).show()
        }else{
            return true
        }

        return false

    }

    private fun accountNameDialog(encryptedKeyData: EncryptedAccountData) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val views = DialogBaseEditTextBinding.bind(layout)
        views.baseTil.hint = getString(R.string.vchat_account_name)

        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.vchat_account_name))
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create()

        dialog.setOnShowListener {
            val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            updateButton.debouncedClicks {
                val name = views.editText.text.toString()

                if (name.isEmpty()){
                    views.editText.error = getString(R.string.vchat_error_name_empty)
                }else {
                    Account.addAccountFromEncryptedAccountData(name, encryptedKeyData)
                    checkDataUpdated()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun prepareBatchAccountsJson(): String{

        val accounts = MeshRealm().getAccountsForBatchSave()

        val gson = GsonBuilder().serializeNulls().create()
        val accountsJson = gson.toJson(accounts)

        val toBatchData = gson.fromJson(accountsJson, Array<BatchAccountData>::class.java)

        val toBatchJson = gson.toJson(toBatchData)

        if (accounts.isNotEmpty()) {
            jsonWallet = "${MeshConstants.meshWalletQrIdentifier}$toBatchJson"
        }

        return jsonWallet
    }

    private fun doOpenQRCodeScanner() {
        QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
    }

    private val scanActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val scannedQrCode = QrCodeScannerActivity.getResultText(activityResult.data)
            val wasQrCode = QrCodeScannerActivity.getResultIsQrCode(activityResult.data)


            if (wasQrCode && !scannedQrCode.isNullOrBlank()) {

                //verify qr code contains mesh identifier
                if (scannedQrCode.startsWith(MeshConstants.meshEncryptedAccountQrIdentifier) || scannedQrCode.startsWith(MeshConstants.meshWalletQrIdentifier)) {
                    //determine batch or single account
                    if (scannedQrCode.startsWith(MeshConstants.meshWalletQrIdentifier)) {
                        val payload = scannedQrCode.substringAfter(MeshConstants.meshWalletQrIdentifier)
                        val isJsonArray = StringUtil.isValidJsonArray(payload)

                        //confirm if payload is in json array
                        if (!isJsonArray) {
                            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
                        } else {
                            val gson = GsonBuilder().create()
                            val batchData = gson.fromJson(payload, Array<BatchAccountData>::class.java)
                            //make sure payload is complete and address is a valid mesh address
                            when {
                                batchData.any { it.address == null || it.encryptedKey == null || it.name == null  } -> {
                                    Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
                                }
                                batchData.any {!MeshAddressUtil.isValidMeshAddressString(it.address!!)} -> {
                                    Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_address), Toast.LENGTH_SHORT).show()
                                }
                                else -> {

                                    MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(getString(R.string.vchat_import_wallet_title))
                                            .setMessage(getString(R.string.vchat_import_wallet_desc))
                                            .setPositiveButton(R.string.vchat_yes) { _, _ ->
                                                Thread {
                                                    MeshRealm().addBatchAccountsFromArray(batchData)

                                                    activity?.runOnUiThread {
                                                        Toast.makeText(requireContext(), getString(R.string.vchat_import_wallet_success, batchData.size), Toast.LENGTH_SHORT).show()
                                                        checkDataUpdated()
                                                    }
                                                }.start()
                                            }
                                            .setNegativeButton(R.string.vchat_cancel, null)
                                            .show()
                                }
                            }
                        }
                    } else {
                        val payload = scannedQrCode.substringAfter(MeshConstants.meshEncryptedAccountQrIdentifier)
                        val isJson = StringUtil.isValidJson(payload)

                        //confirm if payload is in json format
                        if (!isJson) {
                            Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
                        } else if (validateAccount(payload)) {
                            val toJson = Gson().fromJson(payload, EncryptedAccountData::class.java)
                            accountNameDialog(toJson)
                        }
                    }
                }else{
                    Toast.makeText(requireContext(), getString(R.string.vchat_error_import_invalid_mesh_account), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.vchat_wallet_account_scan_qr_error_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSaveWalletBottomDialog(){
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_save_wallet, null)

        val shareWalletView = view.findViewById<LinearLayout>(R.id.llShareWallet)
        val saveWalletView = view.findViewById<LinearLayout>(R.id.llSaveWallet)
        val saveQrView = view.findViewById<LinearLayout>(R.id.llSaveQr)
        val accountsJson = prepareBatchAccountsJson()

        shareWalletView.debouncedClicks {

            if (accountsJson != ""){
                val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val filename = "mesh-wallet-${timestamp}"
                val file = Utils.createJsonFile(requireContext(), accountsJson, filename)
                shareMedia(requireContext(), file, getMimeTypeFromUri(requireContext(), file.toUri()))

                Utils.deleteJsonFile(requireContext(), filename)
                dialog.dismiss()
            }else{
                Toast.makeText(requireContext(), getString(R.string.vchat_error_no_account_to_share), Toast.LENGTH_SHORT).show()
            }

        }

        saveWalletView.debouncedClicks {

            if (accountsJson != ""){
                val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val filename = "mesh-wallet-${timestamp}"
                Utils.saveJsonFile(
                        activity = requireActivity(),
                        activityResultLauncher = saveRecoveryActivityResultLauncher,
                        defaultFileName = filename,
                        chooserHint = getString(R.string.vchat_home_wallet_menu_save_accounts)
                )
                dialog.dismiss()
            }else{
                Toast.makeText(requireContext(), getString(R.string.vchat_error_no_account_to_save), Toast.LENGTH_SHORT).show()
            }
        }

        saveQrView.debouncedClicks {

            if (accountsJson != "" && accountsJson.length < 1600) {
                val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val filename = "mesh-wallet-${timestamp}"

                val qrBitmap = accountsJson.toBitMatrix(480).toBitmap()
                val drawTextToQr = ViewUtil.drawTextToBitmap(requireContext(), qrBitmap, "Mesh Wallet $timestamp")
                saveBitmap(drawTextToQr, filename)
                dialog.dismiss()
            }else if (accountsJson == ""){
                Toast.makeText(requireContext(), getString(R.string.vchat_error_no_account_to_save), Toast.LENGTH_SHORT).show()
            }else if (accountsJson.length > 1600){
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.vchat_error_cannot_save_as_qr_title))
                        .setMessage(getString(R.string.vchat_error_cannot_save_as_qr_desc))
                        .setPositiveButton(getString(R.string.vchat_ok), null)
                        .show()
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private val saveRecoveryActivityResultLauncher = registerStartForActivityResult { activityResult ->
        val uri = activityResult.data?.data ?: return@registerStartForActivityResult
        if (activityResult.resultCode == Activity.RESULT_OK) {
            exportRecoveryKeyToFile(uri, jsonWallet)
        }
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
                                            .setMessage(R.string.vchat_create_mesh_wallet_export_saved)
                                }
                            }
                    )
                    ?.setCancelable(false)
                    ?.setPositiveButton(R.string.ok, null)
                    ?.show()
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
                    .setMessage(R.string.vchat_create_mesh_wallet_export_saved_qr_code)
                    .show()

        } catch (e: Exception) {
            Timber.d( e.toString()) // java.io.IOException: Operation not permitted
        }
    }

    private fun checkDataUpdated(){
        val latestAccounts = MeshRealm().getAccountsList()

        if (accounts.size != latestAccounts.size){
            accounts = latestAccounts
            setupSearchView()
        }else if (accounts.isNotEmpty() && latestAccounts.isNotEmpty()){
            latestAccounts.forEachIndexed { index, latest ->

                if (latest.address != accounts[index].address || latest.name != accounts[index].name) {
                    accounts = latestAccounts
                    setupSearchView()
                    return
                }

            }
        }
    }
}
