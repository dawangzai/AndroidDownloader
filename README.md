# DownloadManager 详解以及简单封装
本文主要是对 `DownloadManager`的使用介绍，包含以下部分

- 监听下载进度
- 判断是否下载过
- Apk 的下载安装
- 简单的封装

## 添加一些权限

	<uses-permission android:name="android.permission.INTERNET" />
    <!--    安装应用权限-->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
    <!--    不弹通知权限-->
    <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
    
    
 首先得添加一些权限由于`DownloadManager` 默认下载是弹一个通知的，如果想要不弹通知得加一个 `android.permission.DOWNLOAD_WITHOUT_NOTIFICATION` 权限
 
 如果要下载`HTTP`资源需要添加
 
 	<application
        android:usesCleartextTraffic="true">
    </application>
 
 ## 监听下载进度
 这里可以通过 `contentResolver`监听`uri`的变化
 
      context.contentResolver.registerContentObserver(
          Uri.parse("content://downloads/my_downloads/${id}"),
          false,
          contentObserver
      )

第一个参数 `content://downloads/my_downloads` 是怎么来的呢，可以看一下`DownloadManager`的`enqueue`方法

	public long enqueue(Request request) {
        ContentValues values = request.toContentValues(mPackageName);
        Uri downloadUri = mResolver.insert(Downloads.Impl.CONTENT_URI, values);
        long id = Long.parseLong(downloadUri.getLastPathSegment());
        return id;
    }

这里的 `Downloads.Impl.CONTENT_URI`的值就是 `content://downloads/my_downloads`.

第二个参数是一个 Boolean 值,`true`表示只要`content://downloads/my_downloads`这个目录下的任何资源变化都会通知，你不需要指定后面的资源`id`,`false`表示需要指定到具体的某个资源才会给你通知，所以上面的方法还可以这么写

    context.contentResolver.registerContentObserver(
        Uri.parse("content://downloads/my_downloads"),
        true,
        contentObserver
    )
    
 第三个参数就是通知的回调,具体的进度值就可以在这里面查询到
 
 	val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
    
然后通过 `cursor` 查询到具体的值
	
    // 总大小
    val total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
    // 已下载的大小
    val current = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
    
当然对于游标的操作要放到子线程中

## 判断是否下载过
有的时候下载某个资源的时候要先判断是否下载过了，比如下载`apk`文件时，这个时候我们也是通过上述相同的方法

	val cursor = downloadManager.query(DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL))

这次过滤出来已经下载过的文件

    val local_uri = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
    // 或者
    val uri = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
    
可以通过 `local_uri`或者`uri`和要下载的文件的 `url`去做比较，看看有没有被下载过

	private fun queryExist(): Pair<Boolean, Long?> {
        val query = DownloadManager.Query()
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        manager.query(query).use {
            while (it.moveToNext()) {
            // 这里通过文件名去做比较，如果有相同的文件名就说明下载过了
                if (it.getString(it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        .contains(builder.name!!)
                ) {
                // 这里把资源 id 返回去
                    val id =
                        it.getLong(it.getColumnIndex(DownloadManager.COLUMN_ID))
                    return Pair(true, id)
                }
            }
        }
        return Pair(false, null)
    }
    
## apk 的下载
`apk` 下载一般会放在 `Service`里然后弹一个通知
	
    DownloadManager.Request(Uri.parse(builder.url)).apply {
        setTitle("下载标题")
        setDescription("下载描述")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        setDestinationInExternalFilesDir(
            builder.context,
            builder.dirType ?: Environment.DIRECTORY_DOWNLOADS,
            if (builder.subPath.isNullOrEmpty()) builder.name else "${builder.subPath}/${builder.name}"
    )
            
`DownloadManager.Request` 可以给给通知做一个配置标题什么的.因为不需要获取下载进度，所以可以直接通过广播去监听是否下载完成

	private fun registerBroadCast() {
        val intentFilter = IntentFilter().also {
            it.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            it.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        builder.context.registerReceiver(broadcastReceiver, intentFilter)
    }
    
	private inner class DownLoadBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            when (intent.action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    if (id == downloadId) {
                        // 这里通过 DownloadManager 提供的方法拿到资源的 uri，就不要通过 FileProvider了 
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
    
## 简单的封装
封装使用 `Build` 模式做了一下参数的定制化，具体的放在了 `github`上了

	DownloaderBuilder.build(this) {
        name = "test.jpg"
        url = "https://image.baidu.com/search/down?tn=download&word=download&ie=utf8&fr=detail&url=https%3A%2F%2Fgimg2.baidu.com%2Fimage_search%2Fsrc%3Dhttp%253A%252F%252Fattachments.gfan.com%252Fforum%252Fattachments2%252F201301%252F29%252F125313339n39z82ydzc32y.jpg%26refer%3Dhttp%253A%252F%252Fattachments.gfan.com%26app%3D2002%26size%3Df9999%2C10000%26q%3Da80%26n%3D0%26g%3D0n%26fmt%3Djpeg%3Fsec%3D1613879131%26t%3Db27c22e4cb13f581da043e1dcc83c00d&thumburl=https%3A%2F%2Fss1.bdstatic.com%2F70cFvXSh_Q1YnxGkpoWK1HF6hhy%2Fit%2Fu%3D2565443740%2C1354606035%26fm%3D26%26gp%3D0.jpg"
        status = {
            when (it) {
                STATUS_SUCCESSFUL -> {
                    Log.i("MainActivity", "STATUS_SUCCESSFUL")
                }
            }
        }
        progress = { current, total ->
            Log.i("MainActivity", "current=${current}--tottal=${total}")
        }
    }.download(this)