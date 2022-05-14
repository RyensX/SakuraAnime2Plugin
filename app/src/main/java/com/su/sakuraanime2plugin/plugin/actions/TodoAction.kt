package com.su.sakuraanime2plugin.plugin.actions

import android.content.Context
import android.widget.Toast
import com.su.mediabox.pluginapi.action.Action
import com.su.mediabox.pluginapi.action.WebBrowserAction
import com.su.sakuraanime2plugin.plugin.components.Const

/**
 * 注意不能使用匿名类自定义action
 */
object TodoAction : Action() {

    override fun go(context: Context) {
        Toast.makeText(context, "正在开发中", Toast.LENGTH_LONG).show()
    }

}