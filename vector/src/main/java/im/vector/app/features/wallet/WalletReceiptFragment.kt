package im.vector.app.features.wallet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.airbnb.mvrx.args
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vcard.vchat.utils.Constants
import com.vcard.vchat.utils.StringUtil
import com.vcard.vchat.utils.Utils
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletReceiptBinding
import timber.log.Timber
import java.lang.Exception
import java.math.BigDecimal
import javax.inject.Inject

class WalletReceiptFragment @Inject constructor(
) : VectorBaseFragment<FragmentWalletReceiptBinding>() {
    private val fragmentArgs: WalletReceiptActivity.Args by args()

    private lateinit var callback: ReceiptCallback


    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletReceiptBinding {

        return FragmentWalletReceiptBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ReceiptCallback
    }

    private fun setupViews(){
        val convertedRate = getRateByUnit(fragmentArgs.selectedUnit)
        val convertedAmount =  BigDecimal(fragmentArgs.amount).divide(convertedRate)

        val amount = "${StringUtil.formatBalanceForDisplayBigDecimal(convertedAmount)} ${fragmentArgs.selectedUnit}"
        views.tvAmountTitle.text = amount


        val dateView = Utils.convertTimeToView(fragmentArgs.date, "dd-MM-yyyy-HH:mm:ss")
        views.tvTime.text = dateView
        views.tvRecipientAddress.text = fragmentArgs.recipientAddress
        views.tvAmountDetail.text = amount

        val convertedFee = (fragmentArgs.fee.toDouble()/com.vcard.vchat.mesh.Constants.milligramRate)
        Timber.d("fee: ${fragmentArgs.fee}, convertedFee: $convertedFee")

        val fee = "$convertedFee ${com.vcard.vchat.mesh.Constants.milligramUnit}"
        views.tvFeeDetail.text = fee
        views.tvReference.text = fragmentArgs.reference
    }

    private fun setupClick(){
        views.btnDone.debouncedClicks {
            callback.onDone()
        }

        views.tvRecipientAddress.debouncedClicks {
            showFullAddressBottomDialog()
        }
    }

    private fun showFullAddressBottomDialog(){
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_wallet_code_address, null)
        val viewContentTitle = view.findViewById<TextView>(R.id.bottomSheetWalletTitle)
        viewContentTitle.text = getString(R.string.vchat_wallet_recipient_address)

        val viewContent = view.findViewById<TextView>(R.id.bottomSheetWalletContent)
        viewContent.text = fragmentArgs.recipientAddress

        dialog.setContentView(view)
        dialog.show()
    }

    private fun getRateByUnit(unit: String): BigDecimal{
        return when(unit){
            com.vcard.vchat.mesh.Constants.kilogramUnit -> BigDecimal(com.vcard.vchat.mesh.Constants.kilogramRate.toString())
            com.vcard.vchat.mesh.Constants.gramUnit -> BigDecimal(com.vcard.vchat.mesh.Constants.gramRate.toString())
            com.vcard.vchat.mesh.Constants.milligramUnit -> BigDecimal(com.vcard.vchat.mesh.Constants.milligramRate.toString())
            com.vcard.vchat.mesh.Constants.microgramUnit -> BigDecimal(com.vcard.vchat.mesh.Constants.microgramRate.toString())
            com.vcard.vchat.mesh.Constants.nanogramUnit -> BigDecimal(com.vcard.vchat.mesh.Constants.nanogramRate.toString())
            else -> throw Exception("Not a valid unit")
        }
    }

    interface ReceiptCallback {
        fun onDone()
    }
}
