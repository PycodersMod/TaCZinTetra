# TaCZinTetra 可配置模块化枪实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with review checkpoints.

**Goal:** 按 `task.md` 的 1–20 顺序完成 TaCZinTetra 的真实联动，并提供用户指定默认内容的配置驱动最简可启动版本。

**Architecture:** 使用配置加载器、纯逻辑 profile/状态层和 TaCZ/Tetra 适配层分离职责。Tetra 负责模块数据与工作台，TaCZ 适配器负责严格生命周期，服务端负责权威状态；透明资源只作为可替换的视觉占位层。

**Tech Stack:** Minecraft 1.20.1、Forge 47.4.16、Tetra 6.17.0、Mutil 6.3.0、TaCZ 1.1.8-hotfix、Java 17、Gradle 8.5、JUnit 5。

**Spec:** `docs/superpowers/specs/2026-09-01-configurable-modular-gun-design.md`

## Global Constraints

- 不创建组合爆炸的 TaCZ `gun_id`；运行时模板 ID 与模块组合分离。
- 所有具体平衡数值、材料和型号必须来自 JSON 配置；代码只保留槽位和协议边界。
- Tetra 主部件固定为 Body、Magazine、Barrel；副部件固定为 Stock、Optic、Grip。
- 不允许铁锭、金锭、木板等原材料直接作为结构件输入。
- 核心编译时不引用第三方附属 Mod 类。
- 公开文档不得写入本机路径、用户名、JDK 路径、内部日志或启动命令。
- 每个阶段先执行对应测试，再执行完整 `clean build`；运行时阶段追加 `runData` 和隔离 PID 窗口测试。

---

### Task 1：依赖/API 与映射基线

**Files:** `task.md`、`docs/TiT_API核验_20260901.md`、Gradle 依赖与 run 配置。

- [ ] 执行 `./gradlew.bat clean` 并记录结果。
- [ ] 重新核对本地 TaCZ/Tetra/Mutil JAR、版本、SHA-256、`IGun`/`AbstractGunItem` 生命周期和 refmap 映射。
- [ ] 将真实 NBT 字段、事件点和未知 API 写入核验文档；禁止用旧版本猜测。
- [ ] 运行 `./gradlew.bat test build --no-daemon --console=plain`，确认基线。

### Task 2：配置与数据生成框架

**Files:** 创建 `config` 包、默认 JSON schema、配置测试；修改 `TaCZinTetra`、`DefinitionReloadListener`、Tetra 数据资源。

- [ ] 先写配置生成、读取、缺失字段和非法值测试并确认失败。
- [ ] 实现 `modules.json` 的原子生成、schema version、有限数值校验、警告和安全回退。
- [ ] 填入五种材料、五种枪身、口径/霰弹/榴弹、常规弹匣、三种副部件的默认键与中文显示名。
- [ ] 让 reload listener 创建配置快照，并使 profile resolver 只消费快照。
- [ ] 通过目标测试、`clean build`、`runData`。

### Task 3：Tetra 模块与透明资源最简闭环

**Files:** `data/tetra/modules/**`、`data/tetra/materials/**`、`assets/taczintetra/models/**`、透明 PNG、语言文件、模块安装适配器。

- [ ] 先写模块键、槽位、材料映射和占位纹理测试。
- [ ] 实现总枪图标及枪管、枪身、弹匣、倍镜、枪托、握把的透明模型资源。
- [ ] 将显示名改为 `TaCZ模块化枪`、`枪管`、`枪身`、`弹匣`、`倍镜`、`枪托`、`握把`。
- [ ] 让加工台和全息球能够枚举并安装最小枪的三主件及副件。
- [ ] 运行资源校验、`runData` 和窗口页面验收。

### Task 4：完整 profile 与状态/替换运行时

**Files:** `logic/**`、`runtime/**`、NBT 适配器、模块替换和打磨适配器。

