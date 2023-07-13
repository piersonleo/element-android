package im.vector.app.features.wallet

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.airbnb.mvrx.args
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.vcard.vchat.mesh.Aes256
import com.vcard.vchat.mesh.Constants
import com.vcard.vchat.mesh.CurrencyEnum
import com.vcard.vchat.mesh.MeshCommand
import com.vcard.vchat.mesh.NumberUtil
import com.vcard.vchat.mesh.TxnFee
import com.vcard.vchat.mesh.data.MeshAccountData
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import com.vcard.vchat.utils.StringUtil
import com.vcard.vchat.utils.Utils
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.FragmentWalletTransferBinding
import im.vector.app.features.userdirectory.UserListController
import timber.log.Timber
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class WalletTransferFragment@Inject constructor(
) : VectorBaseFragment<FragmentWalletTransferBinding>() {

    private val fragmentArgs: WalletTransferActivity.Args by args()

    private lateinit var callback: TransferCallback


    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletTransferBinding {
        setHasOptionsMenu(true)

        return FragmentWalletTransferBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as TransferCallback
    }

    private fun setupViews(){
        setupToolbar(views.walletTransferToolbar)
                .setTitle("Transfer Gold")
                .allowBack(useCross = false)

        views.transferAddressText.setText(fragmentArgs.recipientAddress)

        val items = listOf(Constants.gramUnit, Constants.milligramUnit)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_transfer_unit_list, items)
        views.unitTextView.setAdapter(adapter)
        views.unitTextView.setText(items[0], false)
    }

    private fun setupClick(){
        views.btnTransfer.debouncedClicks {

            if (views.transferAmountInput.text.toString() == ""){
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@debouncedClicks
            }

            val senderAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

            val selectedUnit = views.unitTextView.text.toString()
            val convertRate = if (selectedUnit == Constants.gramUnit) Constants.gramRate else Constants.milligramRate
            val amount = views.transferAmountInput.text.toString().toLong() * convertRate
            val fee = TxnFee.calculateTotalFee(CurrencyEnum.MeshGold, amount).toLong()

            Timber.d("amount: $amount, fee: $fee, convertRate: $convertRate")

            if (amount + fee > senderAccount.balance){
                Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
            }else{
                if (fragmentArgs.type == "test") {

                    val reference = views.transferReferenceInput.text.toString()

                    val loadingDialog = prepareDialogLoader(requireContext())
                    loadingDialog.show()

                    Thread {

                        if (getAccountBalance(fragmentArgs.senderPrivateKey) == "ok") {

                            val updatedAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

                            if (amount + fee > updatedAccount.balance){
                                activity?.runOnUiThread(Runnable {
                                    loadingDialog.dismiss()
                                    Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                                })
                            }else {
                                val sendTransaction = MeshCommand.sendTransaction(
                                        fragmentArgs.senderAddress,
                                        fragmentArgs.senderPrivateKey,
                                        fragmentArgs.recipientAddress,
                                        amount,
                                        reference,
                                        updatedAccount.nonce.toLong()
                                )

                                when (sendTransaction.c) {
                                    "unavailable" -> {
                                        activity?.runOnUiThread(Runnable {
                                            loadingDialog.dismiss()
                                            Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_unavailable), Toast.LENGTH_SHORT).show()
                                        })
                                    }
                                    "ok"          -> {
                                        activity?.runOnUiThread(Runnable {
                                            Timber.d("transaction data: $sendTransaction")
                                            loadingDialog.dismiss()
                                            val date = Utils.getTime("UTC")
                                            val intent = WalletReceiptActivity.newIntent(
                                                    this.requireContext(),
                                                    date,
                                                    fragmentArgs.recipientAddress,
                                                    amount,
                                                    fee,
                                                    selectedUnit,
                                                    reference
                                            )
                                            startActivity(intent)
                                            callback.onTransactionSuccess()
                                        })
                                    }
                                    "busy"        -> {
                                        activity?.runOnUiThread(Runnable {
                                            loadingDialog.dismiss()
                                            Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_busy), Toast.LENGTH_SHORT).show()
                                        })
                                    }
                                    else          -> {
                                        activity?.runOnUiThread(Runnable {
                                            loadingDialog.dismiss()
                                            Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.vchat_grpc_error_send_transaction_fail),
                                                    Toast.LENGTH_SHORT
                                            ).show()

                                            val decodeParams = Base64.decode(sendTransaction.d, Base64.DEFAULT)
                                            val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                                            Timber.d("decodeParams: $decodeParamsUtf8")
                                        })
                                    }
                                }
                            }
                        }else{
                            loadingDialog.dismiss()
                        }
                    }.start()
                }else{
                    inputPassphrase(fragmentArgs.encryptedKey, amount, fee, selectedUnit)
                }
            }
        }
    }

    private fun prepareDialogLoader(context: Context): Dialog {
        val dialog = Dialog(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_dialog_loading)
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        tvMessage.text = getString(R.string.vchat_send_transaction_progress)

        return dialog
    }

    private fun inputPassphrase(encryptedKey: String, amount: Long, fee: Long, selectedUnit: String) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val dialogViews = DialogBaseEditTextBinding.bind(layout)
        dialogViews.editText.hint = "Input passphrase"

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Input passphrase")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val reference = views.transferReferenceInput.text.toString()

                    val passphrase = dialogViews.editText.text.toString()

                    val keyBytes = NumberUtil.hexStrToBytes(encryptedKey)
                    val decryptedKey = Aes256.decryptGcm(keyBytes, passphrase)

                    if (decryptedKey == "invalid passphrase"){
                        Toast.makeText(requireContext(), "Invalid passphrase", Toast.LENGTH_SHORT).show()
                    }else {
                        val loadingDialog = prepareDialogLoader(requireContext())
                        loadingDialog.show()

                        Thread {

                            if (getAccountBalance(decryptedKey) == "ok") {

                                val senderAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

                                if (amount + fee > senderAccount.balance){
                                    activity?.runOnUiThread(Runnable {
                                        loadingDialog.dismiss()
                                        Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                                    })
                                }else {

                                    val sendTransaction = MeshCommand.sendTransaction(
                                            fragmentArgs.senderAddress,
                                            decryptedKey,
                                            fragmentArgs.recipientAddress,
                                            amount,
                                            reference,
                                            senderAccount.nonce.toLong()
                                    )

                                    when (sendTransaction.c) {
                                        "unavailable" -> {
                                            activity?.runOnUiThread(Runnable {
                                                loadingDialog.dismiss()
                                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_unavailable), Toast.LENGTH_SHORT).show()
                                            })
                                        }
                                        "ok"          -> {
                                            activity?.runOnUiThread(Runnable {
                                                Timber.d("transaction data: $sendTransaction")
                                                loadingDialog.dismiss()
                                                val date = Utils.getTime("UTC")
                                                val intent = WalletReceiptActivity.newIntent(
                                                        this.requireContext(),
                                                        date,
                                                        fragmentArgs.recipientAddress,
                                                        amount,
                                                        fee,
                                                        selectedUnit,
                                                        reference
                                                )
                                                startActivity(intent)
                                                callback.onTransactionSuccess()
                                            })
                                        }
                                        "busy"        -> {
                                            activity?.runOnUiThread(Runnable {
                                                loadingDialog.dismiss()
                                                Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_busy), Toast.LENGTH_SHORT).show()
                                            })
                                        }
                                        else          -> {
                                            activity?.runOnUiThread(Runnable {
                                                loadingDialog.dismiss()
                                                Toast.makeText(
                                                        requireContext(),
                                                        getString(R.string.vchat_grpc_error_send_transaction_fail),
                                                        Toast.LENGTH_SHORT
                                                ).show()

                                                val decodeParams = Base64.decode(sendTransaction.d, Base64.DEFAULT)
                                                val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                                                Timber.d("decodeParams: $decodeParamsUtf8")
                                            })
                                        }
                                    }
                                }
                            }else{
                                loadingDialog.dismiss()
                            }
                        }.start()
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    private fun getAccountBalance(key: String): String{

        val refreshAccount = MeshCommand.getAccount(fragmentArgs.senderAddress, key)
        Timber.d("account data: $refreshAccount")

        when (refreshAccount.c) {
            "unavailable" -> {

                activity?.runOnUiThread(Runnable {
                    Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_unavailable), Toast.LENGTH_SHORT).show()
                })

                return "unavailable"
            }
            "ok"          -> {
                activity?.runOnUiThread(Runnable {
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
                })

                return "ok"
            }
            "busy"        -> {
                activity?.runOnUiThread(Runnable {
                    Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_busy), Toast.LENGTH_SHORT).show()
                })

                return "busy"
            }
            else          -> {
                activity?.runOnUiThread(Runnable {
                    val decodeParams = Base64.decode(refreshAccount.d, Base64.DEFAULT)
                    val decodeParamsUtf8 = decodeParams.toString(StandardCharsets.UTF_8)

                    Timber.d("decodeParams: $decodeParamsUtf8")

                    Toast.makeText(requireContext(), getString(R.string.vchat_grpc_error_update_account_fail), Toast.LENGTH_SHORT).show()
                })

                return "error"
            }
        }
    }
    interface TransferCallback {
        fun onTransactionSuccess()
    }

}
