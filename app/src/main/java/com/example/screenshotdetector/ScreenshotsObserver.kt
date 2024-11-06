package com.aesh.screenshotdetector

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ScreenshotsListener {
    fun onScreenshotAdded(uri: Uri);
}

class ScreenshotsObserver(
    private val activity: ComponentActivity,
    private val screenshotsListener:ScreenshotsListener
) : ContentObserver(
    Handler(Looper.getMainLooper())
) {

    private val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE;
    private var permissionGranted = false;
    private var registered=false;
    private var notificationJob: Job?=null;

    private fun checkPermissions(permissionRequestLauncher: ActivityResultLauncher<String>) {
        if (ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                requiredPermission
            ) != PackageManager.PERMISSION_GRANTED
        )
            return requestNecessaryPermissions(permissionRequestLauncher);
        permissionGranted = true;
        registerSelf()
    }

    private fun registerSelf() {
        if(!permissionGranted || registered)
            return;
        registered=true;
        activity.contentResolver.registerContentObserver(imagesUri,true,this);
    }

    private fun requestNecessaryPermissions(permissionRequestLauncher: ActivityResultLauncher<String>) {
        permissionRequestLauncher.launch(requiredPermission)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if(!permissionGranted) return;
        uri?.let { checkLatestChange(it); }
    }

    private fun checkLatestChange(changedUri: Uri) {

        if(!changedUri.toString().contains(imagesUri.toString())) return;
        val query = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?";

        activity.contentResolver.query(
            imagesUri,
            arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns._ID),
            query,
            arrayOf("%Screenshots%"),
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        ).use {
            if (it != null && it.moveToFirst()) {
                val idColumnIndex=it.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
                val imgUri = ContentUris.withAppendedId(imagesUri,it.getLong(idColumnIndex));
                notify(imgUri);
            }
        }
    }

    fun onPermissionResultReceived(granted: Boolean): Unit {
        permissionGranted = granted;
        registerSelf()
    }
    fun start(launcher : ActivityResultLauncher<String>){
        checkPermissions(launcher);
    }

    fun stop() {
        if(!registered) return;
        notificationJob?.cancel();
        activity.contentResolver.unregisterContentObserver(this);
        registered=false;
    }
    private fun notify(uri: Uri){
        if(notificationJob?.isCompleted == true) return;
        notificationJob?.cancel();
        notificationJob=activity.lifecycleScope.launch {
            delay(450)
            if(isActive) {
                withContext(Dispatchers.Main){
                    screenshotsListener.onScreenshotAdded(uri);
                }
                notificationJob=null;
            }
        };
    }
}