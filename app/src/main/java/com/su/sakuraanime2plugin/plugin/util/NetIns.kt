package com.su.sakuraanime2plugin.plugin.util

import com.su.sakuraanime2plugin.plugin.danmaku.OyydsDanmakuApis
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

val retrofit = Retrofit.Builder()
    .client(OkHttpClient())
    .baseUrl("https://api.github.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val oyydsDanmakuApis by lazy(LazyThreadSafetyMode.NONE) { retrofit.create<OyydsDanmakuApis>() }