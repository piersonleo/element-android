package im.vector.app.features.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.usercode.ShowUserCodeFragment
import im.vector.app.features.usercode.UserCodeActivity
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import kotlin.reflect.KClass

@AndroidEntryPoint
class WalletCodeActivity: VectorBaseActivity<ActivitySimpleBinding>(){

    @Parcelize
    data class Args(
            val accountName: String,
            val address: String
    ) : Parcelable

    companion object {
        fun newIntent(context: Context, accountName: String, address: String): Intent {
            return Intent(context, WalletCodeActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(accountName, address))
            }
        }
    }

    override fun getBinding(): ActivitySimpleBinding {
        return ActivitySimpleBinding.inflate(layoutInflater)
    }

    override fun initUiAndData() {
        showFragment(WalletCodeFragment::class)

    }


    private fun showFragment(fragmentClass: KClass<out Fragment>) {
        if (supportFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            replaceFragment(views.simpleFragmentContainer, fragmentClass.java, intent?.extras?.getParcelable(Mavericks.KEY_ARG), fragmentClass.simpleName, useCustomAnimation = true)
        }
    }


}
