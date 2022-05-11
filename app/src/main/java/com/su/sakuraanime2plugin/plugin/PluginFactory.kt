package com.su.sakuraanime2plugin.plugin

import com.su.mediabox.pluginapi.components.*
import com.su.mediabox.pluginapi.IPluginFactory
import com.su.sakuraanime2plugin.plugin.components.*

/**
 * 每个插件必须实现本类
 *
 * 注意包和类名都要相同，且必须提供公开的无参数构造方法
 */
class PluginFactory : IPluginFactory() {

    override val host: String = Const.host

    override fun <T : IBasePageDataComponent> createComponent(clazz: Class<T>) = when (clazz) {
        IVideoPlayPageDataComponent::class.java -> VideoPlayPageDataComponent()
        IMediaSearchPageDataComponent::class.java -> MediaSearchPageDataComponent()
        IMediaDetailPageDataComponent::class.java -> MediaDetailPageDataComponent()
        IMediaClassifyPageDataComponent::class.java -> MediaClassifyPageDataComponent()
        IHomePageDataComponent::class.java -> HomePageDataComponent()
        //自定义页面，需要使用具体类而不是它的基类（接口）
        RankPageDataComponent::class.java -> RankPageDataComponent()
        UpdateTablePageDataComponent::class.java -> UpdateTablePageDataComponent()
        else -> null
    } as? T

}