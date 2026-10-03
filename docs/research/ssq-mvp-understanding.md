# SSQ Strategy Arena MVP 设计理解报告

> 目的：作为两周实施落地的基准理解材料，面向即将动手写代码的工程师。
> 依据（primary sources，均已逐条溯源阅读）：
> - `docs/designs/ssq-strategy-arena-mvp.md`（设计文档，全文 786 行已完整阅读；行号引用为 1-based）
> - `TODOS.md`（仓库根，未跟踪，55 行）
> - `CLAUDE.md`（仓库根，仅含 gstack skill 路由规则，与本报告技术内容无关）
> - 代码库现状：`settings.gradle`、根/各模块 `build.gradle`、`config.gradle`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties`、`app/src/main/AndroidManifest.xml`、`README.md`、源码目录结构
>
> 状态说明（设计文档头部，行 6）：设计已批准（2026-09-27），工程评审完成（2026-10-02，D11-D18 全部拍板），设计评审完成（2026-10-02，D3-D16 共 14 项决定，评分 2/10 → 9/10）。最终判定（行 785）：**DESIGN CLEARED — ENG ISSUES OPEN**（工程发现已全部映射为任务 T1-T10，设计任务 TD1-TD3 并入实现）。

---

## 1. 项目定位与核心理念

### 是什么
- 原文（§Problem Statement，行 12）：「在闲置 Android 手机上运行一个本地 AI 双色球工具：多策略同场竞技、每期自动对账、诚实呈现『任何算法（含端侧 LLM）能否打赢纯随机』。定位是黑客松级观赏性 + 开源研究本地 AI 能力边界。」原始诉求「彩票投资收益最大化」已改写为「诚实实验 + 统计娱乐」（用户明确同意）。
- 数学前提（§Premises 1，行 31）：「每期独立随机，任何算法不能提高中奖概率或期望收益（返奖率≈50%）。产品是诚实实验+统计娱乐，绝不承诺收益。README 首屏声明。」
- 四个卖点（§What Makes This Cool，行 16-19）：LLM 是实验对象不是神谕；走前回放（约 3300 期历史一晚跑完赛季，「等两年」变「今晚出判决」）；零后端 git 农场（v2）；旋转矩阵的真实组合数学内核。

### 不是什么
- 不是预测工具、不是投资建议、不是收益最大化工具（§R9，行 149-151：README 首屏声明数学事实 +「本工具不预测开奖 + 不构成投资建议」；「永不做『提高中奖率』的营销文案」）。
- 旋转矩阵的诚实边界（§R4，行 115）：「旋转矩阵是真实的覆盖设计数学，但它改变的是**前提成立时的命中结构（方差形状）**，不改变期望值。」

### 设计原则
- 「奇观是发动机，诚实数学是许可证」（§Cross-Model Perspective，行 43）——观赏性投入（persona、流式理由、赛季结算）与统计严格性并重。
- 双级火箭（§Approach C，行 53-54）：纯 JVM 内核先行，Android 壳第二层，「周末不碰 Android/UI/WorkManager/模型下载」（行 42）。
- 诚实机制优先：一切数据完整性规则（settle gate、SHA-256 校验、作废重录、单源 CSV）都为「票先于开奖生成」的可信度服务（§Decision ledger D11/D12/D13/D14）。

---

## 2. 模块架构

### 文档要求的两个新模块（§R1，行 61-71）

```
lib_lottery_core/   # 纯 JVM Kotlin（kotlin("jvm")），永不 import android.*
  domain/           # Draw, NumberPool, BetScheme, BetPlan, PrizeTable, LedgerEntry
  strategy/         # Strategy 接口 + 6 个 JVM 内置实现
  replay/           # WalkForwardReplay, NullDistribution(1000次随机), PValue
  arena/            # SeasonRunner: 每期调度全部策略 → 出票 → 对账 → 聚合
  data/             # CSV 解析 (ssq_history.csv), DrawRepository
lib_lottery/        # Android 壳（AGP module），依赖 core
  infer/            # LlamaEngine JNI 移植, ModelDownload 移植, GGUF 资产管理
  strategy/         # LlmPersonaStrategy, VlmImageStrategy, CameraSeedBytesProvider
  ui/               # 三屏: ArenaScreen / LedgerScreen / DrawNightScreen
