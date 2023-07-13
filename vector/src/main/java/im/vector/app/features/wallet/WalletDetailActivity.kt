package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.roommemberprofile.RoomMemberProfileFragment
import kotlin.reflect.KClass

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

    override fun getMenuRes()= R.menu.vchat_wallet_account_menu

    private fun showFragment(fragmentClass: KClass<out Fragment>, params: Parcelable? = null) {
        if (supportFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            replaceFragment(views.simpleFragmentContainer, fragmentClass.java, params, fragmentClass.simpleName, useCustomAnimation = true)
        }
    }

}
