package com.su.sakuraanime2plugin.plugin.util

import com.su.mediabox.pluginapi.Constant
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object JsoupUtil {
    /**
     * 获取没有运行js的html
     */
    suspend fun getDocument(url: String): Document =
        runCatching { getDocumentSynchronously(url) }.getOrThrow()

    fun getDocumentSynchronously(url: String): Document = jsoupRandomHeaderPase(url)

    fun jsoupRandomHeaderPase(url: String) = Jsoup.connect(url)
        .header("User-Agent", Constant.Request.getRandomUserAgent())
        .header("cookie", "rtsm=1;")
        .get()

}