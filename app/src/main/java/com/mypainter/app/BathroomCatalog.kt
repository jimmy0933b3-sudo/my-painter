package com.mypainter.app

/** Bathroom-world catalog. IDs remain stable across application updates. */
object BathroomCatalog {
    val items: List<DrawingItem> = listOf(
        DrawingItem("bath_toilet", "馬桶", "🚽", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_sink", "洗手台", "🚰", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_bathtub", "浴缸", "🛁", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_shower", "蓮蓬頭", "🚿", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_faucet", "水龍頭", "🚰", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_mirror", "鏡子", "🪞", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_soap", "香皂", "🧼", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_liquidsoap", "洗手乳", "🧴", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_shampoo", "洗髮精", "🧴", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_conditioner", "潤髮乳", "🧴", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_toothpaste", "牙膏", "🪥", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_toothbrush", "牙刷", "🪥", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_towel", "毛巾", "🧻", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_roll", "衛生紙捲", "🧻", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_tissues", "面紙盒", "🧻", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_cloth", "擦手巾", "🧼", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_mat", "浴室地墊", "🟦", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_sponge", "海綿", "🧽", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_hairdryer", "吹風機", "💨", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_comb", "梳子", "🪮", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_hairbrush", "髮梳", "🪮", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_brush", "馬桶刷", "🧹", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_plunger", "通馬桶吸盤", "🪠", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_mop", "拖把", "🧹", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_bin", "垃圾桶", "🗑️", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_cup", "漱口杯", "🥤", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_sanitizer", "消毒洗手液", "🧴", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_duck", "小鴨玩具", "🦆", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_curtain", "浴簾", "🚿", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126")),
        DrawingItem("bath_shelf", "置物架", "🗄️", "bathroom", listOf("M 18 18 L 126 18", "M 18 126 L 126 126"))
    )
    val drawingIds: Set<String> = items.map { it.id }.toSet()
    val descriptions: Map<String,String> = items.associate { it.id to ("這是" + it.name + "，可以觀察它的形狀，沿著線條填色") }
    fun unpack(id: String): BooleanArray = BathroomArt.unpack(id)
}
