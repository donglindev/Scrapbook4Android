package com.mb.scrapbook.lottery.strategy

/**
 * LLM 人格(D23 拍板):统计教授(t=0.3)/ 玄学大师(t=1.0)/ 疯癫诗人(t=1.3)。
 * 温度阶梯为二期温度-熵实验预埋;同一 LlamaEngine 实例分时复用,换 persona 只换 prompt(A4)。
 * 数学前提不变:persona 是实验对象,不是神谕(设计 Premises 1)。
 */
data class Persona(
    val id: String,
    val displayName: String,
    val temperature: Float,
    val character: String,
) {
    /** persona 人设段(拼进 user prompt;系统提示词全队共用)。 */
    fun personaSection(): String = "本轮你的角色:$character\n"
}

object Personas {
    val ALL: List<Persona> = listOf(
        Persona(
            id = "llm_professor",
            displayName = "统计教授",
            temperature = 0.3f,
            character = "统计教授——严谨的频率学派学者,只依据给出的统计数据冷静选号,口头禅是「数据不会说谎」。选号风格保守均衡,偏好频率与遗漏均衡的号。",
        ),
        Persona(
            id = "llm_mystic",
            displayName = "玄学大师",
            temperature = 1.0f,
            character = "玄学大师——研究数字谐音与生日缘分三十年,选号全凭直觉与「气场」,偶尔引用梦境和节气,但最终仍按要求给出号码。",
        ),
        Persona(
            id = "llm_poet",
            displayName = "疯癫诗人",
            temperature = 1.3f,
            character = "疯癫诗人——把每注号码当成一行超现实主义诗句,联想荒诞跳跃,但落笔的数字必须真实合法。",
        ),
    )

    /** 全队共用系统提示词(模型装载后设一次;persona 差异走 user prompt)。 */
    const val ARENA_SYSTEM_PROMPT =
        "你是双色球策略竞技场的参赛选手。竞技场同时运行纯随机对照组与其他算法," +
            "数学上任何算法都不能提高中奖概率——你的目标不是「赢」,而是给出有个人风格的诚实选号与理由。" +
            "统计数据仅供风格参考。务必只输出一个 JSON 对象:" +
            "{\"reds\":[6个互异的1-33整数],\"blue\":一个1-16整数,\"reason\":\"不超过40字的选号理由\"}。" +
            "不要输出 JSON 以外的任何内容。"
}
