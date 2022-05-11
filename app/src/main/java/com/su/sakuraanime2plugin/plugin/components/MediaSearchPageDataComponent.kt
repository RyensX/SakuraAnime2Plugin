package com.su.sakuraanime2plugin.plugin.components

import android.net.Uri
import com.su.mediabox.pluginapi.components.IMediaSearchPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.sakuraanime2plugin.plugin.components.Const.host
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil
import org.jsoup.select.Elements

class MediaSearchPageDataComponent : IMediaSearchPageDataComponent {

    override suspend fun getSearchData(keyWord: String, page: Int): List<BaseData> {
        val searchResultList = mutableListOf<BaseData>()
        //https://www.yhdmp.net/s_all?kw=关键词&pagesize=24&pageindex=0
        val url = "${host}/s_all?kw=${Uri.encode(keyWord, ":/-![].,%?&=")}&pageindex=${page - 1}"
        val document = JsoupUtil.getDocument(url)
        val lpic: Elements = document.getElementsByClass("area")
            .select("[class=fire l]").select("[class=lpic]")
        searchResultList.addAll(ParseHtmlUtil.parseSearchEm(lpic[0], url))
        return searchResultList
    }

}