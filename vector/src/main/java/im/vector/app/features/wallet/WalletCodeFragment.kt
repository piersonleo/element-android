package im.vector.app.features.wallet

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcard.vchat.utils.Utils
import com.vcard.vchat.utils.ViewUtil
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentWalletCodeBinding
import im.vector.app.databinding.FragmentWalletDetailBinding
import im.vector.app.features.home.WalletDetailsArgs
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class WalletCodeFragment @Inject constructor(
) : VectorBaseFragment<FragmentWalletCodeBinding>() {

    private val fragmentArgs: WalletCodeActivity.Args by args()
    private var isRotate = false
    private lateinit var orientationEventListener: OrientationEventListener

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletCodeBinding {
        setHasOptionsMenu(true)

        return FragmentWalletCodeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupButton()
        setupOrientationEventListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener.disable()
    }

    private fun setupView(){
        setupToolbar(views.showUserCodeToolBar)
                .allowBack(useCross = true)

        views.walletAddressTitle.text = fragmentArgs.accountName
        views.walletAddressSubtitle.text = fragmentArgs.address

        val qrData = "MESH${fragmentArgs.address}"
        //views.accountQRImage.setData(fragmentArgs.address)
        val icon = BitmapFactory.decodeResource(resources, R.drawable.img_logo_vbiz_rounded_corner_2)
        views.accountQRImage.setData2(qrData, icon)

    }

    private fun setupButton(){
        views.buttonShowMore.debouncedClicks {
            showFullAddressBottomDialog()
        }
        views.walletAddressSubtitle.debouncedClicks {
            showFullAddressBottomDialog()
        }

        views.btnSaveQR.debouncedClicks {
            saveBitmap(views.showUserCodeCard.drawToBitmap(), fragmentArgs.accountName)
        }
    }

    private fun setupOrientationEventListener(){
        orientationEventListener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL){
            override fun onOrientationChanged(orientation: Int) {
                if (orientation in 120..240 && !isRotate){
                    isRotate = true
                    views.showUserCodeCard.animate().rotation(180F).start()
                }else if (orientation in 1..10){
                    if (views.showUserCodeCard.rotation == 180F) {
                        views.showUserCodeCard.animate().rotation(0F).start()
                        isRotate = false
                    }
                }
            }
        }

        orientationEventListener.enable()
    }


    private fun showFullAddressBottomDialog(){
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_wallet_code_address, null)
        val rootLayout = view.findViewById<LinearLayout>(R.id.rootLayout)

        val viewContentTitle = view.findViewById<TextView>(R.id.bottomSheetWalletTitle)
        viewContentTitle.text = getString(R.string.vchat_wallet_qr_address_title)

        val viewContent = view.findViewById<TextView>(R.id.bottomSheetWalletContent)
        viewContent.text = fragmentArgs.address

        if (isRotate){
            rootLayout.animate().rotation(180F).start()
        }else if (!isRotate){
            rootLayout.animate().rotation(0F).start()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    @Suppress("DEPRECATION")
    private fun saveBitmap(bitmap: Bitmap, filename: String) {
        try {
            val fileName = "$filename.jpg"
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(directory, fileName)
                values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
            val uri: Uri? = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            requireActivity().contentResolver.openOutputStream(uri!!).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                requireActivity().contentResolver.update(uri, values, null, null)
            }

            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_title_success)
                    .setMessage(R.string.vchat_create_mesh_account_export_saved_qr_code)
                    .show()

        } catch (e: Exception) {
            Timber.d( e.toString()) // java.io.IOException: Operation not permitted
        }
    }

    fun Bitmap.addOverlayToCenter(overlayBitmap: Bitmap): Bitmap {

        val bitmap2Width = overlayBitmap.width
        val bitmap2Height = overlayBitmap.height
        val marginLeft = (this.width * 0.5 - bitmap2Width * 0.5).toFloat()
        val marginTop = (this.height * 0.5 - bitmap2Height * 0.5).toFloat()
        val canvas = Canvas(this)
        canvas.drawBitmap(this, Matrix(), null)
        canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null)
        return this
    }


}
