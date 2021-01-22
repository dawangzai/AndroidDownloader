package com.wangzai.library.downloader

import android.app.DownloadManager

interface IDownload {
    fun makeRequest(): DownloadManager.Request

    fun download()
}