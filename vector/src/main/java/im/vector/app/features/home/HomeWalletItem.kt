package im.vector.app.features.home

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.vcard.vchat.mesh.database.AccountEntity
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.glide.GlideApp

data class WalletItemModel(
        val address: String,
        val name: String,
        val currency: String,
        val privateKey: String
)

@EpoxyModelClass(layout = R.layout.item_wallet_account)
abstract class HomeWalletItem(private val context: Context) : VectorEpoxyModel<HomeWalletItem.Holder>() {

    @EpoxyAttribute
    lateinit var model: AccountEntity

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.accountName.text = model.name
        holder.accountCurrency.text = "GOLD"
        holder.view.onClick(listener)

        val assetManager = context.resources.assets
        val inputStream = assetManager.open("wallet_avatars/ic_card_placeholder.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)

        GlideApp.with(context).load(bitmap).into(holder.accountAvatar)
    }


    class Holder : VectorEpoxyHolder() {
        val accountAvatar by bind<ImageView>(R.id.walletAccountAvatar)
        val accountName by bind<TextView>(R.id.walletAccountName)
        val accountCurrency by bind<TextView>(R.id.walletAccountCurrency)

    }

}
