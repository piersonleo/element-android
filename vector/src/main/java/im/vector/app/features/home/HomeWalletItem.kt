package im.vector.app.features.home

import android.content.Context
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.Glide
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.features.settings.devices.DeviceItem
import timber.log.Timber

data class WalletItemModel(
        val id: String,
        val displayName: String,
        val tokenName: String,
        val currency: String,
        val value: String,
        val avatarUrl: String
)

@EpoxyModelClass(layout = R.layout.item_wallet_account)
abstract class HomeWalletItem(private val context: Context) : VectorEpoxyModel<HomeWalletItem.Holder>() {

    @EpoxyAttribute
    lateinit var model: WalletItemModel

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.accountName.text = model.displayName
        holder.accountCurrency.text = model.tokenName
        holder.view.onClick(listener)

        val assetManager = context.resources.assets
        val inputStream = assetManager.open(model.avatarUrl)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        GlideApp.with(context).load(bitmap).into(holder.accountAvatar)
    }


    class Holder : VectorEpoxyHolder() {
        val accountAvatar by bind<ImageView>(R.id.walletAccountAvatar)
        val accountName by bind<TextView>(R.id.walletAccountName)
        val accountCurrency by bind<TextView>(R.id.walletAccountCurrency)

    }

}
