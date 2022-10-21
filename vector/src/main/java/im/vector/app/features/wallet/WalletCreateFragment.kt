package im.vector.app.features.wallet

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.vcard.vchat.mesh.Account
import com.vcard.vchat.mesh.data.EncryptedKeyData
import com.vcard.vchat.mesh.data.EncryptedKeyDataSerializer
import com.vcard.vchat.utils.MeshSharedPref
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletCreateBinding
import timber.log.Timber
import javax.inject.Inject

class WalletCreateFragment@Inject constructor(
) : VectorBaseFragment<FragmentWalletCreateBinding>()
{

    private lateinit var callback: CreateAccountCallback

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletCreateBinding {
        setHasOptionsMenu(true)

        return FragmentWalletCreateBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as CreateAccountCallback
    }

    private fun setupViews(){
        setupToolbar(views.walletCreateToolbar)
                .setTitle("Create New Account")
                .allowBack(useCross = false)

    }
    private fun setupClick(){
        views.btnCreateAccount.debouncedClicks {
            val accountName = views.accountNameInput.text.toString().trim()
            val passphrase = views.passphraseInput.text.toString().trim()
            val retypePassphrase = views.retypePassphraseInput.text.toString().trim()

            when {
                accountName.isEmpty() -> {
                    views.accountNameInput.error = "name can't be empty"
                }
                passphrase.isEmpty() -> {
                    views.passphraseLayout.error = "passphrase can't be empty"
                }
                retypePassphrase.isEmpty() -> {
                    views.retypePassphraseLayout.error = "please retype your passphrase"
                }
                passphrase != retypePassphrase -> {
                    views.passphraseInput.error = "passphrase doesn't match"
                    views.retypePassphraseInput.error = "passphrase doesn't match"
                }
                else -> {
                    val loadingDialog = prepareDialogLoader(requireContext())
                    loadingDialog.show()

                    Thread {
                        val encryptedJson = Account.generateAccount(passphrase, accountName)
                        val gson = GsonBuilder().registerTypeAdapter(EncryptedKeyData::class.java, EncryptedKeyDataSerializer()).setPrettyPrinting().create()
                        val data = gson.fromJson(encryptedJson, EncryptedKeyData::class.java)

                        Timber.d("fullAddress: ${data.fullAddress}")
                        val msp = MeshSharedPref(requireContext())
                        msp.storePp(data.fullAddress, passphrase)

                        activity?.runOnUiThread(Runnable {
                            loadingDialog.dismiss()
                            val intent = WalletCreateSuccessActivity.newIntent(this.requireContext(), encryptedJson, accountName)
                            startActivity(intent)
                            callback.onAccountCreated()
                        })
                    }.start()
                }
            }
        }
    }

    private fun prepareDialogLoader(context: Context): Dialog {
        val dialog = Dialog(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_dialog_loading)
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        tvMessage.text = getString(R.string.vchat_send_create_account_progress)

        return dialog
    }

    interface CreateAccountCallback{
        fun onAccountCreated()
    }
}
