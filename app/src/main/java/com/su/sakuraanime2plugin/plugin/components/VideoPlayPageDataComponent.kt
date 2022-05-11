package com.su.sakuraanime2plugin.plugin.components

import com.su.mediabox.pluginapi.components.IVideoPlayPageDataComponent
import com.su.mediabox.pluginapi.data.VideoPlayMedia
import com.su.mediabox.pluginapi.util.TextUtil.urlDecode
import com.su.mediabox.pluginapi.util.WebUtiIns
import com.su.sakuraanime2plugin.plugin.components.Const.host
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import kotlinx.coroutines.*
import org.jsoup.Jsoup

class VideoPlayPageDataComponent : IVideoPlayPageDataComponent {

    private val ua =
        "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36"

    override suspend fun getVideoPlayMedia(episodeUrl: String): VideoPlayMedia {
        val url = host + episodeUrl
        val document = JsoupUtil.getDocument(url)

        //解析链接
        val videoUrl = withContext(Dispatchers.Main) {
            val iframeUrl = withTimeoutOrNull(10 * 1000) {
                WebUtiIns.interceptResource(
                    url, "(.*)url=(.*)",
                    userAgentString = ua
                )
            } ?: ""
            async {
                when {
                    iframeUrl.isBlank() -> iframeUrl
                    iframeUrl.contains("player") -> iframeUrl.substringAfter("url=")
                        .substringBefore("&").urlDecode()
                    else -> {
                        //一般是国产动漫，通过接口解析正版源，通常返回m3m8
                        val iframeDoc = Jsoup.parse(
                            WebUtiIns.getRenderedHtmlCode(
                                iframeUrl,
                                "(.*)play\\.js(.*)",
                                //这里使用电脑的UA会被强制使用H5标准下的blob
                                userAgentString = ua
                            )
                        )
                        //TODO 部分视频解析出的链接是blob，暂未解决
                        iframeDoc.select("#video").select("video").attr("src")
                    }
                }
            }
        }

        //剧集名
        val name = withContext(Dispatchers.Default) {
            async {
                document.select("[class=gohome]").select("span").first()?.let {
                    it.text().replace(": ", "")
                } ?: ""
            }
        }

        return VideoPlayMedia(name.await(), videoUrl.await())
    }

}