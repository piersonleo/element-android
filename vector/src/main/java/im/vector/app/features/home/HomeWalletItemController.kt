package im.vector.app.features.home

import android.content.Context
import com.airbnb.epoxy.TypedEpoxyController
import com.vcard.mesh.sdk.database.entity.AccountEntity
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
