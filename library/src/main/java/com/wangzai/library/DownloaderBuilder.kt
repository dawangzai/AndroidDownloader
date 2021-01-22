package com.wangzai.library

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.wangzai.library.downloader.Downloader
import com.wangzai.library.downloader.IDownload
import com.wangzai.library.entity.Notify

class DownloaderBuilder(
    var context: Context,
    var url: String?,
    var dirType: String?,
    var subPath: String?,
    var name: String?,
    var progress: ((Int, Int) -> Unit)?,
    var status: ((Int) -> Unit)?,
    var notify: Notify?
) {
    private constructor(builder: Builder) : this(
        builder.context,
        builder.url,
        builder.dirType,
        builder.subPath,
        builder.name,
        builder.progress,
        builder.status,
        builder.notify
    )

    companion object {
        inline fun build(context: Context, block: Builder.() -> Unit) =
            Builder(context).apply(block).build()
    }

    fun download(owner: LifecycleOwner) {
        Downloader(this, owner).download()
    }

    fun download(download: IDownload) {
        download.download()
    }

    fun download(block: DownloaderBuilder.() -> Unit) {
        this.block()
    }

    class Builder(val context: Context) {
        var url: String? = null
        var dirType: String? = null
        var subPath: String? = null
        var name: String? = null
        var progress: ((Int, Int) -> Unit)? = null
        var status: ((Int) -> Unit)? = null
        var notify: Notify? = null

        fun build() = DownloaderBuilder(this)
    }
}