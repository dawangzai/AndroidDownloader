package com.wangzai.library.downloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import com.wangzai.library.DownloaderBuilder

class ApkDownloader(builder: DownloaderBuilder) :
    Downloader(builder, null) {

    private var broadcastReceiver = DownLoadBroadcast()

    init {
        registerBroadCast()
    }

    override fun makeRequest(): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(builder.url)).apply {
            setTitle("下载标题")
            setDescription("下载描述")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(
                builder.context,
                builder.dirType ?: Environment.DIRECTORY_DOWNLOADS,
                if (builder.subPath.isNullOrEmpty()) builder.name else "${builder.subPath}/${builder.name}"
            )
        }
    }

    override fun download() {
        checkExist { isExit, id ->
            if (isExit) {
                val uri = manager.getUriForDownloadedFile(id!!)
                installApk(uri)
                clear()
                builder.status?.invoke(STATUS_SUCCESSFUL)
            } else {
                super.download()
            }
        }
    }

    private fun registerBroadCast() {
        val intentFilter = IntentFilter().also {
            it.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            it.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        builder.context.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterBroadCast() {
        builder.context.unregisterReceiver(broadcastReceiver)
    }

    private inner class DownLoadBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            when (intent.action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    if (id == downloadId) {
                        val uri = manager.getUriForDownloadedFile(id)
                        installApk(uri)
                    }
                    clear()
                    builder.status?.invoke(STATUS_SUCCESSFUL)
                }
            }
        }
    }

    fun installApk(apkPath: Uri?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(apkPath, "application/vnd.android.package-archive")
        builder.context.startActivity(intent)
    }

    private fun clear() {
        unregisterBroadCast()
    }
}