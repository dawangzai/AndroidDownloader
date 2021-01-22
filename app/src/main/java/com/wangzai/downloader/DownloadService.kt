package com.wangzai.downloader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.wangzai.library.DownloaderBuilder
import com.wangzai.library.downloader.ApkDownloader
import com.wangzai.library.downloader.STATUS_SUCCESSFUL
import com.wangzai.library.entity.Notify

@Suppress("UNREACHABLE_CODE")
class DownloadService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DownloaderBuilder.build(this) {
            name = "test.apk"
            url = "http://m.lieni.com/download/LieNiC.apk"
            notify = Notify("下载标题", "下载描述")
            status = {
                when (it) {
                    STATUS_SUCCESSFUL -> {
                        stopSelf()
                    }
                }
            }
        }.download {
            download(ApkDownloader(this))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}