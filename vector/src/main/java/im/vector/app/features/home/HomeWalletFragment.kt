package im.vector.app.features.home

import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletHomeBinding
import timber.log.Timber
import javax.inject.Inject

//fragment for vChat's mesh wallet

class HomeWalletFragment @Inject constructor(): VectorBaseFragment<FragmentWalletHomeBinding>() {
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletHomeBinding {
        setHasOptionsMenu(true)
        return FragmentWalletHomeBinding.inflate(inflater, container, false)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.menu_home_filter)
        Timber.i("item: $item")
        if (item != null){
            item.isVisible = false
        }
    }
}
