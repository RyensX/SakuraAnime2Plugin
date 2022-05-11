package com.su.sakuraanime2plugin

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.mediabox.pluginapi.components.IMediaClassifyPageDataComponent
import com.su.mediabox.pluginapi.components.IMediaDetailPageDataComponent
import com.su.mediabox.pluginapi.components.IVideoPlayPageDataComponent
import com.su.sakuraanime2plugin.plugin.PluginFactory
import com.su.sakuraanime2plugin.plugin.components.MediaDetailPageDataComponent
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * 组件单元测试示例
 */
@RunWith(AndroidJUnit4::class)
class DataComponentUnitTest {

    @Test
    fun testMediaClassifyPageDataComponent() = runBlocking {
        val factory = PluginFactory()
        val dataComponent =
            factory.createComponent(IMediaClassifyPageDataComponent::class.java)?.apply {
                val data = getClassifyItemData()
                assert(!data.isNullOrEmpty())
                data.forEach {
                    Log.d("*查看数据", it.toString())
                }
            }
        assertNotNull(dataComponent)
    }

    @Test
    fun testMediaDetailPageDataComponent() = runBlocking {
        val factory = PluginFactory()
        val dataComponent =
            factory.createComponent(IMediaDetailPageDataComponent::class.java)?.apply {
                val data = getAnimeDetailData("/showp/22146.html").third
                assert(!data.isNullOrEmpty())
                data.forEach {
                    Log.d("*查看数据", it.toString())
                }
            }
        assertNotNull(dataComponent)
    }

}