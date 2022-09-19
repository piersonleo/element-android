package im.vector.app.features.home

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.vcard.vchat.mesh.Account
import com.vcard.vchat.mesh.Address
import com.vcard.vchat.mesh.data.EncryptedKeyData
import com.vcard.vchat.mesh.database.AccountEntity
import com.vcard.vchat.mesh.database.RealmExec
import com.vcard.vchat.utils.Utils
import com.vcard.vchat.utils.Utils.Companion.openJsonFileSelection
import com.vcard.vchat.utils.ViewAnimation
import im.vector.app.R
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.DialogBaseEditTextBinding
import im.vector.app.databinding.FragmentWalletHomeBinding
import im.vector.app.features.wallet.WalletCreateActivity
import im.vector.app.features.wallet.WalletDetailActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.events.model.toContent
import reactivecircus.flowbinding.android.widget.textChanges
import timber.log.Timber
import javax.inject.Inject

//fragment for vChat's mesh wallet
@Parcelize
data class WalletDetailsArgs(
        val name: String,
        val currency: String,
        val address: String,
        val privateKey: String,
        val encryptedKey: String,
        val type: String
) : Parcelable

class HomeWalletFragment @Inject constructor(
        val homeWalletController: HomeWalletItemController
): VectorBaseFragment<FragmentWalletHomeBinding>(), HomeWalletItemController.Callback {

    private lateinit var wallets: ArrayList<WalletItemModel>
    private lateinit var accounts: List<AccountEntity>

    private var isFabAddClicked = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletHomeBinding {
        setHasOptionsMenu(true)

        return FragmentWalletHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTestAccounts()
        //setupRecyclerView()
        //setupSearchView()
        insertNodesFromJson()
        setupButton()
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
        setupSearchView()
    }

    private fun setupTestAccounts() {
        Timber.d("setupRecycler")
        val walletJson = Utils.getJsonDataFromAsset(this.requireContext(), "wallets.json")
        RealmExec().addAccountsFromJson(walletJson)
    }

    private fun setupRecyclerView(){
//        val walletList = object : TypeToken<ArrayList<WalletItemModel>>() {}.type
//        wallets = Gson().fromJson(walletJson, walletList)

        accounts = RealmExec().getAccountsList()

        homeWalletController.callback = this
        homeWalletController.setData(accounts)
        views.accountListRecyclerView.configureWith(homeWalletController)
    }

    private fun setupSearchView() {
        views.accountListSearch
                .textChanges()
                .onEach { text ->
                    val searchValue = text.trim()
                    if (searchValue.isBlank()) {
                        homeWalletController.setData(accounts)
                        views.accountListRecyclerView.configureWith(homeWalletController)
                    } else {
                        val result = ArrayList<AccountEntity>()
                        for (account in accounts) {
                            if (account.name.lowercase().contains(searchValue.toString().lowercase()) || account.currency.lowercase().contains(searchValue.toString().lowercase())) {
                                result.add(account)
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

    private fun insertNodesFromJson(){
        val nodesJson = Utils.getJsonDataFromAsset(this.requireContext(), "nodes.json")
        Thread {
            RealmExec().addNodesFromJson(nodesJson)
        }.start()
    }

    private fun setupButton(){

        ViewAnimation.init(views.fabCreateWalletAccount)
        ViewAnimation.init(views.fabImportAccount)

        views.fabAdd.debouncedClicks {
            isFabAddClicked = ViewAnimation.rotateFab(views.fabAdd, !isFabAddClicked)
            if (isFabAddClicked){
                ViewAnimation.showIn(views.fabCreateWalletAccount)
                ViewAnimation.showIn(views.fabImportAccount)
            }else{
                ViewAnimation.showOut(views.fabCreateWalletAccount)
                ViewAnimation.showOut(views.fabImportAccount)
            }
        }

        views.fabCreateWalletAccount.debouncedClicks {
            val intent = WalletCreateActivity.newIntent(this.requireContext())
            startActivity(intent)

            //Account.generateAccount("test")
        }

        views.fabImportAccount.debouncedClicks {
            importAccount()
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.menu_home_filter)
        if (item != null){
            item.isVisible = false
        }
    }

    override fun onItemClick(wallet: AccountEntity) {

        val args = WalletDetailsArgs(
                name = wallet.name,
                currency = wallet.currency,
                address = wallet.address,
                privateKey = wallet.privateKey,
                encryptedKey = wallet.encryptedKey,
                type = wallet.type
        )
        val intent = WalletDetailActivity.newIntent(this.requireContext(), args)
        startActivity(intent)
    }

    @SuppressLint("NewApi")
    private fun importAccount() {
        openJsonFileSelection(
                requireActivity(),
                importAccountActivityResultLauncher,
                false,
                0
        )
    }

    private val importAccountActivityResultLauncher = registerStartForActivityResult {
        val data = it.data?.data ?: return@registerStartForActivityResult
        if (it.resultCode == Activity.RESULT_OK) {
            //val filename = getFilenameFromUri(requireContext(), data)

            val selectedFileJson =  requireContext().contentResolver.openInputStream(data);
            val jsonString = selectedFileJson!!.bufferedReader().use { it.readText() }

            val toJson = Gson().fromJson(jsonString, EncryptedKeyData::class.java)

            //check the validity of json file
            if (toJson == null ||
                toJson.address.isEmpty() ||
                toJson.checksum.isEmpty() ||
                toJson.fullAddress.isEmpty() ||
                toJson.prefix.isEmpty() ||
                toJson.encryptedKey.cipher.isEmpty() ||
                toJson.encryptedKey.encryptedText.isEmpty() ||
                toJson.encryptedKey.keyChecksum.isEmpty()){

                Toast.makeText(requireContext(), "The content of the file does not match Mesh format", Toast.LENGTH_SHORT).show()
            }else if (!Address.isValidMeshAddressString(toJson.fullAddress)){
                Toast.makeText(requireContext(), "Invalid mesh address", Toast.LENGTH_SHORT).show()
            }else if (RealmExec().getAccountByAddress(toJson.fullAddress) != null){
                Toast.makeText(requireContext(), "Account with the same address already exist", Toast.LENGTH_SHORT).show()
            }else{
                accountNameDialog(toJson)
            }

        }
    }

    private fun accountNameDialog(encryptedKeyData: EncryptedKeyData) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)
        val views = DialogBaseEditTextBinding.bind(layout)
        views.editText.hint = "Account Name"

        MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Account Name")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val name = views.editText.text.toString()

                    if (name.isEmpty()){
                        Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show()
                    }else {

                        Timber.d("jsonData: ${encryptedKeyData.fullAddress}")
                        Timber.d("name: ${views.editText.text}")
                        Account.addAccountFromEncryptedKeyData(name, encryptedKeyData)
                        setupRecyclerView()
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }
}
