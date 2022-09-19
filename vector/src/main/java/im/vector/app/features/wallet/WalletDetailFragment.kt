package im.vector.app.features.wallet

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import com.airbnb.mvrx.args
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.vcard.vchat.mesh.Address
import com.vcard.vchat.mesh.Aes256
import com.vcard.vchat.mesh.Constants
import com.vcard.vchat.mesh.MeshCommand
import com.vcard.vchat.mesh.NumberUtil
import com.vcard.vchat.mesh.data.MeshAccountData
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import com.vcard.vchat.utils.StringUtil
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.FragmentWalletDetailBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.qrcode.QrCodeScannerActivity
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

class WalletDetailFragment @Inject constructor(
): VectorBaseFragment<FragmentWalletDetailBinding>() {

    private val fragmentArgs: WalletDetailsArgs by args()

    private var accountBalance = 0.0
    private var displayUnit = Constants.gramUnit


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
        setupSubmitButton()
        setupAccount()
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

            val conversionRate = if (displayUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate

            accountBalance = accountData.balance.toDouble()
            Timber.d("accountBalance: ${accountData.balance}")
            val convertedBalance = accountBalance/conversionRate
            val formattedBalance = StringUtil.formatBalanceForDisplay(convertedBalance)
            val currentBalance = "$formattedBalance $displayUnit"
            views.walletDetailHeaderSubtitle.text = currentBalance
        }else{
            views.btnRefreshAccount.performClick()
        }
    }



    private fun setupSubmitButton() {
        views.btnScanQRCode.setOnClickListener {
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

            if (fragmentArgs.type == "test") {
                views.walletDetailHeaderSubtitle.visibility = View.INVISIBLE
                views.simpleActivityWaitingView.visibility = View.VISIBLE

                Thread {
                    val refreshAccount = MeshCommand.getAccount(fragmentArgs.address, fragmentArgs.privateKey)
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

                                Timber.d("decodeParams: $decodeParamsUtf8")

                                val accountData = Gson().fromJson(decodeParamsUtf8, MeshAccountData::class.java)
                                Timber.d("accountData: ${accountData.nonce}")
                                val accountEntity = AccountEntity()
                                accountEntity.address = accountData.address
                                accountEntity.balance = accountData.balance
                                accountEntity.currency = accountData.currency
                                accountEntity.nonce = accountData.nonce
                                accountEntity.moduleHash = accountData.moduleHash?.toByteArray()
                                accountEntity.rootHash = accountData.rootHash.toByteArray()

                                RealmExec().addUpdateAccountBalance(accountEntity)

                                val conversionRate = if (displayUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate

                                accountBalance = accountData.balance.toDouble()

                                val convertedBalance = accountBalance/conversionRate
                                val formattedBalance = StringUtil.formatBalanceForDisplay(convertedBalance)
                                val currentBalance = "$formattedBalance $displayUnit"
                                views.walletDetailHeaderSubtitle.text = currentBalance
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

                                val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
                                val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                                Timber.d("decodeParams: $decodeParamsUtf8")

                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_update_account_fail), Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }.start()
            }else{
                //for encrypted account we need to input passphrase
                inputPassphrase(fragmentArgs.encryptedKey)
            }
        }

        views.walletDetailHeaderSubtitle.debouncedClicks {

            //switch display
            displayUnit = if (displayUnit == Constants.gramUnit)  Constants.milligramUnit else Constants.gramUnit

            val conversionRate = if (displayUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate


            val convertedBalance = accountBalance/conversionRate
            val formattedBalance = StringUtil.formatBalanceForDisplay(convertedBalance)
            val currentBalance = "$formattedBalance $displayUnit"
            views.walletDetailHeaderSubtitle.text = currentBalance

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
                if (validateQRCode(scannedQrCode)){
                    val qrPrefix = "MESH"
                    val qrAddress = scannedQrCode.substringAfter(qrPrefix)
                    val intent = WalletTransferActivity.newIntent(this.requireContext(), fragmentArgs.address, qrAddress, fragmentArgs.privateKey, fragmentArgs.encryptedKey, fragmentArgs.type)
                    startActivity(intent)
//                    val selectedNodes = RealmExec().getNodesForElection(fragmentArgs.address)
//                    if (selectedNodes != null) {
//                        Timber.d("selected ${selectedNodes.size} nodes")
//                    }
                }
            } else {
                Timber.w("It was not a QR code, or empty result")
            }
        }
    }

    private fun validateQRCode(scannedQrCode: String): Boolean{
        val qrPrefix = "MESH"
        val qrAddress = scannedQrCode.substringAfter(qrPrefix)
        if (!scannedQrCode.startsWith(qrPrefix) && !Address.isValidMeshAddressString(qrAddress)){
            Toast.makeText(requireContext(), getString(R.string.vchat_wallet_account_scan_qr_error_invalid), Toast.LENGTH_SHORT).show()
            return false
        }else if(qrAddress == fragmentArgs.address){
            Toast.makeText(requireContext(), getString(R.string.vchat_wallet_account_scan_qr_error_yourself), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun inputPassphrase(encryptedKey: String) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val dialogViews = DialogBaseEditTextBinding.bind(layout)
        dialogViews.editText.hint = "Input passphrase"

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Input passphrase")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ ->

                    val passphrase = dialogViews.editText.text.toString()

                    val keyBytes = NumberUtil.hexStrToBytes(encryptedKey)
                    val decryptedKey = Aes256.decryptGcm(keyBytes, passphrase)

                    if (decryptedKey == "invalid passphrase"){
                        Toast.makeText(requireContext(), "Invalid passphrase", Toast.LENGTH_SHORT).show()
                    }else {
                        views.walletDetailHeaderSubtitle.visibility = View.INVISIBLE
                        views.simpleActivityWaitingView.visibility = View.VISIBLE

                        Thread {
                            Timber.d("address: ${fragmentArgs.address}")

                            val refreshAccount = MeshCommand.getAccount(fragmentArgs.address, decryptedKey)
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

                                        Timber.d("decodeParams: $decodeParamsUtf8")

                                        val accountData = Gson().fromJson(decodeParamsUtf8, MeshAccountData::class.java)
                                        Timber.d("accountData: ${accountData.nonce}")
                                        val accountEntity = AccountEntity()
                                        accountEntity.address = accountData.address
                                        accountEntity.balance = accountData.balance
                                        accountEntity.currency = accountData.currency
                                        accountEntity.nonce = accountData.nonce
                                        accountEntity.moduleHash = accountData.moduleHash?.toByteArray()
                                        accountEntity.rootHash = accountData.rootHash.toByteArray()

                                        RealmExec().addUpdateAccountBalance(accountEntity)

                                        val conversionRate = if (displayUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate

                                        accountBalance = accountData.balance.toDouble()

                                        val convertedBalance = accountBalance/conversionRate
                                        val formattedBalance = StringUtil.formatBalanceForDisplay(convertedBalance)
                                        val currentBalance = "$formattedBalance $displayUnit"
                                        views.walletDetailHeaderSubtitle.text = currentBalance
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

                                        val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
                                        val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                                        Timber.d("decodeParams: $decodeParamsUtf8")

                                        Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_update_account_fail), Toast.LENGTH_SHORT).show()
                                    })
                                }
                            }
                        }.start()
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }
}
