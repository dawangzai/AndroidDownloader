package com.wangzai.library.downloader

import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.wangzai.library.DownloaderBuilder
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val STATUS_RUNNING = DownloadManager.STATUS_RUNNING
const val STATUS_SUCCESSFUL = DownloadManager.STATUS_SUCCESSFUL

@Suppress("LeakingThis")
open class Downloader(val builder: DownloaderBuilder, owner: LifecycleOwner?) :
    IDownload {
    private val context = builder.context
    private val mainExecutor = MainThreadExecutor()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    val manager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    private val contentObserver = DownloadChangeObserver()
    var downloadId: Long? = null

    init {
        checkParam()
        owner?.let {
            it.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_DESTROY -> {
                            clear()
                        }
                    }
                }
            })
        }
    }

    override fun download() {
        downloadId = manager.enqueue(makeRequest())
        registerContentObserver()
    }

    override fun makeRequest(): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(builder.url)).apply {
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            setDestinationInExternalFilesDir(
                context,
                builder.dirType ?: Environment.DIRECTORY_DOWNLOADS,
                if (builder.subPath.isNullOrEmpty()) builder.name else "${builder.subPath}/${System.currentTimeMillis()}${builder.name}"
            )
        }
    }

    private fun registerContentObserver() {
        context.contentResolver.registerContentObserver(
            Uri.parse("content://downloads/my_downloads/${downloadId}"),
            false,
            contentObserver
        )
    }

    private fun unregisterContentObserver() {
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    private fun updateProgress() {
        downloadId?.let {
            manager.query(DownloadManager.Query().setFilterById(it))
                .use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_RUNNING -> {
                                val total =
                                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                val current =
                                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                builder.progress?.let {
                                    mainExecutor.execute {
                                        it.invoke(current, total)
                                    }
                                }
                                builder.status?.let {
                                    mainExecutor.execute {
                                        it.invoke(STATUS_RUNNING)
                                    }
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val total =
                                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                builder.progress?.let {
                                    mainExecutor.execute {
                                        it.invoke(total, total)
                                    }
                                }
                                builder.status?.let {
                                    mainExecutor.execute {
                                        it.invoke(STATUS_SUCCESSFUL)
                                    }
                                }
                                clear()
                            }
                        }
                    }
                }
        }
    }

    private inner class DownloadChangeObserver : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            executorService.execute { updateProgress() }
        }
    }

    private fun clear() {
        unregisterContentObserver()
    }

    private fun checkParam() {
        if (builder.url.isNullOrEmpty()) {
            throw NullPointerException("url is null")
        }
        if (builder.name.isNullOrEmpty()) {
            throw NullPointerException("name is null")
        }
    }

    fun checkExist(block: (Boolean, Long?) -> Unit) {
        executorService.execute {
            val (isExit, id) = queryExist()
            mainExecutor.execute {
                block.invoke(isExit, id)
            }
        }
    }

    private fun queryExist(): Pair<Boolean, Long?> {
        val query = DownloadManager.Query()
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        manager.query(query).use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        .contains(builder.name!!)
                ) {
                    val id =
                        it.getLong(it.getColumnIndex(DownloadManager.COLUMN_ID))
                    return Pair(true, id)
                }
            }
        }
        return Pair(false, null)
    }

    class MainThreadExecutor : Executor {
        private val handler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable?) {
            command?.let {
                handler.post(it)
            }
        }
    }
}