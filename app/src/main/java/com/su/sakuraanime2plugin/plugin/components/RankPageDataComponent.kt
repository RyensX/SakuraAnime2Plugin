package com.su.sakuraanime2plugin.plugin.components

import com.su.mediabox.pluginapi.components.ICustomPageDataComponent
import com.su.mediabox.pluginapi.data.BaseData
import com.su.mediabox.pluginapi.data.ViewPagerData
import com.su.sakuraanime2plugin.plugin.actions.CustomAction
import com.su.sakuraanime2plugin.plugin.components.Const.host
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil

class RankPageDataComponent : ICustomPageDataComponent {

    override val pageName = "排行榜"
    override fun menus() = mutableListOf(CustomAction())

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = host
        val doc = JsoupUtil.getDocument(url)

        //排行榜，包含两项
        //一周排行榜
        val weekRank =
            doc.getElementsByClass("pics")
                .first()?.let {
                    object : ViewPagerData.PageLoader {
                        override fun pageName(page: Int): String {
                            return "一周排行"
                        }

                        override suspend fun loadData(page: Int): List<BaseData> {
                            return ParseHtmlUtil.parseSearchEm(it, url)
                        }
                    }
                }
        //动漫排行榜
        val totalRank = object : ViewPagerData.PageLoader {
            override fun pageName(page: Int): String {
                return "总排行"
            }

            override suspend fun loadData(page: Int): List<BaseData> {
                return getTotalRankData()
            }
        }
        return listOf(ViewPagerData(mutableListOf(weekRank!!, totalRank)).apply {
            layoutConfig = BaseData.LayoutConfig(
                itemSpacing = 0,
                listLeftEdge = 0,
                listRightEdge = 0
            )
        })
    }

    private suspend fun getTotalRankData(): List<BaseData> {
        val document = JsoupUtil.getDocument("$host/ranklist/")
        val data = mutableListOf<BaseData>()
        document.getElementsByClass("lpic").first()?.also {
            data.addAll(ParseHtmlUtil.parseSearchEm(it, ""))
        }
        return data
    }
}