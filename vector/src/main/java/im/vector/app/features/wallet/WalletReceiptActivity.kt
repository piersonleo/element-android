package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import kotlinx.parcelize.Parcelize
import im.vector.app.databinding.ActivitySimpleBinding
import timber.log.Timber
import java.math.BigInteger

@AndroidEntryPoint
class WalletReceiptActivity: VectorBaseActivity<ActivitySimpleBinding>(), WalletReceiptFragment.ReceiptCallback {

    @Parcelize
    data class Args(
            val date: String,
            val recipientAddress: String,
            val amount: BigInteger,
            val fee: BigInteger,
            val selectedUnit: String,
            val reference: String
    ) : Parcelable

    companion object {
        fun newIntent(context: Context, date: String, recipientAddress: String, amount: BigInteger, fee: BigInteger, selectedUnit: String, reference: String): Intent {
            return Intent(context, WalletReceiptActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(date, recipientAddress, amount, fee, selectedUnit, reference))
            }
        }
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)

    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, WalletReceiptFragment::class.java, intent?.extras?.getParcelable(Mavericks.KEY_ARG))
        }
    }

    override fun onDone() {
        finish()
    }
}
