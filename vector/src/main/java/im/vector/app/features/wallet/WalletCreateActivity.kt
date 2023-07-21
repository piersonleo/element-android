package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding

@AndroidEntryPoint
class WalletCreateActivity: VectorBaseActivity<ActivitySimpleBinding>(), WalletCreateFragment.CreateAccountCallback {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, WalletCreateActivity::class.java)
        }
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, WalletCreateFragment::class.java)
        }

    }

    override fun onAccountCreated() {
        finish()
    }
}
