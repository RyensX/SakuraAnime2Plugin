package com.su.sakuraanime2plugin.plugin.danmaku

data class OyydsDanmaku(
    val code: Int,
    val `data`: Data,
    val msg: String
) {

    data class Data(
        val `data`: List<DataX>,
        val episode: Episode,
        val total: Int
    )

    data class DataX(
        val color: String,
        val content: String,
        val createdAt: String,
        val episodeId: String,
        val id: String,
        val ip: String,
        val time: String,
        val type: String,
        val updatedAt: String,
        val userId: String
    )

    data class Episode(
        val createdAt: String,
        val goodsId: String,
        val id: String,
        val number: String,
        val updatedAt: String
    )
}