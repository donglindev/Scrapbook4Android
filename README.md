# Scrapbook4Android · SSQ 策略竞技场(双色球诚实实验)

> **数学事实声明(本工具的立身之本)**
> 双色球每期独立随机,**任何算法(包括端侧 LLM)都不能提高中奖概率或期望收益**,返奖率约 50%。
> 本应用是一个诚实实验 + 统计娱乐装置:多策略同场竞技、每期自动对账、以统计检验回答
> 「谁能打赢纯随机」——预期答案是没有人能。**本工具不预测开奖,不构成投资建议,永不承诺提高中奖率。**
> 一/二等奖为浮动奖金,账本按假设值计入(一等奖 500 万 / 二等奖 15 万,显著标注)。

## 它做什么

- **竞技场**:6 位纯 JVM 策略(纯随机对照组 / 频率派 / 冷号猎手 / 遗漏保守派 / 旋转矩阵 / PixelSeed)+ 3 位端侧 LLM 人格 + 1 位 VLM(W3),同场竞技。
- **走前回放**:3490 期历史逐期重演(期 t 只见 [0,t)),1000 次同注量随机赛季构造零分布,双侧 p 值 + α/K 多重比较校正,产出判决表。
- **开奖夜**:聚光灯流式出票 → settle gate(全员出票/弃权才可结算,`pickAt < enteredAt` 断言)→ 手动录入开奖号(回显确认,结算后不可变)→ stagger 揭晓。
- **端侧 AI**:llama.cpp(GGUF)驱动 MiniCPM5-2B 三人格(温度阶梯 0.3/1.0/1.3),解析失败重试一次后显式弃权——弃权也是实验数据。

## 构建

要求 JDK 11(仓库已钉 `org.gradle.java.home`,他机请自行调整)、Android SDK。

```bash
git clone <repo> && cd Scrapbook4Android
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew :lib_lottery_core:test      # 核心单元测试(秒级)
```

## 复现判决表(诚实实验的可验证性)

```bash
FULL_REPLAY=1 ./gradlew :lib_lottery_core:test --tests "*FullHistory*"
# 输出 MILESTONE-A 判决表(seed=2026L,同种子同表):6 位 JVM 选手 3490 期,约 0.5 分钟
```

示例结果(2026-10-03):五位 1 注/期选手全部「与随机无差异」(ROI -0.72 ~ -0.81,符合返奖率预期);
旋转矩阵(46 注/期)因 160K 注命中头奖级尾部事件触发「显著更好 = 数据警报」徽章(p=0.006 < α/6≈0.0083)——
这正是本实验的设计语义:**任何「打赢随机」的结果首先被当作数据警报,而不是奇迹**。

## 模块

| 模块 | 说明 |
|---|---|
| `lib_lottery_core` | 纯 JVM Kotlin 内核(永不 import android.*):domain / strategy / replay / arena / data |
| `lib_lottery` | Android 壳:三屏 UI(View+DataBinding)、llama.cpp JNI、模型下载与校验、赛季持久化 |
| `app` | 宿主壳(launcher = 竞技场主 Activity) |

## 诚实机制

- settle gate:票先于开奖生成(时间戳断言,违例 fail loudly)
- 结算后不可变;侧载 CSV 冲突只告警、只追加新期
- 模型下载 SHA-256/MD5 校验,失败自动重下
- 多重比较校正绑定报表阵容(K=6 → α/6);live < 30 期不判决
- 纯随机对照组是科学底座,永远在场

详细设计:`docs/designs/ssq-strategy-arena-mvp.md`(含全部决策记录 D11-D28)。词汇表:`CONTEXT.md`。
