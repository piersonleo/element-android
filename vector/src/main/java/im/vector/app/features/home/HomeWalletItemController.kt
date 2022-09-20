package im.vector.app.features.home

import android.content.Context
import com.airbnb.epoxy.TypedEpoxyController
import com.vcard.vchat.mesh.database.AccountEntity
import im.vector.app.features.userdirectory.UserListController
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import timber.log.Timber
import javax.inject.Inject

class HomeWalletItemController @Inject constructor(private val context: Context) : TypedEpoxyController<List<AccountEntity>>() {

    var callback: Callback? = null

    override fun buildModels(data: List<AccountEntity>?) {
        val host = this
        data?.forEach {item ->
            homeWalletItem(context) {
                id(item.address)
                model(item)
                listener {
                    host.callback?.onItemClick(item)
                }
            }
        }
    }

    interface Callback {
        fun onItemClick(wallet: AccountEntity)
    }
}
