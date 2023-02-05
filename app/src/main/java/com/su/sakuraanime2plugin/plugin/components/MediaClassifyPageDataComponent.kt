package com.su.sakuraanime2plugin.plugin.components

import android.util.Log
import com.su.mediabox.pluginapi.Constant
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.components.IMediaClassifyPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.ClassifyItemData
import com.su.mediabox.pluginapi.util.PluginPreferenceIns
import com.su.mediabox.pluginapi.util.WebUtil
import com.su.mediabox.pluginapi.util.WebUtilIns
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.lang.StringBuilder

class MediaClassifyPageDataComponent : IMediaClassifyPageDataComponent {

    override suspend fun getClassifyItemData(): List<ClassifyItemData> {
        val classifyItemDataList = mutableListOf<ClassifyItemData>()
        //示例：使用WebUtil解析动态生成的分类项
        val cookies = mapOf("cookie" to PluginPreferenceIns.get(JsoupUtil.cfClearanceKey, ""))
        val document = Jsoup.parse(
            WebUtilIns.getRenderedHtmlCode(
                Const.host + "/list/", loadPolicy = object :
                    WebUtil.LoadPolicy by WebUtil.DefaultLoadPolicy {
                    override val headers = cookies
                    override val userAgentString = Const.ua
                    override val isClearEnv = false
                }
            )
        )
        document.getElementById("search-list")?.getElementsByTag("li")?.forEach {
            classifyItemDataList.addAll(ParseHtmlUtil.parseClassifyEm(it))
        }
        return classifyItemDataList
    }

    override suspend fun getClassifyData(
        classifyAction: ClassifyAction,
        page: Int
    ): List<BaseData> {
        val classifyList = mutableListOf<BaseData>()
        //https://www.yhdmp.net/list/?year=2021&pageindex=1
        val urlSb = StringBuilder(classifyAction.url ?: "")
        urlSb.append(if (urlSb.contains("?")) "&" else "?")
        urlSb.append("pageindex=${page - 1}")
        var url = urlSb.toString()
        if (!url.startsWith(Const.host))
            url = Const.host + url
        Log.d("获取分类数据", url)
        val document = JsoupUtil.getDocument(url)
        val areaElements: Elements = document.getElementsByClass("area")

        for (area in areaElements)
            for (target in area.children())
                when (target.className()) {
                    "fire l" -> {
                        val fireLChildren: Elements = target.children()
                        for (k in fireLChildren.indices) {
                            when (fireLChildren[k].className()) {
                                "lpic" -> {
                                    classifyList.addAll(
                                        ParseHtmlUtil.parseSearchEm(
                                            fireLChildren[k],
                                            url
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        return classifyList
    }

}