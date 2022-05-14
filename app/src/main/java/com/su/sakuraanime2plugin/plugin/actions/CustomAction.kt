package com.su.sakuraanime2plugin.plugin.actions

import android.content.Context
import android.widget.Toast
import com.su.mediabox.pluginapi.action.Action
import com.su.mediabox.pluginapi.action.WebBrowserAction
import com.su.sakuraanime2plugin.plugin.components.Const

/**
 * 注意不能使用匿名类自定义action
 */
class CustomAction : Action() {

    init {
        extraData = "打开数据源"
    }

    override fun go(context: Context) {
        Toast.makeText(context, extraData!! as String, Toast.LENGTH_SHORT).show()
        WebBrowserAction.obtain(Const.host).go(context)
    }

}