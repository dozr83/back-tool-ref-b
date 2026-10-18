# back-tool-ref-b（方案验证配套仓库）

本仓库**不是正式版本**，只是 [back-tool](https://github.com/dozr83/back-tool) 的一次架构方案验证配套，用于保存「摘要能力重构」的参考实现（方案 B）。

- **正式版本**：[dozr83/back-tool](https://github.com/dozr83/back-tool) —— 所有功能、构建方式、配置说明均以那边为准
- **本仓库定位**：独立保留一版与正式仓库不同的实现思路，仅供对照、评估设计取舍

## 与正式版本的关系

- 由 back-tool 仓库整体复制而来，提交历史与复制时点一致
- 工作区中保留了一份未提交的摘要计算模块重构改动（显式方法 + 遍历骨架方案），与正式版本采用的 Config/枚举方案是同一问题的两种设计
- 不与正式版本同步维护，更新不定期

## 摘要模块的职责拆分

本仓库将摘要能力由单一 `DigestCalculator` 大类按功能拆为 6 个类：

| 类 | 职责 |
|---|---|
| `DigestWalker`（包私有） | 遍历引擎：剪枝遍历 + 注入计算函数 + 相对路径转换，业务不可直接访问 |
| `DirMd5Digest` | 目录 MD5 摘要计算、按 MD5 分组 |
| `DirLenDigest` | 目录长度（LEN）摘要计算 |
| `DirLenMd5Digest` | 目录长度+MD5（LEN_MD5）摘要计算 |
| `DirDigestComparer` | 按路径比对（`compareByPath` + `PathDiffResult`） |
| `DigestManifest` | 清单文件读写（.size / .md5 / .version） |

拆分原则：计算、遍历、比对、清单读写各归各类，每个计算类内含 4 个方法（File 键 / 相对路径键 × 默认规则 / 自定义规则），核心实现各 1 行，转发重载不重复逻辑。

## 其它

除上述差异外，功能、环境要求、构建运行方式与 [back-tool](https://github.com/dozr83/back-tool) 完全相同，请直接参考其 README。
