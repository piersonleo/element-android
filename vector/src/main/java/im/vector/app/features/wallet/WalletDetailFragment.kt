package im.vector.app.features.wallet

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletDetailBinding
import im.vector.app.features.home.WalletDetailsArgs
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import timber.log.Timber
import javax.inject.Inject

class WalletDetailFragment @Inject constructor(
): VectorBaseFragment<FragmentWalletDetailBinding>() {

    private val fragmentArgs: WalletDetailsArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletDetailBinding {
        setHasOptionsMenu(true)

        return FragmentWalletDetailBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews(){
        setupToolbar(views.walletDetailToolBar)
                .setTitle(fragmentArgs.displayName)
                .allowBack(useCross = false)

        views.walletDetailHeaderTitle.text = fragmentArgs.displayName
        views.walletDetailHeaderSubtitle.text = getString(R.string.wallet_value, fragmentArgs.value, fragmentArgs.currency)

        val assetManager = requireContext().resources.assets
        val inputStream = assetManager.open(fragmentArgs.avatarUrl)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        GlideApp.with(requireContext()).load(bitmap).into(views.walletImageView)

    }


}
