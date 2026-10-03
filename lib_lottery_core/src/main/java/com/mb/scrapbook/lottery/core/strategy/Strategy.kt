package com.mb.scrapbook.lottery.core.strategy

import com.mb.scrapbook.lottery.core.domain.BetPlan
import com.mb.scrapbook.lottery.core.domain.Draw
import kotlin.random.Random

/** 策略出票上下文(设计 R3):期 t 策略只见 [0,t),走前回放的完整性由回放引擎(T4)保证。 */
class PickContext(
    val period: String,
    /** 升序历史,截至 t-1。 */
    val history: List<Draw>,
    /** 共享随机源,回放用固定种子保证可复现。 */
    val rng: Random,
    /** PixelSeed 种子照片字节(R1:种子字节经 PickContext 注入;JVM 回放用打包资产照片)。 */
    val seedBytes: ByteArray = ByteArray(0),
)

/** 参赛策略(设计 R3)。requiresModel=false 的实现永不触碰 Android。 */
interface Strategy {
    val id: String
    val displayName: String

    /** false=纯 JVM 可跑;true=需端侧模型(Android)。 */
    val requiresModel: Boolean
    fun pick(ctx: PickContext): BetPlan
}
