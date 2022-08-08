package com.su.sakuraanime2plugin.plugin.components

import android.util.Log
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.su.mediabox.pluginapi.components.IVideoPlayPageDataComponent
import com.su.mediabox.pluginapi.data.VideoPlayMedia
import com.su.mediabox.pluginapi.util.AppUtil
import com.su.mediabox.pluginapi.util.TextUtil.urlDecode
import com.su.mediabox.pluginapi.util.WebUtilIns
import com.su.sakuraanime2plugin.plugin.components.Const.host
import com.su.sakuraanime2plugin.plugin.components.Const.ua
import com.su.sakuraanime2plugin.plugin.danmaku.OyydsDanmakuParser
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.Text.trimAll
import com.su.sakuraanime2plugin.plugin.util.oyydsDanmakuApis
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.File

class VideoPlayPageDataComponent : IVideoPlayPageDataComponent {

    private val blobDataTmpFile by lazy(LazyThreadSafetyMode.NONE) {
        File(AppUtil.appContext.externalCacheDir, "blob_data_tmp.m3u8").apply {
            if (!exists())
                createNewFile()
        }
    }

    private var episodeDanmakuId = ""
    override suspend fun getDanmakuData(
        videoName: String,
        episodeName: String,
        episodeUrl: String
    ): List<DanmakuItemData> {
        val name = videoName.trimAll()
        var episode = episodeName.trimAll()
        //剧集对集去除所有额外字符，增大弹幕适应性
        val episodeIndex = episode.indexOf("集")
        if (episodeIndex > -1 && episodeIndex != episode.length - 1) {
            episode = episode.substring(0, episodeIndex + 1)
        }
        Log.d("请求Oyyds弹幕", "媒体:$name 剧集:$episode")
        return oyydsDanmakuApis.getDanmakuData(name, episode).data.let { danmukuData ->
            val data = mutableListOf<DanmakuItemData>()
            danmukuData.data.forEach { dataX ->
                OyydsDanmakuParser.convert(dataX)?.also { data.add(it) }
            }
            episodeDanmakuId = danmukuData.episode.id
            data
        }
    }

    override suspend fun putDanmaku(
        videoName: String,
        episodeName: String,
        episodeUrl: String,
        danmaku: String,
        time: Long,
        color: Int,
        type: Int
    ): Boolean = try {
        Log.d("发送弹幕到Oyyds", "内容:$danmaku 剧集id:$episodeDanmakuId")
        oyydsDanmakuApis.addDanmaku(
            danmaku,
            //Oyyds弹幕标准时间是秒
            (time / 1000F).toString(),
            episodeDanmakuId,
            OyydsDanmakuParser.danmakuTypeMap.entries.find { it.value == type }?.key ?: "scroll",
            String.format("#%02X", color)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override suspend fun getVideoPlayMedia(episodeUrl: String): VideoPlayMedia {
        val url = host + episodeUrl
        val document = JsoupUtil.getDocument(url)

        //解析链接
        val videoUrl = withContext(Dispatchers.Main) {
            val iframeUrl = withTimeoutOrNull(10 * 1000) {
                WebUtilIns.interceptResource(
                    url, "(.*)url=(.*)",
                    userAgentString = ua
                )
            } ?: ""
            async {
                when {
                    iframeUrl.isBlank() -> iframeUrl
                    //1. dpx2/alm3p需要blob拦截（直接拼接请求是加密的）
                    //https://m.yhdmp.net/yxsf/player/dpx2/alm3p.html?url=%2fgm3px%2fgt%2d20110046%5frbak%2f%5bSC%2dOL%5d%5b6286f85304f8f2fa%5d%5b01%5d%5b720P%5d%5bCHS%5d%20nvl%2drm&getplay_url=%2F_getplay%3Faid%3D11128%26playindex%3D1%26epindex%3D0%26r%3D0.07102778563665257&vlt_l=0&vlt_r=0
                    iframeUrl.contains("dpx2/alm3p") -> {
                        val blobData = WebUtilIns.interceptBlob(iframeUrl, "^#EXTM(.*)")
                        blobDataTmpFile.run {
                            writeText(blobData)
                            absolutePath
                        }
                    }
                    //2. ckx1/dpx2直接拼接
                    //https://m.yhdmp.net/yxsf/player/ckx1/?vtype=video%2Fmp4&url=https%3A%2F%2Falidocs.oss-cn-zhangjiakou.aliyuncs.com%2Fres%2FMeYVOLRQjKL0npz2%2Fimg%2F38e3039d-9c9d-474d-a9c8-85cc67c173d6.gif&getplay_url=%2F_getplay%3Faid%3D20410%26playindex%3D1%26epindex%3D0%26r%3D0.7426984447268477&vlt_l=0&vlt_r=0
                    //https://m.yhdmp.net/yxsf/player/dpx2/?url=https%3A%2F%2Fv26.bdxiguavod.com%2F45df1c7fca3e1e04614c37a0cbee8659%2F627e8479%2Fvideo%2Ftos%2Fcn%2Ftos%2Dcn%2Dv%2D3506%2F2f354fc352d14aa4853b74da14ebc9f6%2F&getplay_url=%2F_getplay%3Faid%3D22263%26playindex%3D1%26epindex%3D0%26r%3D0.5827033728897308&vlt_l=0&vlt_r=0
                    iframeUrl.contains("player") -> iframeUrl.substringAfter("url=")
                        .substringBefore("&").urlDecode()
                    //3. 一般是国产动漫，通过接口解析正版源，通常返回m3u8
                    //https://chaxun.truechat365.com/?url=https://v.qq.com/x/cover/yl6lapwmmx5ivew/d0040evqzsv.html&getplay_url=%2F_getplay%3Faid%3D21471%26playindex%3D1%26epindex%3D0%26r%3D0.9602989112327851&vlt_l=0&vlt_r=0
                    else -> {
                        Log.d("无法获取链接", "开始分析iframe内源码")
                        val iframeDoc = Jsoup.parse(
                            WebUtilIns.getRenderedHtmlCode(
                                iframeUrl,
                                "(.*)\\.(gif|mp4)\\?(.*)",
                                //这里使用电脑的UA会被强制使用H5标准下的blob
                                userAgentString = ua
                            )
                        )
                        iframeDoc.select("#video").select("video").attr("src")
                    }
                }
            }
        }

        //剧集名
        val name = withContext(Dispatchers.Default) {
            async {
                document.select("[class=gohome]").select("span").first()?.text()?.replace(": ", "")
                    ?: ""
            }
        }

        return VideoPlayMedia(name.await(), videoUrl.await())
    }

}