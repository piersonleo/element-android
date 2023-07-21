package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.WalletDetailsArgs
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@AndroidEntryPoint
class WalletTransferActivity: VectorBaseActivity<ActivitySimpleBinding>(), WalletTransferFragment.TransferCallback {

    @Parcelize
    data class Args(
            val senderAddress: String,
            val recipientAddress: String,
            val senderPrivateKey: String,
            val encryptedKey: String,
            val type: String
    ) : Parcelable

    companion object {
        fun newIntent(context: Context, senderAddress: String, recipientAddress: String, senderPrivateKey: String, encryptedKey: String, type: String): Intent {
            return Intent(context, WalletTransferActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(senderAddress, recipientAddress, senderPrivateKey, encryptedKey, type))
            }
        }
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)

    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, WalletTransferFragment::class.java, intent?.extras?.getParcelable(Mavericks.KEY_ARG))
        }
    }

    override fun onTransactionSuccess() {
        finish()
    }
}
