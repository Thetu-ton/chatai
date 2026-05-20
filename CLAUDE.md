# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" -> "Write tests for invalid inputs, then make them pass"
- "Fix the bug" -> "Write a test that reproduces it, then make it pass"
- "Refactor X" -> "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] -> verify: [check]
2. [Step] -> verify: [check]
3. [Step] -> verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## 5. 模块文档规范 (Module Documentation)

### 5.1 每个模块必须输出 README.md

每完成一个模块（或子模块）的开发，必须在模块**根目录**生成一份 `README.md`，内容包括：

| 章节 | 内容 |
|------|------|
| **模块概述** | 这个模块做什么，在整个项目中的角色 |
| **环境要求** | 语言版本、SDK 版本、依赖的第三方库 |
| **核心 API** | 所有公开的接口/类/方法签名，按功能分组 |
| **必须实现的接口** | 使用者（其他模块的 Agent）必须实现的接口清单，标注优先级(P0/P1) |
| **可选接口** | 按需实现的接口 |
| **使用示例** | 最小可运行的集成代码片段 |
| **待实现部分** | 标记 TODO 或留给其他模块的接口约定 |
| **构建/运行** | 如何构建、测试、运行这个模块 |

### 5.2 读取优先级规则

当 Agent 需要访问项目中的某个文件夹时，必须遵守以下顺序：

```
1. 先读 <folder>/README.md    <- 优先读文档
2. 判断信息是否足够完成任务
   - 足够 -> 直接使用文档中的信息
   - 不足 -> 再深入访问文件夹内的具体文件
```

此规则适用于所有子目录，包括但不限于：`Android/`、`aiinteract/`、`memory/` 以及将来新增的任何模块目录。

### 5.3 文档更新

- 模块的 README.md 随代码变更同步更新
- 新增了公开 API -> 必须更新 README
- 修改了接口签名 -> 必须更新 README
- 如果 README 与实际代码不一致，以代码为准但需标记为待修复
 ### 5.4
 - 本模块更新后，需要其他模块进行对应的更新，则要将todo单独写到一个总的todo.md文件里，这个在大文件夹下，所有模块都可以访问