```

- PixelSeed 归属定论（§R1，行 73）：确定性出票逻辑在 core（6 个 JVM 实现之一，种子字节经 PickContext 注入）；lib_lottery 只放相机种子字节 provider（拍照→字节）。
- `settings.gradle` 增加 `include ':lib_lottery_core', ':lib_lottery'`（§R1，行 75）。lib_lottery 沿用宿主 `rootAndroid`（minSdk 21 / compileSdk 34）；core 为纯 JVM 无 minSdk（行 75）。
- 现有宿主模块（settings.gradle:1-5）：`:app`、`:lib_base`、`:lib_stock`、`:lib_views`——文档要求新模块「不得破坏现有 app/lib_base/lib_stock/lib_views 构建」（§Constraints，行 23）。
- 可复用宿主基建（工程评审 What already exists，行 594）：lib_base 的 BaseActivity/BaseFragment/BaseRepository/State（MVVM）与 utilcodex 文件工具；lib_stock 为参考模块布局（api/repository/viewmodel）。
- 并行开发策略（工程评审 Worktree parallelization strategy，行 651-660）：Lane A = T1→T5（core 串行）；Lane B = T6→T9（壳层），T1 接口冻结后可与 Lane A 并行；最后 T10（壳层 androidTest）。无跨 lane 共享模块。
- 文档未写明 `:app` 与 `:lib_lottery` 的依赖/入口集成方式（T6 只要求 `./gradlew :lib_lottery:assembleDebug`，行 688）→ 见开放问题 9.6。

---

## 3. 功能清单

默认阵容 10 位选手：6 纯 JVM + 3 LLM 人格 + 1 VLM；JVM 回放中 requiresModel 策略缺席（赛季记录缺席原因），Android 实机全量参赛（仅 live-forward）（§R3，行 105）。

### F1. Strategy 接口与六 JVM 策略（§R3，行 87-104）
- 接口：`val id / displayName / requiresModel: Boolean`；`fun pick(ctx: PickContext): BetPlan`（ctx 含 history 截至t-1、rng、params）。
- 选手：纯随机（对照组，均匀采样）、频率派（frequency 加权移植）、冷号猎手（cold_hot 移植）、遗漏保守派（missing 移植）、旋转矩阵（见 F2，≈25 注/期）、PixelSeed（种子字节 SHA-256 → kotlin.random → 确定性出号）。均 requiresModel=false，除 matrix 外各 1 注/期。
- 冷启动规则（§R5，行 120）：统计策略（频率/冷热/遗漏）在历史 < 30 期时回退均匀采样，从第 1 期起确定可复现。
- 验收：`./gradlew :lib_lottery_core:test` 全绿（§R10，行 155）；策略权重生效统计断言（行 156）；3300 期回放产出判决表、6 位 JVM 选手 p 值齐全、matrix 注数≈25/期（§Success Criteria，行 489）。

### F2. 旋转矩阵策略（§R4，行 109-115；用户点名「一等信息」）
- 语义移植自 lotterylab `rotation_matrix.py: generate_virtual_wheeling_system`，逻辑不变：「中 P 保 T」——若开出的 6 个红球全部落在 n 个号中的某个 P 子集里，保证至少一注与该前提交集命中 ≥ T。实现：位掩码（Long，n≤33）+ 前提组合全覆盖贪心（每轮选覆盖剩余前提最多的候选注），n==6 特判短路。
- 默认预设：12 红 + 1 蓝，缩水到约 25 注 = ¥50/期（对比全组合 C(12,6)=924 注）；P/T 实现期回放校准，起点 P=7/T=4（开放问题 1）。
- 验收断言（行 111）：① 全部前提场景被覆盖（测试硬断言）② 注数 ∈ [20,30] 且成本 ≤ ¥60 ③ 蓝球池全配对（1 蓝球即 25 注 ×1）。
- 竞技场接入（行 113）：pool→shrink 组合策略，每期由频率派产出 NumberPool，matrix 消费缩水成 BetPlan，独立记分；保留 matrix→filter 链式（奇偶比/大小比/和值/连号）作为高级配置。
- 性能预算（§R10，行 157）：矩阵贪心 n=12 单期 < 50ms（朴素重扫约 40-100ms 贴线，需增量覆盖更新或全覆盖即停）。

### F3. 走前回放引擎（§R5，行 119）
- `WalkForwardReplay`：期 t 时策略只见 [0, t)，出票 → 兑奖 → 入账；固定种子，确定性；验收：同种子同结果的快照测试（行 119、156）。
- 性能预算（行 157）：开发机 JVM 全量回放 3300 期 × 6 JVM 选手（含共享零分布）≤ 15 分钟（独立 tagged 任务，非 CI）。

### F4. 诚实账本与统计判决（§R5，行 121-124）
- `NullDistribution` 按注数匹配：每策略用同注量 1000 次纯随机全赛季回放构造零假设（1 注/期策略用 1 注/期 null，matrix 用 ≈25 注/期 null）；比较 stake-normalized ROI 的双侧经验分位。共享缓存：1 注/期策略共享同一份 1000 次零分布，按（每期注数, 期数, 弃权结构）缓存（工程评审 C2 澄清，省约 5 倍计算）。
- 判决规则（行 122）：双侧，单策略 α=0.05；多重比较校正绑定报表阵容——同时判决 K 位选手的报表统一用 α/K（第一周 JVM 判决表 K=6 → α/6≈0.0083；实机全阵容 K=10 → 0.005）；同一数据在 JVM 与 Android 上算出的徽章必须一致；1000 次零分布经验分辨率下限 p≈0.001，报告为 "<0.001"。
- 徽章四态（行 122 + §R11 D13，行 225）：与随机无显著差异 / 显著更差 / 显著更好（=「数据警报：结果异常，请检查数据与流程」）/ 样本不足。
- live 模型选手判决（行 123）：live 期数 < 30 时只报 ROI/命中分布 +「样本不足，暂不判决」；≥ 30 期后用 horizon-matched null（1000 次同长度、同每期注数、含弃权结构的随机赛季；弃权期按 0 注计）。
- 账本字段（行 124）：累计投入 / 累计中奖 / ROI / 命中分布（6+1…0+0）/ p 值 / 判决。一等奖按 500 万、二等奖按 15 万假设值计（显著标注「假设值」）。

### F5. 历史数据 CSV（§R6，行 128；D11=A 行 271-277）
- 来源：lotterylab SQLite `data/lottery.db` 表 `draw_results`，`sqlite3 -header -csv` 单行命令导出 `ssq_history.csv`（约 3300 期）；schema `period,red1..red6,blue,date,source`。
- 打包位置（D11=A）：唯一副本置于 `lib_lottery_core/src/main/resources/ssq_history.csv`；core 测试与 Android 壳均走 classpath 读取，`DrawRepository` 封装 ClassLoader；不做 app assets 副本。
- 验收：CSV 解析边界（缺列/越界号/乱序期号）测试 + 资源缺失报错路径（§R10，行 156；T2，行 672）。

### F6. 期结果导入（§R6，行 129；D14=A 行 360-362）
- 手动录入开奖号（6+1 数字键盘）；录入确认回显（排序后 6 红+1 蓝，确认后才入账）；结算前可作废重录、结算后不可变；侧载 CSV 与已结算期不一致时告警且只追加新期；侧载替换 CSV 刷新历史（写 filesDir 可写副本，D11 accepted scope 行 277）；on-device 抓 500.com（WorkManager）后置。
- 验收：录入即时校验（重复红球/越界/不足 6 红 = 即时红字 + 确认按钮禁用，§R11 状态矩阵行 199）；androidTest 覆盖录入确认/作废重录状态机、侧载 CSV 冲突守卫（D17=A，行 446）。

### F7. PixelSeed 种子链（§R6，行 130；§R7，行 139）
- 回放：打包资产照片字节 → SHA-256 → 赛季种子 → 每期 seed = hash(赛季种子 ‖ 期号)，全赛季确定性可复现；实机默认一季一照片，可选每期重拍。
- 卖点级交互：「同一张照片永远同一组号」（§R7，行 139）。
- 验收：PixelSeed 种子链测试（§R10，行 156）；T3「PixelSeed 同照片同票」（行 674）。

### F8. settle gate 结算门槛（§R7，行 141；D12=A 行 299-306）
- `SeasonRunner.settle(period)` 前置为该期在场选手全部出票或显式弃权；按期 JSON 断言 `pickAt < enteredAt`，违例 fail loudly；UI 拒绝结算并列出未完成选手。
- 弃权语义纯净：弃权仅指模型解析失败（D12 对比表，行 287）。

### F9. Android 壳三屏 + 端侧 AI（§R7，行 134-141；§R11）
- 模型策略仅 live-forward 参赛，不做端上 3300 期模型回填（「末 50 期回填」为后续可选实验开关）。
- LlamaEngine JNI 从 MiniCPM-V-demo-Android 移植（llama.cpp + GGUF）；ModelDownloadService 移植改造：两模型槽位（MiniCPM5-2B Q4 约 1.5GB；MiniCPM-V-4.6 GGUF+mmproj），断点续传，用户主动触发。
- LLM 人格出票：统计 JSON → persona 提示词 → 流式输出 → 结构化 JSON 票据；解析失败重试 1 次后弃权并记录（「弃权也是实验数据」）。
- VLM 读图：照片或抽帧 → 视觉描述 → 红/蓝区权重偏好 → 出号；live 默认每个开奖夜一张用户拍摄照片（与 PixelSeed 种子照片相互独立），可配整季一张；回放/重播用打包资产照片。
- 运行时完整性约束四条（行 141）：settle gate（F8）；模型校验——下载清单 JSON 附每文件 size+SHA-256，失败删除自动重下（D13=A）；ABI 基线——llama.cpp .so 仅打包 arm64-v8a，非 arm64 设备显示「设备不支持」不崩溃（D16=A）；内存调度——同一时刻至多一个模型常驻，LLM 人格 ×3 复用同一 LlamaEngine 实例（换 persona 只换 prompt），VLM 与 LLM 分时加载。
- 验收：真机冒烟（下载→校验→加载→推理）+ 非 arm64 降级提示（T7，行 692）；LLM fixture 语料解析成功率套件（T8，行 696）；三屏 UI 与流式走手工演示脚本（§R10，行 156）。

### F10. 首启向导与赛季管理（§R11 D7=A，行 184-188）
- 三步向导（首次启动自动进入）：① 建赛季（默认名 S1、起始期 = CSV 最新期可改，文案「live 记录从此期开始，JVM 回放不受影响」）② 模型下载两槽位选择 +「稍后下载」跳过 ③ 完成进入竞技场 + 横幅。设置内提供赛季切换/重建/归档。

### F11. 模型下载管理页（§R11 D8=A，行 190）
- 状态机：未开始 → 下载中（进度条+已续传 GB）→ 校验中（SHA-256）→ 校验失败自动重下（红条+原因）→ 就绪（变出票入口）；非 arm64 设备槽位卡显示「设备不支持」。

### F12. 诚实合规（§R9，行 149-151）
- README 首屏数学声明 + 不预测 + 不构成投资建议；账本页常驻免责声明条；永不做提高中奖率营销文案。
- 验收（§Success Criteria，行 491）：他人可 `git clone && ./gradlew test` 复现判决。

### F13. 农场协议 v2 预留（§R8，行 145）
- 仅定义 `PeriodResult` JSON schema（见 §4），不做手机端 git 客户端；NOT in scope（行 584）。

### F14. 工程与质量（§R10，行 155-157）
- CI 门禁：`./gradlew :lib_lottery_core:test`（秒级，不含全量回放）。
- 关键测试清单（行 156）：矩阵全覆盖、模型校验不变量、回放确定性、p 值正确性、CSV 边界、冷启动回退确定性、PrizeTable 九级命中边界、策略权重统计断言、PixelSeed 种子链、settle gate、30 期判决切换与弃权计 0 注、DrawRepository 资源缺失报错、null 共享缓存键等价；壳层 androidTest 五类（赛季持久化+进程重启恢复、settle gate 拒绝/放行、录入状态机、侧载冲突守卫、模型清单校验——D17=A 行 446）。

### 明确 NOT in scope（工程评审，行 583-589；TODOS.md:5-51）
- v2 手机农场（P2）、WorkManager 自动抓取（P2）、末 50 期模型回填（P3）、熵纯度实验室二期（P3）、全 ABI 模型支持、Compose UI；TODOS.md 另列：正式 DESIGN.md（P3）、横屏/投屏适配（P4）。

---

## 4. 数据模型与存储

### 领域对象（§R2，行 79-82；对齐 lotterylab/models.py 不变量）
| 实体 | 字段 | 不变量 |
|---|---|---|
| `Draw` | period, reds: SortedSet<6>, blue, date, source | 红 1-33 不重复已排序；蓝 1-16 |
| `NumberPool` | redPool, bluePool, strategyName | 红池 ≥ 6、蓝池 ≥ 1 |
| `BetScheme` | reds×6, blue | 构造即校验 |
| `BetPlan` | schemes, totalCost = schemes×¥2, generatedAt, config | 非法抛 `IllegalArgumentException`（对齐 Python `__post_init__`） |
| `PrizeTable` | 九级奖级 | 三等及以下固定金额；一/二等奖浮动，回放按 500 万/15 万假设并显著标注「假设值」 |
| `LedgerEntry` | （§R5 账本字段）累计投入/累计中奖/ROI/命中分布/p 值/判决 | |

- `PickContext`：history（截至 t-1）、rng、params（§R3，行 90）。

### 存储
- **core**：`ssq_history.csv` 唯一副本在 `lib_lottery_core/src/main/resources/`，classpath 读取（§R6，行 128；D11=A）。
- **Android 壳**（§R7，行 135）：`filesDir/seasons/<season>/` 下按期 JSON（票据+理由+对账）+ 账本聚合 JSON；**MVP 不引 Room**；模型下载状态与偏好存 **DataStore**；进程重启后从文件恢复赛季。
- **PeriodResult 统一 JSON schema**（§R8，行 145）：`strategy_id, period, tickets, reason, settlement, abstention, timestamps, model_meta`——R7 filesDir 按期 JSON 与 v2 农场同一 schema。
- 按期 JSON 含时间戳断言 `pickAt < enteredAt`（§R7，行 141）。
- 侧载刷新写 filesDir 可写副本，不动打包源（D11 accepted scope，行 277）。

---

## 5. UI/页面结构（§R11，行 159-231）

### 导航（D3=A，行 161）
BottomNavigationView 三 tab：竞技场（HOME）/ 诚实账本 / 开奖夜；开奖夜 tab 带状态角标（待开奖/出票中/待录入/已结算）。Material 组件现成，零新依赖。

### 三屏
- **ArenaScreen（D4=A，行 163-175）**：卡片三行——行1 策略名+判决徽章；行2 ROI 大数字（盈利绿/亏损红/对照组灰）；行3 本期 6+1 票号胶囊。网格按累计 ROI 降序，对照组固定末位灰卡；弃权置灰+「弃权」角标；模型未就绪显示下载态。ASCII 线框在行 166-174（文档称原 temp 线框已丢失，以此为准，行 8）。
- **LedgerScreen（D5=A，行 177）**：每选手一行：判决徽章 + ROI 次级 + 7 段微型命中分布条形图（6+1…0+0，自定义 View，lib_views 模式）；免责声明条固定底部常驻；一/二等奖假设值 = 表头星注 + 首次结算弹一次说明；live<30 期显示「样本不足，暂不判决」徽章；回放缺席选手 = 灰行「回放缺席」（状态矩阵，行 197）。
- **DrawNightScreen（D6=A，行 179-182）**：单 Activity 三态状态机：出票态（persona 名+大字号流式文本）→ 锁定态（全员票已亮+等待录入空位；settle gate 未完成名单在此显示）→ 结算态（逐策略命中揭晓后落徽章）。

### 演出规格（D10/D11/D12=A，行 205-208）
- 流式呈现：聚光灯逐个轮演（一次只演一个 persona），完成者收进侧栏缩略票卡，顶部进度「2/3 出票中」，切换 400ms 淡入。
- 重播模式：右上角水印「重播 · 第 N 期」；重播结算直接展示该期 JVM 判决表完整徽章。官方演示脚本（≥5 min，验收用）：竞技场 30s → 重播开奖夜 3 min → 账本+免责 1 min → live 状态 30s。
- 结算揭晓：按 ROI 序逐行 stagger（票号胶囊亮 → 命中数翻出 → 徽章落下，间隔 ~600ms），对照组最后揭晓；全 app 动画仅此一处。

### 交互状态矩阵（D9=A，行 193-203）
8 行 × 5 列（加载/空/错误/成功/部分），覆盖：竞技场卡片、诚实账本、开奖夜流式、录入、作废/锁定、重启恢复（横幅「第 N 期出票中断，已恢复」）、双照片（权限拒绝→从相册选；两张并排预览「左：PixelSeed 种子 / 右：VLM 读图」）、投屏（keep-screen-on）。实施时应逐项对照验收（TD3，行 741）。

### 视觉与无障碍
- 徽章语义（D13=A，行 225）：无差异=中性灰；显著更差=橙红下行箭头；显著更好=警示黄+感叹号+「数据警报」文案；样本不足=虚线灰边。ROI 绿/红与徽章语义两套色不混用。
- Token 最小集（D14=A，行 227）：暗色全局，深色背景 #0E1116 级；流式正文 ≥22sp、ROI 28-34sp、票号胶囊 16sp、标签 ≥12sp；等宽数字特性防跳动；触摸目标 ≥48dp；对比度 ≥4.5:1；persona 用首字 monogram 文字头像。
- 无障碍（D16=A，行 231）：锁竖屏；徽章/命中图/票号胶囊全部带语义 contentDescription；「减少动画」开启时 stagger 瞬时呈现；keep-screen-on。

---

## 6. 技术栈与约束（文档要求 vs 代码库实际）

| 项 | 文档要求 | 代码库实际 | 差异 |
|---|---|---|---|
| AGP | 7.4.2（§Constraints 行 23） | `com.android.tools.build:gradle:7.4.2`（build.gradle:18） | 一致 |
| Gradle | 7.5（行 23） | `gradle-7.5-bin.zip`（gradle/wrapper/gradle-wrapper.properties:3） | 一致 |
| JDK | 11（行 23；Premises 4 行 34） | 仓库文件未钉死（gradle.properties 无 java.home 配置）；commit f3d9a7f 提及 OpenJDK11 升级 | 需本机/CI 确认 JDK 11（开放问题 9.10） |
| Kotlin | 1.9.21（build.gradle:4；§R1 行 75） | `ext.kotlin_version = "1.9.21"`（build.gradle:4） | 一致；`kotlin('jvm')` module 与宿主 buildscript 风格兼容（行 75） |
| compileSdk / targetSdk | 34（§R1 行 75） | 34 / 34（config.gradle:3,6） | 一致 |
| minSdk | lib_lottery 沿用宿主 21（SC3 更正，行 244）；core 纯 JVM 无 minSdk（行 75） | 21（config.gradle:5） | 一致 |
| UI 栈 | View + DataBinding + lib_base MVVM，不引 Compose（D15=A，行 388） | app/build.gradle:30-32 `dataBinding { enabled = true }`；lib_base 有 BaseActivity/BaseFragment/BaseViewModel/BaseRepository/State；无 compose 插件/依赖 | 一致，零新依赖 |
| Material | BottomNavigationView 现成（设计评审，行 723） | material 1.4.0（config.gradle:16） | 一致（零新依赖） |
| 测试 | JUnit4 同栈零新框架（行 596；app/build.gradle:64） | `junit:junit:4.+`（app/build.gradle:64） | 一致 |
| DataStore | 模型下载状态与偏好存 DataStore（§R7 行 135） | config.gradle 无 datastore 依赖 | **缺口：需新增依赖**（开放问题 9.7） |
| Room | MVP 不引（行 135） | 无 | 一致 |
| WorkManager | 后置（Premises 2，行 32） | 无 | 一致 |
| ABI | llama.cpp .so 仅打包 arm64-v8a（D16=A，行 416） | app abiFilters armeabi-v7a/arm64-v8a/x86（app/build.gradle:23-26）；蓝本 jniLibs 实测仅 arm64-v8a（行 25） | arm64 已在 app filters 内；lib_lottery 侧仅打包 arm64 |
| jvmTarget | 文档未指定 core | 宿主 1.8（app/build.gradle:44-46） | core 实现期自定（开放问题 9.10） |
| 仓库风格 | Groovy buildscript（非 plugins DSL） | 同（build.gradle:3-21） | 一致 |
| 算法蓝本 | lotterylab（Python，七阶段 pipeline；路径 `D:/gitprojects/devtoolslab/x-projects/pythonlab/lotterylab`，lottery.db 434KB 在位）（行 24） | 仓库外 | Kotlin 移植源，不重造（行 592） |
| 推理蓝本 | MiniCPM-V-demo-Android（LlamaEngine.kt/ModelDownloadService/VideoFrameExtractor.kt/TTS；路径 `D:/gitprojects/devtoolslab/x-projects/androidlab/MiniCPM-V-Apps/MiniCPM-V-demo-Android`）（行 25） | 仓库外 | 移植源 |
| 模型 | MiniCPM5-2B（Q4 约 1.5GB）+ MiniCPM-V-4.6（~1.3B GGUF+mmproj）（行 26、136） | 无 | 下载链接/体积待确认（The Assignment，行 510） |
| Maven 源 | 未提及 | 阿里云/华为云镜像 + jcenter（build.gradle:6-15） | 对开源复现（行 491）可能有影响（开放问题 9.11） |

其他宿主依赖可用（config.gradle:37-68）：coroutines 1.7.3、lifecycle 2.6.2（ViewModel/LiveData/ktx）、appcompat 1.6.1、core-ktx 1.12.0、recyclerview 1.2.1、BaseRecyclerViewAdapterHelper 3.0.10、retrofit 2.9.0、okhttp 3.14.9、utilcodex 1.30.6、glide、fresco、multidex。

---

## 7. 现状差距（Gap 分析）

### 已有（可直接复用）
1. 四模块宿主构建完好（settings.gradle:1-5；各 build.gradle 均引用 config.gradle 统一配置）。
2. lib_base MVVM 基建：BaseActivity/BaseFragment/BaseViewModelActivity/BaseViewModelFragment/BaseViewModel/BaseRepository/State（lib_base/src/main/java/com/mb/scrapbook/lib/base/mvvm/，约 32 文件）；网络层 RetrofitFactory 等。
3. lib_views 自定义 View 模式（约 25 文件）——7 段微型命中条形图的实现范式参考（设计评审行 723）。
4. lib_stock 模块布局参考（api/repository/viewmodel，7 文件）。
5. JUnit4 测试依赖与模板测试（app/build.gradle:64；各模块 ExampleUnitTest/ExampleInstrumentedTest）。
6. utilcodex 文件工具（config.gradle:55；工程评审行 594）。
7. 设计文档 + TODOS.md 已就位（git 未跟踪：`?? TODOS.md`、`?? docs/`）。
8. app 现有代码为示例性质（app/src 下 40 文件、18 个 .kt，全部 module/example 演示代码 + 壳），不构成阻碍。

### 缺失（文档要求但不存在）
1. **模块未建**：无 lib_lottery_core、lib_lottery 目录；settings.gradle:2-5 仅含宿主四模块（R-CSV runtime evidence 行 251 亦确认「新模块未建」）。
2. **无 ssq_history.csv**：全仓 Glob `**/*.csv` 无结果；The Assignment（行 510）要求先从 lotterylab 导出提交——开工前置条件未完成。
3. **无 core 代码**：domain/strategy/replay/arena/data 全部待写（T1-T5）。
4. **无 Android 壳**：infer（LlamaEngine JNI/jniLibs/模型清单 JSON）、strategy（LLM persona/VLM/相机 provider）、ui 三屏全部待写（T6-T9、TD1-TD3）。
5. **无 DataStore 依赖**（§R7 行 135 要求）。
6. **无诚实机制 androidTest**：仅模板 ExampleInstrumentedTest（D17=A 要求五类测试，行 446）。
7. **README 不合规**：当前仅一行 `# Scrapbook4Android`（README.md:1）；R9 要求首屏数学事实声明（行 149）。
8. **无 CI**：无 .github 目录；Distribution Plan 要求 Actions 跑 core 测试 + app assemble（行 497）。
9. **无 app 入口集成**：当前 launcher 是 ExampleActivity（app/src/main/AndroidManifest.xml:15-23，package com.mb.scrapbook.app）；竞技场入口未定义。
10. **模型资产未确认**：MiniCPM5-2B Q4 下载链接与实际体积未确认（The Assignment，行 510；开放问题 3/4）。

---

## 8. 两周实施建议

依据：§Success Criteria（行 489-492，第一周末/第二周起两里程碑）、工程评审 T1-T10 任务表（行 665-704，含优先级与估时）、设计评审 TD1-TD3（行 730-741）、Next Steps 周末计划（行 502-506）、并行 Lane 策略（行 649-660）。

**前置（D0，开工前）**：完成 The Assignment（行 510）——sqlite3 导出 ssq_history.csv + 确认 MiniCPM5-2B Q4 链接与体积。不做这两件事 T2/T7 无法启动。

### 第 1 周（Lane A：core，全部 P1）→ 里程碑 A = Success Criteria 第 1 条
| 天 | 任务 | 内容与验收 |
|---|---|---|
| D1-D2 | T1 | core 骨架 + domain 模型与不变量校验；`./gradlew :lib_lottery_core:test` 全绿且 `:app:assembleDebug` 不受影响（行 668）；**接口冻结点：此后 Lane B 可并行**（行 659） |
| D3 | T2 | CSV 提交为 core main resources 单源 + DrawRepository classpath 读取；边界测试 + 资源缺失报错（行 672） |
| D3-D5 | T3 | 六 JVM 策略（最重项）：matrix 全覆盖硬断言 + 注数 [20,30] + 权重/种子链测试（行 676） |
| D5-D6 | T4 | 回放引擎 + null 共享缓存 + p 值判决：同种子同账本快照 + α/K 绑定报表 + 缓存键等价（行 680） |
| D6-D7 | T5 | settle gate + 账本聚合 + PeriodResult schema：settle 拒绝/放行 + pickAt<enteredAt + 30 期切换边界（行 684） |

里程碑 A 验收（行 489）：3300 期回放产出判决表，6 位 JVM 选手 p 值齐全，matrix ≈25 注/期。
建议：第 1 周末即启动 T7 的真机冒烟（JNI/模型加载是最大不确定项，蓝本路径行 25 已确认）。

### 第 2 周（Lane B：壳层，P2）→ 里程碑 B = Success Criteria 第 2/4 条
| 天 | 任务 | 内容与验收 |
|---|---|---|
| D8-D9 | T7 提前量 | LlamaEngine/ModelDownloadService 移植 + SHA-256 校验 + ABI/内存调度；下载→校验→加载→推理冒烟（行 692） |
| D8-D10 | T6 + TD3 | 三屏 View/DataBinding 按 R11 全规格（含状态矩阵逐项对照）（行 688、738-741） |
| D10 | TD1 | 首启三步向导 + 赛季管理（行 730-733） |
| D10-D11 | TD2 | 模型下载管理页两槽位状态机（行 734-737） |
| D11-D12 | T8 | LLM persona 出票（解析重试→弃权）+ fixture 套件（行 693-696） |
| D12 | T9 | 录入确认/作废重录状态机 + 侧载 CSV 冲突守卫（行 697-700） |
| D13 | T10 + 收尾 | androidTest 五类 + 演示脚本彩排 ≥5 分钟不冷场（行 701-704、492）；README 数学声明（R9） |

### 排期风险提示（有据）
- 文档自排为三周（Next Steps 行 506：「第三周起：VLM 读图 + 开奖夜 UI + 开源打磨」）；两周内 VLM（T8 的 VLM 部分）应视为 stretch goal——Success Criteria 第 2 条（行 490）只要求「三屏可用 + MiniCPM5-2B 人格流式出票」，未含 VLM。是否两周内必含 VLM 需用户确认（开放问题 9.8）。
- 任务 human 估时合计约 23-24 人日（T1-T10 + TD1-TD3 行 665-704、730-741），超出 2 周单人日历；文档同时给出 CC 估时（每任务 15min-4h），两周转档成立的前提是 CC 辅助开发按文档估时兑现。

---

## 9. 开放问题

### 文档自带（§Open Questions，行 481-485）
1. 12+1 缩水到 25 注的精确 P/T 预设（实现期回放校准，起点 P=7/T=4）。
2. 500.com 端上抓取的稳定性与合规姿势（UA/频率限制）——MVP 用打包 CSV 规避。
3. MiniCPM-V-4.6 在 llama.cpp Android 的 mmproj 细节与实际 token 速度——需真机验证。
4. 模型分发：HuggingFace 直连还是镜像/Releases 附带下载清单（国内网络）。
5. LLM 出票解析的鲁棒格式（JSON mode 或语法约束解码）。

### 实施前需向用户确认（文档未明确）
6. **app 与 lib_lottery 的集成方式**：`:app` 是否依赖 `:lib_lottery`、竞技场入口如何挂（当前 launcher 是 ExampleActivity，app/src/main/AndroidManifest.xml:15）——T6 只验收 `:lib_lottery:assembleDebug`，未涉及 app 集成。
7. **DataStore 依赖**：用 Preferences DataStore 还是 Proto；版本与声明位置（config.gradle 统一加，还是 lib_lottery 局部加）——文档只说「存 DataStore」（§R7 行 135）。
8. **两周范围是否含 VLM**：文档第三周才排 VLM（行 506），Success Criteria 两周期只含 LLM 人格（行 490）；若两周必含 VLM 需砍 TD3 打磨深度或加时。
9. **PrizeTable 固定奖级金额**：文档说「三等奖及以下固定金额」但未列数值（§R2 行 82）——需确认按官方现行金额（三等 3000/四等 200/五等 10/六等 5）或另行给定。
10. **core 的 JVM toolchain/jvmTarget 与 JDK 11 落地方式**：仓库未钉死 JDK（见 §6 表）；CI 与本机需统一为 JDK 11。
11. **开源形态**：「GitHub 公开仓库（Scrapbook4Android 或拆独立仓库，实现期定）」（行 496）+ 根 build.gradle 的阿里云/jcenter 镜像对外部 `git clone && ./gradlew test` 复现（行 491）的影响。
12. **LLM 三 persona 的定义**（名称/人设提示词/温度）：文档只定「N=3 为 MVP 默认，配置可调」（§R3 行 102），未给 persona 内容。
13. **模型下载清单 JSON 的初始内容**：需与模型分发渠道（开放问题 4）一起定（D13=A 要求清单含 size+SHA-256）。

---

*报告完。生成于 2026-10-02，分支 dev-ai-lottery（HEAD 2638ae6）。*

---

## 10. 实施更正记录（2026-10-02，T1 完成时）

- **代码库此前从未成功 `assembleDebug`**（存量问题，与新增模块无关）：AGP 7.4.2 builder 内嵌的 r8 4.0.52 无法在 compileSdk 34 平台下 dex 任何含 Kotlin metadata 的构件（崩溃于 `com.android.tools.r8.kotlin.H`）。本机实验定位：CLI 裸跑 D8 成功，加 `--lib android-34/android.jar` 即复现。仓库 core-ktx 1.12 强制 compileSdk≥34，不可降级回避。
- **修复（提交 d4394e5）**：根 build.gradle 的 buildscript classpath 在 AGP 之前声明 `com.android.tools:r8:8.1.56`，以类加载顺序遮蔽内嵌旧 r8；`gradle.properties` 钉 `org.gradle.java.home` 为本机 JDK 11（Gradle 7.5 无法在 JDK 21 上运行——本机默认 JDK 是 21）。开放问题 9.10 本机侧已闭，开源复现前应改为 CI/用户环境注入。
- Kotlin 维持 1.9.21 与文档一致（r8 8.1.56 可正常 dex 其 metadata 构件）。
- **T1 完成（提交 54d22ef）**：lib_lottery_core 六个 domain 模型（构造即校验）+ 21 测试全绿；`ssq_history.csv`（3490 期，03001→26093）入库 `main/resources`（D11=A；The Assignment 的导出项完成）。
- PrizeTable 按官方六级奖实现（文档「九级」未定义结构，采用官方规则 + 用户拍板金额，边界测试覆盖全部中奖命中组合）——开放问题 9.9 已闭。