- [ ] 为组合、缺失模块、材料、打磨、特殊词条和非法状态补充失败测试。
- [ ] 接通配置快照、ItemStack、模块状态和每手 runtime context；按固定顺序连乘并统一裁剪。
- [ ] 接通 Body/Barrel/Magazine 替换清理、honing/improvement、耐久和 broken 状态。
- [ ] 完成对应单测和全量构建。

### Task 5：TaCZ 单枪生命周期桥

**Files:** `item/ModularGunItem.java`、`compat/**`、Mixin/Accessor/Invoker 配置、TaCZ lifecycle adapter。

- [ ] 先写 `IGun` 识别、严格 `AbstractGunItem` 调用点和生命周期契约测试。
- [ ] 实现最小单枪模板：开火、枪机、换弹、模式、弹道、ADS、动画和 NBT 桥；Mixin 只做类型/调用缝合。
- [ ] 验证普通 TaCZ 枪未被污染，并完成进入世界和一次真实射击。

### Task 6：资源插入、批次换弹与网络权威

**Files:** `network/**`、资源插入适配器、Menu/Slot 事件、reload adapter、HUD 数据接口。

- [ ] 先写服务端权威、容器插入、剩余物、不可提取、批次和重复包测试。
- [ ] 接入 E 背包、箱子、标准 Menu、兼容适配器和手中枪右键插入。
- [ ] 接入真实 TaCZ reload 开始/tick/完成/中断事件；完成时才扣资源和增加弹量。
- [ ] 接入 sequence、hand、stack identity、broken/overheat 验证与同步回包。
- [ ] 完成单人、局域网/专服和延迟重复包验收。

### Task 7：N/M 射击、耐久、修理与热量

**Files:** `logic/**`、TaCZ projectile adapter、Tetra repair/enchantment adapter、热量 NBT/runtime。

- [ ] 为普通/霰弹/榴弹、burst、耐久附魔、broken、repair、连续冷却和过热补充失败测试。
- [ ] 接通 N 次独立发射与每发 M 个弹丸、TaCZ 伤害/穿透/爆炸能力和一次耐久扣除。
- [ ] 接通原版耐久附魔概率、材料 repair agent、一次性修理数量和 Tetra 附魔区域。
- [ ] 接通懒更新热量、射击加热、指数冷却、曲线配置和过热锁定。
- [ ] 完成真实射击、换弹、耗损、修理和热量窗口验收。

### Task 8：输入路由、双持、HUD 与模型动作

**Files:** `client/**`、`runtime/**`、HUD、Shared-Rig/渲染适配器、动画桥。

- [ ] 先写四种输入状态、重绑定按键、双持互斥、快捷栏/F 中断、HUD 和渲染实例测试。
- [ ] 接入 TaCZ 重绑定 fire/reload/mode 按键，完成单持、双手枪、长枪副手保留规则。
- [ ] 接入双手独立 profile/heat/recoil/animation、主副手 HUD 和缺失资源回退。
- [ ] 接入 Shared-Rig、第一/第三人称、左手第三人称和 ADS/动作透传。
- [ ] 完成第一/第三人称、双持和普通 TaCZ 回归窗口验收。

### Task 9：扩展 API、Starter 和最终测试矩阵

**Files:** `api/**`、starter 配方、Tetra growth resources、`src/test/**`、`task.md`。

- [ ] 先写事件、`ResourceInsertionAdapter`、`ExternalCapabilityAdapter` 和模拟附属测试。
- [ ] 提供 bullet/reload/explosion 事件、资源通道/供弹/弹道扩展注册，不引用第三方类。
- [ ] 通过原版工作台提供 starter pistol，禁用不必要的 TaCZ 制造前台并保留必要模式切换。
- [ ] 执行 `task.md` 第 19 节完整矩阵、普通 TaCZ 回归、`clean build`、`runData`、隔离 PID `runClient`。
- [ ] 逐项将有代码、资源、测试和运行证据的条目标为完成；公开文档做隐私扫描；只有完成定义全部满足后结束目标。
