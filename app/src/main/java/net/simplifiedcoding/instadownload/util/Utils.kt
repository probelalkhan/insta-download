package net.simplifiedcoding.instadownload.util

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat

fun View.visible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun View.enable(isEnabled: Boolean) {
    setEnabled(isEnabled)
    alpha = if (isEnabled) 1f else 0.5f
}

fun Context.toast(text: String?) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun Context.showPermissionRequestDialog(
    title: String,
    body: String,
    callback: () -> Unit
) {
    AlertDialog.Builder(this).also {
        it.setTitle(title)
        it.setMessage(body)
        it.setPositiveButton("Ok") { _, _ ->
            callback()
        }
    }.create().show()
}

fun Context.permissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
