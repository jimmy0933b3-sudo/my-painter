package com.mypainter.app

/**
 * The coloring-page ID is the one source of truth for its picture description.
 * The displayed item name always comes from ShopCatalog, so renaming a picture
 * also changes what children hear. Do not duplicate item names in voice scripts.
 *
 * The descriptions below were checked against the 36 embedded V2 line drawings
 * in the six-column artwork contact sheet, not against the outdated emoji paths.
 */
object ItemSpeech {
    private val visualClues: Map<String, String> = mapOf(
        // 水果: from the displayed black-and-white coloring illustration
        "apple" to "圓圓的蘋果上面有一片葉子",
        "banana" to "香蕉彎彎長長的",
        "grape" to "好多顆小葡萄長在一起，旁邊還有葉子",
        "strawberry" to "草莓上有一粒一粒的小籽，頂端有葉子",
        "watermelon" to "切開的西瓜裡面有一顆顆西瓜籽",
        "orange" to "圓圓的橘子上面有葉子",
        // 蔬菜
        "carrot" to "紅蘿蔔長在土裡，上面有葉子",
        "tomato" to "番茄圓圓的，頂端有葉子",
        "corn" to "玉米有一排排的玉米粒，外面包著葉子",
        "broccoli" to "花椰菜的上面像一朵朵小花，下面有粗粗的莖",
        "cucumber" to "小黃瓜長長的，表面有小點點",
        "pumpkin" to "南瓜有一瓣一瓣的瓜身，上面有瓜蒂和葉子",
        // 麵包甜點
        "toast" to "這是一片吐司，外面有麵包邊",
        "bread" to "長長的法國麵包上面有幾道斜斜的切痕",
        "donut" to "甜甜圈中間有一個洞，外面有糖霜",
        "cake" to "這是一片有分層奶油和櫻桃的蛋糕",
        "cookie" to "圓圓的餅乾上面有一塊塊巧克力豆",
        "croissant" to "可頌彎彎的，表面有一層層的麵皮",
        // 飲品乳製品: distinguish the apple and grape cartons correctly
        "milk" to "牛奶裝在有摺角的紙盒裡",
        "juice" to "這盒蘋果汁插著吸管，盒子上還畫著蘋果",
        "water" to "瓶裝水有瓶蓋，瓶身一圈一圈的",
        "yogurt" to "這杯草莓優格上面有杯蓋，杯子上畫著草莓",
        "cheese" to "這一塊起司有大大小小的圓洞",
        "soy" to "這是一盒葡萄汁，紙盒上畫著一串葡萄",
        // 雜貨: identify the actual illustrated food rather than its old title
        "rice" to "碗裡裝著煮好的白飯，一粒一粒的",
        "eggs" to "有完整的雞蛋，還有一顆裂開的蛋殼",
        "cereal" to "碗裡有好多一圈圈的早餐穀片",
        "pasta" to "好多根空心的通心粉放在一起",
        "jam" to "草莓果醬裝在玻璃罐裡，標籤上畫著草莓",
        "can" to "這是一罐番茄罐頭，上面畫著番茄",
        // 購物玩具
        "cart" to "購物車有籃子、握把和輪子",
        "basket" to "購物籃有提把，籃身有一條條的洞",
        "register" to "收銀機有顯示螢幕、按鈕和抽屜",
        "teddy" to "泰迪熊有圓圓的耳朵、鼻子和腳掌",
        "toycar" to "玩具車有車窗、車門和輪子",
        "ball" to "足球上面有一塊塊不同形狀的拼片"
    )

    fun description(item: DrawingItem): String =
        visualClues.getValue(item.id)

    fun intro(item: DrawingItem): String =
        "這是${item.name}。${description(item)}。一起幫${item.name}塗上漂亮的顏色吧！"

    fun completed(item: DrawingItem): String =
        "太棒了！${item.name}畫好了，現在可以為它塗上喜歡的顏色。"

    val describedIds: Set<String> get() = visualClues.keys
}
