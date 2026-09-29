# TaCZinTetra

将 TaCZ 枪械与 Tetra 模块化装备组合，维护枪械配置、兼容层和运行测试。

## 工程布局

- Gradle 工程：[TaCZinTetra](./TaCZinTetra/)
- 工程详细说明：[TaCZinTetra/README.md](./TaCZinTetra/README.md)
- 项目注册信息：[PycodersMod.projects.json](../../PycodersMod.projects.json)
- 工作区统一测试入口：[launch.ps1](../../launch.ps1)
- 启动参数填写说明：[launch-parameters.md](../../launch-parameters.md)

## 构建

在 TaCZinTetra 目录执行 .\gradlew.bat clean build。

工程使用 Gradle Java Toolchain 声明所需 Java 版本。机器本地的 JDK 路径位于工作区 local-config，不写入 Mod 仓库。

## 标识

| 项目 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.4.16 |
| Java | 17 |
| Mod ID | taczintetra |
| Java Package | com.pycoder.taczintetra |

Mod ID、Registry Namespace 和存档标识保持原值。工程目录、Gradle group、Java package 和 GitHub 仓库名属于工程组织信息。
