package com.wangzai.downloader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.wangzai.library.DownloaderBuilder
import com.wangzai.library.downloader.STATUS_SUCCESSFUL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }
}