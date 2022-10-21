package im.vector.app.features.wallet

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.args
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.vcard.vchat.mesh.Address
import com.vcard.vchat.mesh.Aes256
import com.vcard.vchat.mesh.Constants
import com.vcard.vchat.mesh.CurrencyEnum
import com.vcard.vchat.mesh.MeshCommand
import com.vcard.vchat.mesh.NumberUtil
import com.vcard.vchat.mesh.TxnFee
import com.vcard.vchat.mesh.data.MutxoListData
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import com.vcard.vchat.utils.DecimalDigitsInputFilter
import com.vcard.vchat.utils.MeshSharedPref
import com.vcard.vchat.utils.StringUtil
import com.vcard.vchat.utils.Utils
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.DialogBaseInputPassphraseBinding
import im.vector.app.databinding.FragmentWalletTransferBinding
import timber.log.Timber
import java.lang.Exception
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

class WalletTransferFragment@Inject constructor(
) : VectorBaseFragment<FragmentWalletTransferBinding>() {

    private val fragmentArgs: WalletTransferActivity.Args by args()

    private lateinit var callback: TransferCallback

    private lateinit var senderAccount: AccountEntity


    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletTransferBinding {
        setHasOptionsMenu(true)

        return FragmentWalletTransferBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
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

        val items = listOf(Constants.kilogramUnit, Constants.gramUnit, Constants.milligramUnit, Constants.microgramUnit,  Constants.nanogramUnit)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_transfer_unit_list, items)
        views.unitTextView.setAdapter(adapter)

        //defaults to milligram
        views.unitTextView.setText(items[2], false)

        senderAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

        displayCurrentBalance()
        limitDecimalInput()
    }

    private fun setupListeners(){

        //update balance view
        views.unitTextView.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            displayCurrentBalance()
            setInputType()
            formatInputAmount()
            limitDecimalInput()
        }



        views.transferAmountInput.addTextChangedListener(object : TextWatcher{

            var isDecimalInputted = false
            var preChangedText = ""
            var changedText = ""

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                preChangedText = p0.toString()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                changedText = p0.toString()
                isDecimalInputted = !preChangedText.contains(".") && changedText.contains(".")
                Timber.d("onTextChanged p1: $p1, p2: $p2, p3: $p3")
            }

            override fun afterTextChanged(p0: Editable?) {
                val inputText = p0.toString()

                if (inputText.isNotEmpty() && !isDecimalInputted && inputText[inputText.lastIndex].toString() != ".")  {

                    val amountInput = inputText.replace(",", "").toDouble()
                    if (amountInput >= 1000) {
                        views.transferAmountInput.removeTextChangedListener(this)

                        val formatInput = StringUtil.formatBalanceForDisplay(amountInput)

                        val endsWithZero = inputText.endsWith("0")
                        if (endsWithZero && inputText.contains(".")) {
                            val concatWithZero = formatInput.plus("0")
                            views.transferAmountInput.setText(concatWithZero)
                            views.transferAmountInput.setSelection(concatWithZero.length)
                        }else{
                            views.transferAmountInput.setText(formatInput)
                            views.transferAmountInput.setSelection(formatInput.length)
                        }

                        views.transferAmountInput.addTextChangedListener(this)
                    }else if (inputText.contains(",") && amountInput < 1000){
                        views.transferAmountInput.removeTextChangedListener(this)

                        views.transferAmountInput.setText(inputText.replace(",", ""))
                        views.transferAmountInput.setSelection(inputText.length-1)

                        views.transferAmountInput.addTextChangedListener(this)

                    }
                }
            }
        })
    }

    private fun setupClick(){
        views.btnTransfer.debouncedClicks {

            if (views.transferAmountInput.text.toString() == ""){
                views.transferAmountInput.error = "Please enter an amount"
                return@debouncedClicks
            }

            //val senderAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

            val selectedUnit = views.unitTextView.text.toString()
            val conversionRate = getRateByUnit(selectedUnit)

            val amountInput = views.transferAmountInput.text.toString()

            val bigDecimalInput = BigDecimal(amountInput.replace(",", ""))
            val calcAmount = bigDecimalInput * conversionRate
            val amount = calcAmount.toBigInteger()
            val fee = TxnFee.calculateTotalFeeBigInt(CurrencyEnum.MeshGold, amount)

            //use divide to prevent rounding up
            val convertedFee = BigDecimal(fee).divide(BigDecimal(Constants.milligramRate))

            val feeForDisplay = "$convertedFee ${Constants.milligramUnit}"
            val reference = views.transferReferenceInput.text.toString()

            Timber.d("bigDecimal:  $bigDecimalInput calcAmount: $calcAmount, amount: $amount, fee: $fee, convertedFee: $convertedFee, convertRate: $conversionRate")

            if (amount + fee > BigInteger(senderAccount.balance.toString())){
                Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
            }else{
                if (fragmentArgs.type == Constants.test) {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Transfer summary")
                            .setMessage("Recipient: ${fragmentArgs.recipientAddress}\n" +
                                    "Amount: $amountInput $selectedUnit\n" +
                                    "Fee: $feeForDisplay\n" +
                                    "Reference: $reference\n")
                            .setPositiveButton("Confirm transfer") { _, _ ->
                                val loadingDialog = prepareDialogLoader(requireContext())
                                loadingDialog.show()

                                Thread {

//                        if (getAccountBalance(fragmentArgs.senderPrivateKey) == "ok") {

                                    val updatedAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

                                    if (amount + fee > BigInteger(updatedAccount.balance.toString())){
                                        activity?.runOnUiThread(Runnable {
                                            loadingDialog.dismiss()
                                            Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                                        })
                                    }else {

                                        val date = Utils.getTime("UTC")

                                        val sendTransaction = MeshCommand.sendTransaction(
                                                fragmentArgs.senderAddress,
                                                fragmentArgs.senderPrivateKey,
                                                fragmentArgs.recipientAddress,
                                                amount,
                                                reference,
                                                date
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
//                        }else{
//                            loadingDialog.dismiss()
//                        }
                                }.start()                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                }else{
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Transfer summary")
                            .setMessage("Recipient: ${fragmentArgs.recipientAddress}\n" +
                                        "Amount: $amountInput $selectedUnit\n" +
                                        "Fee: $feeForDisplay\n" +
                                        "Reference: $reference\n")
                            .setPositiveButton("Confirm transfer") { _, _ ->
                                val msp = MeshSharedPref(requireContext())
                                val pp = msp.getPp(fragmentArgs.senderAddress)
                                if (pp == "") {
                                    inputPassphrase(fragmentArgs.encryptedKey, amount, fee, selectedUnit)
                                }else{
                                    val keyBytes = NumberUtil.hexStrToBytes(fragmentArgs.encryptedKey)
                                    val dk = Aes256.decryptGcm(keyBytes, pp)
                                    sendMutxo(dk,  amount, fee, selectedUnit)
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
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

    private fun inputPassphrase(encryptedKey: String, amount: BigInteger, fee: BigInteger, selectedUnit: String) {
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
                val pp = dialogViews.editText.text.toString()

                val keyBytes = NumberUtil.hexStrToBytes(encryptedKey)
                val dk = Aes256.decryptGcm(keyBytes, pp)

                if (dk == "invalid passphrase") {
                    dialogViews.baseTil.error = getString(R.string.vchat_error_invalid_pass)
                } else {
                    val msp = MeshSharedPref(requireContext())
                    msp.storePp(fragmentArgs.senderAddress, pp)
                    sendMutxo(dk, amount, fee, selectedUnit)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun sendMutxo(dk: String, amount: BigInteger, fee: BigInteger, selectedUnit: String){
        val loadingDialog = prepareDialogLoader(requireContext())
        loadingDialog.show()

        Thread {

//           if (getAccountBalance(dk) == "ok") {

                val senderAccount = RealmExec().getAccountByAddress(fragmentArgs.senderAddress)!!

                if (amount + fee > BigInteger(senderAccount.balance.toString())){
                    activity?.runOnUiThread(Runnable {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                    })
                }else {

                    val date = Utils.getTime("UTC")
                    val reference = views.transferReferenceInput.text.toString()

                    val sendTransaction = MeshCommand.sendTransaction(
                            fragmentArgs.senderAddress,
                            dk,
                            fragmentArgs.recipientAddress,
                            BigInteger(amount.toString()),
                            reference,
                            date
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
//           }else{
//                loadingDialog.dismiss()
//           }
        }.start()
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

                    Timber.d("decodeParams: $decodeParamsUtf8\n")
                    val gson = GsonBuilder().disableHtmlEscaping().create()
                    val accountData = gson.fromJson(decodeParamsUtf8, MutxoListData::class.java)

                    val address = Address.createFullAddress(accountData.ownerAddress.prefix, accountData.ownerAddress.address, accountData.ownerAddress.checksum)

                    Timber.d("accountData: ${gson.toJson(accountData)}")
                    val accountEntity = AccountEntity()
                    accountEntity.address = address
                    accountEntity.balance = accountData.total
                    accountEntity.currency = CurrencyEnum.MeshGold.name

                    RealmExec().addUpdateAccountBalance(accountEntity)
                    RealmExec().addAccountMutxoFromMap(address, accountData.mutxoList)
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

    private fun displayCurrentBalance(){
        val selectedUnit = views.unitTextView.text.toString()
        val conversionRate = getRateByUnit(selectedUnit)
        val convertedBalance = BigDecimal(senderAccount.balance).divide(conversionRate)
        val formattedBalance = StringUtil.formatBalanceForDisplayBigDecimal(convertedBalance)
        val currentBalance = "$formattedBalance $selectedUnit"

        views.transferAmountLayout.helperText = "Current balance: $currentBalance"
    }

    private fun getRateByUnit(unit: String): BigDecimal{
        return when(unit){
            Constants.kilogramUnit -> BigDecimal(Constants.kilogramRate.toString())
            Constants.gramUnit -> BigDecimal(Constants.gramRate.toString())
            Constants.milligramUnit -> BigDecimal(Constants.milligramRate.toString())
            Constants.microgramUnit -> BigDecimal(Constants.microgramRate.toString())
            Constants.nanogramUnit -> BigDecimal(Constants.nanogramRate.toString())
            else -> throw Exception("Not a valid unit")
        }
    }

    private fun setInputType(){
        val selectedUnit = views.unitTextView.text.toString()
        if (selectedUnit == Constants.nanogramUnit) {
            views.transferAmountInput.inputType = InputType.TYPE_CLASS_NUMBER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                views.transferAmountInput.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
            }else{
                @Suppress("DEPRECATION")
                views.transferAmountInput.keyListener = DigitsKeyListener.getInstance(false, false)
            }
        } else{
            views.transferAmountInput.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                views.transferAmountInput.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
            }else{
                @Suppress("DEPRECATION")
                views.transferAmountInput.keyListener = DigitsKeyListener.getInstance(false, true)
            }

        }
    }

    private fun formatInputAmount(){
        val selectedUnit = views.unitTextView.text.toString()
        val currentInputAmount = views.transferAmountInput.text.toString()

        when(selectedUnit){
            Constants.gramUnit ->{
                //9 decimal places
                removeExceedingDecimals(currentInputAmount, Constants.gramRate)
            }
            Constants.milligramUnit -> {
                //6 decimal places
                removeExceedingDecimals(currentInputAmount, Constants.milligramRate)
            }
            Constants.microgramUnit -> {
                //3 decimal places
                removeExceedingDecimals(currentInputAmount, Constants.microgramRate)
            }
            Constants.nanogramUnit -> {
                //no decimals
                val substring = currentInputAmount.substringBefore(".")
                views.transferAmountInput.setText(substring)
            }
        }
    }

    private fun removeExceedingDecimals(input: String, rate: Int){
        val decimalSubstring = input.substringAfter(".")

        Timber.d("decimalSubLength: ${decimalSubstring.length} , rateLength: ${rate.toString().length}")
        if (decimalSubstring.length > rate.toString().length-1){
            val endIndex = (input.indexOf(".")+rate.toString().length)
            views.transferAmountInput.setText(input.substring(0, endIndex))
        }
    }

    private fun limitDecimalInput(){

        when(views.unitTextView.text.toString()){
            Constants.kilogramUnit ->{
                //12 decimal places
                views.transferAmountInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(Constants.kilogramRate.toString().length-1))
            }

            Constants.gramUnit ->{
                //9 decimal places
                views.transferAmountInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(Constants.gramRate.toString().length-1))

        }
            Constants.milligramUnit -> {
                //6 decimal places
                views.transferAmountInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(Constants.milligramRate.toString().length-1))
            }
            Constants.microgramUnit -> {
                //3 decimal places
                views.transferAmountInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(Constants.microgramRate.toString().length-1))
            }
        }
    }
    interface TransferCallback {
        fun onTransactionSuccess()
    }

}
