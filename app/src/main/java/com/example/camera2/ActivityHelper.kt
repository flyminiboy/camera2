package com.example.camera2

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val PERMISSIONS_REQUEST_CODE = 9222
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

fun Activity.toast(msg:String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun Activity.requestCameraPermission() {
    val shouldRational = ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.CAMERA
    )

    if (shouldRational) {
        this.toast("camera permission is needed to run this application")
    } else {
        ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
    }
}

fun Activity.hasCameraPermission() = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
