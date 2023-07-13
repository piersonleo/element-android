package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class WalletCreateSuccessActivity: VectorBaseActivity<ActivitySimpleBinding>(), WalletCreateSuccessFragment.CreateSuccessCallback {

    companion object {
        fun newIntent(context: Context, keyJson: String, accountName: String): Intent {
            return Intent(context, WalletCreateSuccessActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(keyJson, accountName))
            }
        }
    }

    @Parcelize
    data class Args(
            val jsonString: String,
            val accountName: String
    ) : Parcelable

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, WalletCreateSuccessFragment::class.java, intent?.extras?.getParcelable(Mavericks.KEY_ARG))
        }

    }

    override fun onDone() {
        finish()
    }
}
