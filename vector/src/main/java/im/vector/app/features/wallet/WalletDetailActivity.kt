package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.roommemberprofile.RoomMemberProfileFragment

@AndroidEntryPoint
class WalletDetailActivity:  VectorBaseActivity<ActivitySimpleBinding>(){

    companion object {
        fun newIntent(context: Context, args: WalletDetailsArgs): Intent {
            return Intent(context, WalletDetailActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: WalletDetailsArgs = intent?.extras?.getParcelable(Mavericks.KEY_ARG) ?: return
            addFragment(views.simpleFragmentContainer, WalletDetailFragment::class.java, fragmentArgs)
        }

    }
}
