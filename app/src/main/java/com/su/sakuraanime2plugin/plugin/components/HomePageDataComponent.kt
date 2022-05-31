package com.su.sakuraanime2plugin.plugin.components

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import com.su.mediabox.pluginapi.action.ClassifyAction
import com.su.mediabox.pluginapi.action.CustomPageAction
import com.su.mediabox.pluginapi.action.DetailAction
import com.su.mediabox.pluginapi.components.IHomePageDataComponent
import com.su.mediabox.pluginapi.data.*
import com.su.mediabox.pluginapi.util.UIUtil.dp
import com.su.sakuraanime2plugin.plugin.actions.TodoAction
import com.su.sakuraanime2plugin.plugin.components.Const.host
import com.su.sakuraanime2plugin.plugin.util.JsoupUtil
import com.su.sakuraanime2plugin.plugin.util.ParseHtmlUtil.getImageUrl
import java.lang.StringBuilder

class HomePageDataComponent : IHomePageDataComponent {

    private val layoutSpanCount = 12

    override suspend fun getData(page: Int): List<BaseData>? {
        if (page != 1)
            return null
        val url = host
        val doc = JsoupUtil.getDocument(url)
        val data = mutableListOf<BaseData>()

        //1.横幅
        doc.getElementsByClass("foucs bg").first()?.apply {
            val bannerItems = mutableListOf<BannerData.BannerItemData>()
            for (em in children()) {
                when (em.className()) {
                    "hero-wrap" -> {
                        em.select("[class=heros]").select("li").forEach { bannerItem ->
                            val nameEm = bannerItem.select("p").first()
                            val ext = StringBuilder()
                            bannerItem.getElementsByTag("em").first()?.text()?.also {
                                ext.append(it)
                            }
                            nameEm?.getElementsByTag("span")?.text()?.also {
                                ext.append(" ").append(it)
                            }
                            val videoUrl = bannerItem.getElementsByTag("a").first()?.attr("href")
                            val bannerImage = bannerItem.select("img").attr("src")
                            if (bannerImage.isNotBlank()) {
                                Log.d("添加横幅项", "封面：$bannerImage 链接：$videoUrl")
                                bannerItems.add(
                                    BannerData.BannerItemData(
                                        bannerImage, nameEm?.ownText() ?: "", ext.toString()
                                    ).apply {
                                        if (!videoUrl.isNullOrBlank())
                                            action = DetailAction.obtain(videoUrl)
                                    }
                                )
                            }
                        }
                        break
                    }
                }
            }
            if (bannerItems.isNotEmpty())
                data.add(BannerData(bannerItems, 6.dp).apply {
                    layoutConfig = BaseData.LayoutConfig(layoutSpanCount, 14.dp)
                    spanSize = layoutSpanCount
                })
        }

        //2.菜单

        //排行榜
        data.add(
            MediaInfo1Data(
                "", Const.Icon.RANK, "", "排行榜",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(RankPageDataComponent::class.java)
            })

        //更新表
        data.add(
            MediaInfo1Data(
                "", Const.Icon.TABLE, "", "时间表",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = CustomPageAction.obtain(UpdateTablePageDataComponent::class.java)
            })
        //专题
        data.add(
            MediaInfo1Data(
                "", Const.Icon.TOPIC, "", "专题",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = TodoAction
                //TODO
            })
        //最近更新
        data.add(
            MediaInfo1Data(
                "", Const.Icon.UPDATE, "", "最近更新",
                otherColor = 0xff757575.toInt(),
                coverScaleType = ImageView.ScaleType.FIT_CENTER,
                coverHeight = 24.dp,
                gravity = Gravity.CENTER
            ).apply {
                spanSize = layoutSpanCount / 4
                action = TodoAction
                //TODO
            })

        //3.各类推荐
        val types = doc.getElementsByClass("firs l").first() ?: return null
        var hasUpdate = false
        val update = mutableListOf<BaseData>()
        for (em in types.children()) {
            Log.d("元素", em.className())
            when (em.className()) {
                //分类
                "dtit" -> {
                    val type = em.select("h2").select("a")
                    val typeName = type.text()
                    val typeUrl = type.attr("href")
                    if (!typeName.isNullOrBlank()) {

                        typeName.contains("更新").also {
                            if (!it && hasUpdate) {
                                //示例使用水平列表视图组件
                                data.add(HorizontalListData(update, 120.dp).apply {
                                    spanSize = layoutSpanCount
                                })
                            }
                            hasUpdate = it
                        }

                        data.add(SimpleTextData(typeName).apply {
                            fontSize = 15F
                            fontStyle = Typeface.BOLD
                            fontColor = Color.BLACK
                            spanSize = layoutSpanCount / 2
                        })
                        data.add(SimpleTextData("查看更多 >").apply {
                            fontSize = 12F
                            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                            fontColor = Const.INVALID_GREY
                            spanSize = layoutSpanCount / 2
                        }.apply {
                            action = ClassifyAction.obtain(typeUrl, typeName)
                        })
                        Log.d("视频分类", "typeName=$typeName url=$typeUrl")
                    }
                }
                //分类下的视频
                "img" -> {
                    for (video in em.select("li")) {
                        video.getElementsByClass("tname").first()?.select("a")?.first()?.apply {
                            val name = text()
                            val videoUrl = attr("href")
                            val coverUrl = video.select("img").first()?.attr("src")?.getImageUrl()
                            val episode = video.select("[target]").first()?.text()

                            if (!name.isNullOrBlank() && !videoUrl.isNullOrBlank() && !coverUrl.isNullOrBlank()) {
                                (if (hasUpdate) update else data).add(
                                    MediaInfo1Data(name, coverUrl, videoUrl, episode ?: "")
                                        .apply {
                                            spanSize = layoutSpanCount / 3
                                            action = DetailAction.obtain(videoUrl)
                                            if (hasUpdate) {
                                                paddingRight = 8.dp
                                            }
                                        })
                                Log.d("添加视频", "($name) ($videoUrl) ($coverUrl) ($episode)")
                            }
                        }
                    }
                }
            }
        }

        return data
    }
}