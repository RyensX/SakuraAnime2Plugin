package com.su.sakuraanime2plugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import com.su.mediabox.pluginapi.components.IMediaDetailPageDataComponent
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.action.PlayAction
import com.su.mediabox.pluginapi.data.*
import com.su.mediabox.pluginapi.util.TextUtil.urlEncode
import com.su.mediabox.pluginapi.util.UIUtil.dp
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil.getImageUrl
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class MediaDetailPageDataComponent : IMediaDetailPageDataComponent {

    private val mouthRegex = Regex("[\\d]")

    override suspend fun getMediaDetailData(partUrl: String): Triple<String, String, List<BaseData>> {
        var cover = ""
        var title = ""
        var desc = ""
        var score = -1F
        var upState = ""
        val url = Const.host + partUrl
        val document = JsoupUtil.getDocument(url)
        val tags = mutableListOf<TagData>()

        val details = mutableListOf<BaseData>()

        //TODO 并发优化
        //番剧头部信息
        val area: Elements = document.getElementsByClass("area")
        for (i in area.indices) {
            val areaChildren = area[i].children()
            for (j in areaChildren.indices) {
                when (areaChildren[j].className()) {
                    "fire l" -> {
                        val fireLChildren =
                            areaChildren[j].select("[class=fire l]")[0].children()
                        for (k in fireLChildren.indices) {
                            when (fireLChildren[k].className()) {
                                "thumb l" -> {
                                    cover = fireLChildren[k]
                                        .select("img").attr("src").getImageUrl()
                                }
                                "rate r" -> {   //其他信息，如标签、地区等
                                    val rateR = fireLChildren[k]
                                    title = rateR.select("h1").text()
                                    val sinfo: Elements = rateR.select("[class=sinfo]")
                                    val span: Elements = sinfo.select("span")
                                    //更新状况
                                    upState = sinfo.select("p")
                                        .run { if (size == 1) get(0) else get(1) }
                                        .text()
                                    //年份
                                    val yearEm = span[0].select("a")[0]
                                    val year = Regex("\\d+").find(yearEm.text())?.value
                                    if (year != null)
                                        tags.add(TagData(year).apply {
                                            action = ClassifyAction.obtain(
                                                yearEm.attr("href"),
                                                "", year
                                            )
                                        })
                                    //地区
                                    val animeArea = span[1].select("a")
                                    tags.add(TagData(animeArea.text()).apply {
                                        action = ClassifyAction.obtain(
                                            animeArea.attr("href"),
                                            "",
                                            animeArea.text()
                                        )
                                    })

                                    //类型
                                    val typeElements: Elements = span[2].select("a")
                                    for (l in typeElements.indices) {
                                        tags.add(TagData(typeElements[l].text()).apply {
                                            action = ClassifyAction.obtain(
                                                typeElements[l].attr("href"),
                                                "",
                                                typeElements[l].text()
                                            )
                                        })
                                    }
                                    //标签
                                    val tagElements: Elements = span[4].select("a")
                                    for (l in tagElements.indices) {
                                        var name = tagElements[l].text()
                                        if (name.length == 1 && name.matches(mouthRegex))
                                            name += "月"
                                        tags.add(TagData(name).apply {
                                            action = ClassifyAction.obtain(
                                                tagElements[l].attr("href"),
                                                "", name
                                            )
                                        })
                                    }

                                    //评分
                                    score = fireLChildren[k].select(".score").select("em")[0].text()
                                        .toFloatOrNull()
                                        ?: -1F
                                }
                                "tabs", "tabs noshow" -> {     //播放列表+header
                                    //TODO 有些动漫看不到播放列表4
                                    val playNameList =
                                        fireLChildren[k].select("[class=menu0]").select("li")
                                    val playEpisodeList = fireLChildren[k].select("[class=main0]")
                                        .select("[class=movurl]")

                                    for (index in 0..playNameList.size) {
                                        val playName = playNameList.getOrNull(index)
                                        val playEpisode = playEpisodeList.getOrNull(index)
                                        if (playName != null && playEpisode != null) {

                                            val episodes = parseEpisodes(playEpisode)

                                            if (episodes.isNullOrEmpty())
                                                continue

                                            details.add(
                                                SimpleTextData(
                                                    playName.select("li")
                                                        .text() + "(${episodes.size}集)"
                                                ).apply {
                                                    fontSize = 16F
                                                    fontColor = Color.WHITE
                                                }
                                            )

                                            details.add(EpisodeListData(episodes))
                                        }
                                    }
                                }
                                "info" -> {         //动漫介绍
                                    desc = fireLChildren[k].select("[class=info]").text()
                                }
                            }
                        }
                    }
                    "sido r" -> {         //系列动漫推荐
                        Log.d("解析系列作品", "---->")
                        areaChildren[j].getElementsByClass("pics").first()?.also {
                            val series = parseSeries(it)
                            if (series.isNotEmpty()) {
                                Log.d("其他系列作品", "size=${series.size}")
                                details.add(
                                    SimpleTextData("其他系列作品").apply {
                                        fontSize = 16F
                                        fontColor = Color.WHITE
                                    }
                                )
                                details.addAll(series)
                            }
                        }
                    }
                }
            }
        }
        return Triple(cover, title, mutableListOf<BaseData>().apply {
            add(Cover1Data(cover, score = score).apply {
                layoutConfig =
                    BaseData.LayoutConfig(
                        itemSpacing = 12.dp,
                        listLeftEdge = 12.dp,
                        listRightEdge = 12.dp
                    )
            })
            add(
                SimpleTextData(title).apply {
                    fontColor = Color.WHITE
                    fontSize = 20F
                    gravity = Gravity.CENTER
                    fontStyle = 1
                }
            )
            add(TagFlowData(tags))
            add(
                LongTextData(desc).apply {
                    fontColor = Color.WHITE
                }
            )
            add(LongTextData(douBanSearch(title)).apply {
                fontSize = 14F
                fontColor = Color.WHITE
                fontStyle = Typeface.BOLD
            })
            add(SimpleTextData("·$upState").apply {
                fontSize = 14F
                fontColor = Color.WHITE
                fontStyle = Typeface.BOLD
            })
            addAll(details)
        })
    }

    private fun parseEpisodes(element: Element): List<EpisodeData> {
        val episodeList = mutableListOf<EpisodeData>()
        val elements: Elements = element.select("ul").select("li")
        for (k in elements.indices) {
            val episodeUrl = elements[k].select("a").attr("href")
            episodeList.add(
                EpisodeData(elements[k].select("a").text(), episodeUrl).apply {
                    action = PlayAction.obtain(episodeUrl)
                }
            )
        }
        return episodeList
    }

    private fun parseSeries(element: Element): List<MediaInfo1Data> {
        val videoInfoItemDataList = mutableListOf<MediaInfo1Data>()
        val results: Elements = element.select("ul").select("li")
        for (i in results.indices) {
            val cover = results[i].select("a").select("img").attr("src").getImageUrl()
            val title = results[i].select("h2").select("a").text()
            val url = results[i].select("h2").select("a").attr("href")
            val item = MediaInfo1Data(
                title, cover, Const.host + url,
                nameColor = Color.WHITE, coverHeight = 120.dp
            ).apply {
                action = DetailAction.obtain(url)
            }
            videoInfoItemDataList.add(item)
        }
        return videoInfoItemDataList
    }

    private fun douBanSearch(name: String) =
        "·豆瓣评分：https://m.douban.com/search/?query=${name.urlEncode()}"
}