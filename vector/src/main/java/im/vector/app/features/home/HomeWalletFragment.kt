package im.vector.app.features.home

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vcard.vchat.utils.Utils
import im.vector.app.R
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletHomeBinding
import im.vector.app.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.userdirectory.UserListAction
import im.vector.app.features.wallet.WalletDetailActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.parcelize.Parcelize
import reactivecircus.flowbinding.android.widget.textChanges
import timber.log.Timber
import javax.inject.Inject

//fragment for vChat's mesh wallet
@Parcelize
data class WalletDetailsArgs(
        val displayName: String,
        val tokenName: String,
        val avatarUrl: String,
        val currency: String,
        val value: String
) : Parcelable

class HomeWalletFragment @Inject constructor(
        val homeWalletController: HomeWalletItemController
): VectorBaseFragment<FragmentWalletHomeBinding>(), HomeWalletItemController.Callback {

    private lateinit var wallets: ArrayList<WalletItemModel>
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletHomeBinding {
        setHasOptionsMenu(true)

        return FragmentWalletHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
    }

    private fun setupRecyclerView(){
        val walletJson = Utils.getJsonDataFromAsset(this.requireContext(), "wallets.json")

        val walletList = object : TypeToken<ArrayList<WalletItemModel>>() {}.type
        wallets = Gson().fromJson(walletJson, walletList)

        homeWalletController.callback = this
        homeWalletController.setData(wallets)
        views.accountListRecyclerView.configureWith(homeWalletController)
    }

    private fun setupSearchView() {
        views.accountListSearch
                .textChanges()
                .onEach { text ->
                    val searchValue = text.trim()
                    if (searchValue.isBlank()) {
                        homeWalletController.setData(wallets)
                        views.accountListRecyclerView.configureWith(homeWalletController)
                    } else {
                        val result = ArrayList<WalletItemModel>()
                        for (wallet in wallets) {
                            if (wallet.displayName.lowercase().contains(searchValue.toString().lowercase()) || wallet.currency.lowercase().contains(searchValue.toString().lowercase())) {
                                result.add(wallet)
                            }
                        }

                        homeWalletController.setData(result)
                        views.accountListRecyclerView.configureWith(homeWalletController)
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.accountListSearch.setupAsSearch()
        views.accountListSearch.requestFocus()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.menu_home_filter)
        if (item != null){
            item.isVisible = false
        }
    }

    override fun onItemClick(wallet: WalletItemModel) {
        Timber.v("wallet: ${wallet.displayName} is clicked")
        val args = WalletDetailsArgs(
                displayName = wallet.displayName,
                tokenName = wallet.tokenName,
                currency = wallet.currency,
                value = wallet.value,
                avatarUrl = wallet.avatarUrl
        )
        val intent = WalletDetailActivity.newIntent(this.requireContext(), args)
        startActivity(intent)

    }
}
