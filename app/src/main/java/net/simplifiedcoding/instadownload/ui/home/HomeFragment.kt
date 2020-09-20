package net.simplifiedcoding.instadownload.ui.home

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import net.simplifiedcoding.instadownload.BuildConfig
import net.simplifiedcoding.instadownload.R
import net.simplifiedcoding.instadownload.databinding.FragmentHomeBinding
import net.simplifiedcoding.instadownload.network.Resource
import net.simplifiedcoding.instadownload.ui.base.BaseFragment
import net.simplifiedcoding.instadownload.util.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class HomeFragment : BaseFragment<HomeViewModel, FragmentHomeBinding>() {

    private lateinit var imageLoader: ImageLoader
    private lateinit var mediaDownloadURL: String
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var downloadedBitmap: Bitmap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPermissionCallback()
        binding.progressbar.visible(false)
        imageLoader = Coil.imageLoader(requireContext())

        binding.buttonPasteLink.setOnClickListener {
            pasteLink()
        }

        binding.buttonDownload.setOnClickListener {
            getInstaInfo()
        }

        binding.buttonSave.setOnClickListener {
            getBitmapFromUrl()
        }

        binding.editTextInstaLink.addTextChangedListener {
            binding.buttonDownload.enable(it.toString().trim().isNotEmpty())
        }

        viewModel.instaInfo.observe(viewLifecycleOwner, Observer {
            binding.progressbar.visible(false)
            when (it) {
                is Resource.Success -> fetchImage(it.response.string())
                is Resource.Failure -> requireContext().toast(it.errorMessage)
            }
        })
    }

    private fun setPermissionCallback() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    if (::downloadedBitmap.isInitialized) {
                        saveMediaToStorage(downloadedBitmap)
                    }
                }
            }
    }

    private fun getBitmapFromUrl() = viewLifecycleOwner.lifecycleScope.launch {
        binding.progressbar.visible(true)
        if (::mediaDownloadURL.isInitialized) {
            val request = ImageRequest.Builder(requireContext())
                .data(mediaDownloadURL)
                .build()
            downloadedBitmap = (imageLoader.execute(request).drawable as BitmapDrawable).bitmap
            checkPermissionAndSaveBitmap()
        } else {
            requireContext().toast("No Media Downloaded")
        }
    }

    private fun checkPermissionAndSaveBitmap() {
        binding.progressbar.visible(false)
        when {
            requireContext().permissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                saveMediaToStorage(downloadedBitmap)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                requireContext().showPermissionRequestDialog(
                    getString(R.string.permission_title),
                    getString(R.string.write_permission_request)
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun fetchImage(string: String) {
        try {
            mediaDownloadURL = JSONObject(string)
                .getJSONObject("graphql")
                .getJSONObject("shortcode_media")
                .getString("display_url")
            binding.imageView.load(mediaDownloadURL)
        } catch (e: Exception) {
            requireContext().toast("Cannot fetch the Media File, try a Different URL")
            e.printStackTrace()
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context?.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES
                    )
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            context?.toast("Saved to Photos")
        }
    }

    private fun getInstaInfo() {
        binding.progressbar.visible(true)
        val instaUrl = binding.editTextInstaLink.text.toString().trim()
        val url = try {
            instaUrl.substring(0, instaUrl.indexOf("?"))
        } catch (e: StringIndexOutOfBoundsException) {
            instaUrl
        }
        viewModel.getInstaInfo("$url?__a=1")
    }

    private fun pasteLink() {
        val clipboard: ClipboardManager? =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard?.hasPrimaryClip() == true) {
            binding.editTextInstaLink.setText(clipboard.primaryClip?.getItemAt(0)?.text.toString())
        }
    }

    override fun getViewModelClass() = HomeViewModel::class.java

    override fun getFragmentLayoutBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_privacy_policy -> {
                openPrivacyPolicy()
                true
            }
            R.id.menu_share_app -> {
                shareApp()
                true
            }
            else -> false
        }
    }

    private fun openPrivacyPolicy() {
        val policyUrl = "https://probelalkhan.github.io/instadownload.github.io/"
        Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse(policyUrl)
            startActivity(it)
        }
    }

    private fun shareApp() {
        try {
            Intent(Intent.ACTION_SEND).also {
                it.type = "text/plain"
                it.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                val shareMessage = StringBuilder().apply {
                    append(getString(R.string.share_message))
                    append("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                }.toString()
                it.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(it, getString(R.string.select_an_app)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}