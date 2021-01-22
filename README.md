# AndroidDownloader

https://juejin.cn/post/6920461495549558798/

使用 `DownloadManager`实现的下载功能，包含以下功能

- 监听下载进度
- 判断是否下载过
- Apk 的下载安装

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
    
## 使用

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