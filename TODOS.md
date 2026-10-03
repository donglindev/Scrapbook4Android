# TODOS

## SSQ Arena (lib_lottery_core / lib_lottery)

### v2 手机农场协议实施
**What:** 每台手机一策略，NightlyJob 推 PeriodResult JSON 到 git 专用分支；GitHub Actions 聚合各机 JSON → 合并账本 → Pages 大屏。
**Why:** 把单机实验扩展为零后端分布式装置；git history 即防篡改账本（「手机农场」科幻感的产品形态）。
**Context:** 设计文档 R8 已定协议与 JSON schema（与 R7 filesDir 按期 JSON 统一为 PeriodResult，工程评审 C1，2026-10-02）。MVP 已在接口层预留；触发条件：MVP 三屏 + live-forward 稳定运行后。起点：docs/designs/ssq-strategy-arena-mvp.md R8。
**Effort:** L
**Priority:** P2
**Depends on:** lib_lottery 赛季稳定运行

### WorkManager 自动抓取 500.com
**What:** 端上增量抓取历史页替代手动侧载刷新，WorkManager 调度；抓取失败不破坏已有赛季数据。
**Why:** 去掉手动 CSV 侧载，历史刷新全自动闭环。
**Context:** 设计文档 Premises 2 已定后置；合规姿势（UA/频率限制）见 Open Question 2。起点：DrawRepository 侧载通道上加 fetch 分支（fetch→增量合并→冲突守卫复用 D14 规则）。
**Effort:** M
**Priority:** P2
**Depends on:** MVP 交付（手动侧载先跑通）

### 末 50 期模型回填实验开关
**What:** 显式实验开关：live 模型策略对末 50 期历史做回填回放。
**Why:** 给 LLM/VLM 选手补短期回放数据，对照 horizon-matched null 观察差异。
**Context:** 设计文档 R7 已列为可选；不做全量 3300 期回填。起点：LlamaEngine 批量推理 + 回放引擎的 requiresModel 通道。
**Effort:** M
**Priority:** P3
**Depends on:** lib_lottery 模型策略 live 稳定

### 熵纯度实验室（二期研究）
**What:** 同图 N 次采样分布、跨图分布置换检验、温度-熵曲线、prompt 改写稳定性（卡方检验选号偏好）。
**Why:** 把「LLM 熵纯度」从观察变成可发表的实验；Cross-Model 冷读的核心研究提案。
**Context:** 设计文档 Cross-Model Perspective 节 + R7 研究任务注记；依赖 live 采样数据积累。起点：VlmImageStrategy 的采样记录流。
**Effort:** L
**Priority:** P3
**Depends on:** VLM 读图策略上线 + 采样数据积累

### 正式设计系统 DESIGN.md（/design-consultation）
**What:** 经 /design-consultation 把 R11 token 最小集升级为完整设计系统文档（字体/色/版式/动效）。
**Why:** 开源后跨贡献者的一致性与发现性；MVP 期 R11 已够用（设计评审 D15=A，2026-10-02）。
**Context:** docs/designs/ssq-strategy-arena-mvp.md R11 已含暗色 token、徽章语义、状态矩阵、字号/触摸目标下限。触发条件：开源打磨期。起点：R11。
**Effort:** M
**Priority:** P3
**Depends on:** 三屏实现落地（R11 验证过可用）

### 横屏 / 投屏专项适配
**What:** 横屏布局或投屏专用呈现模式。
**Why:** 竖屏 letterbox 在大屏上留黑边；字号下限已保证可读，但横屏更沉浸。
**Context:** 设计评审 D16=A 锁竖屏为 MVP 基线（2026-10-02），横屏后置。起点：R11 token + 竞技场网格横排适配。
**Effort:** S
**Priority:** P4
**Depends on:** 演示验收通过（确认有真实横屏需求再做）

## Completed

（暂无）
