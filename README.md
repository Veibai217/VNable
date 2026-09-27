[中文]

<div align="center">

# VNable

**开源的视觉小说引擎**

一个基于「剧本包」的 Android 视觉小说引擎：创作者只需编写 JSON 与打包资源，玩家侧一键导入即可游玩。

</div>

## 项目简介

VNable 是一个运行在 Android 上的视觉小说引擎。它把「游戏」与「内容」彻底分离：

- **引擎本体**只负责剧本解析、场景演出、物品与存档管理；
- **剧本包**是一个普通的 zip 压缩包，里面放 JSON 剧本和图片、音频资源。

创作者不需要安装任何引擎或 SDK——只需一个文本编辑器、一个文件管理器、基础的 JSON语法，就可以做出大部分适配的视觉小说。

## 快速开始

前往本仓库的 Releases 页面下载，或自行构建

构建环境要求：Android Studio（含 AGP 8.13）与 JDK 17。

## 剧本包创作指南

### 前置要求

1. 一个任意文本编辑器
2. 一个任意文件管理器 / 压缩工具
3. 基础的 JSON 语法知识

### 剧本包结构

```VNable
剧本包.zip
├─ Main.json    --必须包含的剧本入口
├─ Info.json    --必须包含的剧本包信息
├─ Val.json    --可选存在的物品注册表
└─ Res/    --资源目录（文件夹名不限）
```
> 提示：zip 内允许再套一层顶层文件夹（例如 `MyPack/Main.json`），引擎会自动定位；
> macOS 打包产生的 `__MACOSX/`、`.DS_Store` 等杂项文件会被自动忽略。
> 以相同 UID 重新导入会覆盖旧版本。

### `Info.json` —— 剧本包信息

```json
{
    "name": "剧本包名字",
    "uid": "com.example.mypack",
    "ver": "1.0",
    "type": [
        "Official",
        "Alpha"
    ]
}
```

| 键值 | 说明 |
|-----|------|
| `name` | 剧本包名称；缺省时使用 `uid` |
| `uid` | 剧本包唯一标识，**必须填写且全局唯一**；不能为空，不能包含 `/` 或 `\`，不能为 `.` 或 `..` |
| `ver` | 剧本包版本；缺省为 `"1.0"` |
| `type` | 标签数组，用于剧本包列表展示，如 `Official`、`Alpha` 等 |

### `Val.json` —— 物品注册表

```json
{
    "key": "/Res/icon_key",
    "flower": "/Res/icon_flower"
}
```

键为**物品 ID**，值为物品图标的资源引用。只有在 `Val.json` 中注册过的物品，
才会在游戏内物品栏中显示图标；未注册的物品仍可参与增减与条件判定，只是不显示图标。

### `Main.json` —— 场景与剧本语法

`Main.json` 的根对象由若干「场景」组成，键为场景名，值为场景对象。
**引擎固定从 `Main.json` 的 `main` 场景开始游戏**，因此剧本必须包含键 `main`。

```json
{
    "main": { },
    "scene_a": { },
    "scene_b": { }
}
```

**场景对象字段总表：**

| 键值 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `bg` | string | 否 | 背景资源引用；不填时背景将被清空 |
| `audio` | object | 否 | 音频设置，见下文 |
| `actor` | object | 否 | 人物与台词，见下文；不填则无立绘、无台词 |
| `btn` | array | 否 | 选项按钮列表，台词显示完毕后出现；不填则点击屏幕推进 |
| `add` | object | 否 | 物品增加表 `{ 物品ID: 数量 }`，由 `fun` 为 `"add"` 的按钮触发 |
| `sub` | object | 否 | 物品减少表 `{ 物品ID: 数量 }`，由 `fun` 为 `"sub"` 的按钮触发 |
| `if` | object | 否 | 条件分支，见下文 |
| `next` | object | 否 | 默认推进目标 `{ "dir": "...", "target": "..." }` |

**`audio` —— 背景音乐**

```json
"audio": {
    "dir": "/Res/bgm_room",
    "mode": "loop"
}
```

| 键值 | 说明 |
|------|------|
| `dir` | 音频文件资源引用 |
| `mode` | `"loop"` 为循环播放；`"single"` 为单次播放 |

**`actor` —— 人物与台词**

```json
"actor": {
    "res": "/Res/hero",
    "side": "r",
    "name": "小雨",
    "msg": "房间里静悄悄的……"
}
```

| 键值 | 说明 |
|------|------|
| `res` | 立绘资源引用；可不填或资源不存在，此时不显示立绘 |
| `side` | 立绘位置：`"l"` 为左，`"r"` 为右；缺省为 `"l"` |
| `name` | 人物名称；不填则不显示名牌 |
| `msg` | 人物台词 |

**`btn` —— 选项按钮**

```json
"btn": [
    {
        "id": "pick_key",
        "text": "拿起钥匙",
        "fun": "add"
    },
    {
        "text": "查看大门",
        "fun": {
            "dir": "Main.json",
            "target": "door"
        }
    }
]
```

| 键值 | 说明 |
|------|------|
| `id` | 按钮标识，可省略 |
| `text` | 按钮文本 |
| `fun` | 按钮行为，支持两种写法 |

`fun` 的两种写法：

1. **字符串**（函数式）：
   - `"add"`：被点击时，把本场景 `add` 表中的物品加入背包；
   - `"sub"`：被点击时，从背包扣除本场景 `sub` 表中的物品；
   - 其他任意字符串（如 `"next"`，也是缺省值）：被点击时按普通流程推进（评估 `if`、走 `next`）。
2. **对象**（跳转式）：`{ "dir": "文件名.json", "target": "场景名" }`，被点击时直接跳转到目标场景，优先级高于 `if` 与 `next`。

**`if` —— 条件分支**

```json
"if": {
    "mode": "above",
    "cond": {
        "key": 1
    },
    "then": { "dir": "Main.json", "target": "opening" },
    "else": { "dir": "Main.json", "target": "locked" }
}
```

| 键值 | 说明 |
|------|------|
| `mode` | 判定模式：`"above"` 持有数量**不少于**阈值；`"below"` **不超过**阈值；`"equal"` **恰好等于**阈值 |
| `cond` | 判定条件表 `{ 物品ID: 阈值 }`；**所有条目同时满足**才算通过 |
| `then` | 条件通过时执行的分支 |
| `else` | 条件不通过时执行的分支；可省略 |

`then` / `else` 分支支持三种写法：

1. **字符串**：如 `"next"`，按普通流程推进；
2. **跳转对象**：`{ "dir": "...", "target": "..." }`，跳转到目标场景；
3. **嵌套 `if`**：实现多级判定，最多嵌套 3 层。

### 场景寻址规则

所有跳转（`btn.fun` 对象、`if.then/else`、`next`）都使用统一的地址格式：

```
"dir": "目标 JSON 文件名（含 .json 扩展名）",
"target": "该文件中的场景键"
```

- `dir` 是**包内 JSON 文件的相对路径**，须包含 `.json` 扩展名，例如 `"Main.json"`；允许子目录，如 `"Sub/Chapter2.json"`；
- `target` 是该文件根对象中的场景键，例如 `"main"`；
- 一个剧本包可以包含任意多个 JSON 场景文件，跨文件跳转只需改写 `dir`。

### 资源引用规则

`bg`、`actor.res`、`audio.dir`、`Val.json` 中的图标都使用统一的资源引用：

- 路径**相对剧本包根目录**，开头的 `/` 可写可不写，如 `"/Res/bg_room"`；
- **扩展名可以省略**：引擎按格式列表自动匹配；
  - 图片：`webp` → `png` → `jpg` → `jpeg` → `gif` → `bmp`
  - 音频：`wav` → `mp3` → `ogg` → `m4a` → `flac` → `aac`
- 也可以写出完整文件名（含扩展名）直接引用。

### 完整的场景示例

```json
{
    "main": {
        "bg": "/res/bg",
        "audio": {
            "dir": "/res/audio",
            "mode": "loop"
        },
        "actor": {
            "res": "/res/actor",
            "name": "Admin",
            "side": "l",
            "msg": "Hello World!"
        },
        "btn": [
            {
                "id": "id",
                "text": "text",
                "fun": "add"
            },
            {
                "id": "id",
                "text": "text",
                "fun": "sub"
            },
            {
                "id": "id",
                "text": "text",
                "fun": "if"
            },
            {
                "id": "id",
                "text": "text",
                "fun": {
                    "dir": "/end.json",
                    "target": "end"
                }
            }
        ],
        "add": {
            "item": 1
        },
        "sub": {
            "item": 1
        },
        "if": {
            "mode": "above",
            "cond": {
                "item": 1
            },
            "then": {
                "dir": "/end.json",
                "target": "end"
            },
            "else": {
                "dir": "/end.json",
                "target": "end"
            }
        },
        "next": {
            "dir": "/end.json",
            "target": "end"
        }
    }
}
```


---


[English]

<div align="center">

# VNable

**Open-source visual novel engine**

An Android visual novel engine based on "script packs": creators only need to write JSON and package assets, while players can import and play with one tap.

</div>

## Project Introduction

VNable is a visual novel engine running on Android. It completely separates "game" and "content":

- **The engine itself** is only responsible for script parsing, scene presentation, item and save management;
- **A script pack** is an ordinary zip archive containing JSON scripts and image/audio assets.

Creators do not need to install any engine or SDK—just a text editor, a file manager, and basic JSON syntax knowledge, and they can create visual novels compatible with most features.

## Quick Start

Go to this repository's Releases page to download, or build it yourself.

Build environment requirements: Android Studio (with AGP 8.13) and JDK 17.

## Script Pack Creation Guide

### Prerequisites

1. Any text editor
2. Any file manager / compression tool
3. Basic JSON syntax knowledge

### Script Pack Structure

```VNable
ScriptPack.zip
├─ Main.json    -- Required script entry
├─ Info.json    -- Required script pack info
├─ Val.json    -- Optional item registry
└─ Res/    -- Resource directory (folder name unrestricted)
```
> Tip: The zip may contain an extra top-level folder (for example, `MyPack/Main.json`), and the engine will locate it automatically;
> Miscellaneous files such as `__MACOSX/` and `.DS_Store` generated by macOS packaging will be ignored automatically.
> Re-importing with the same UID will overwrite the old version.

### `Info.json` — Script Pack Info

```json
{
    "name": "Script Pack Name",
    "uid": "com.example.mypack",
    "ver": "1.0",
    "type": [
        "Official",
        "Alpha"
    ]
}
```

| Key | Description |
|-----|------|
| `name` | Script pack name; uses `uid` if omitted |
| `uid` | Unique script pack identifier, **must be provided and globally unique**; cannot be empty, cannot contain `/` or `\`, cannot be `.` or `..` |
| `ver` | Script pack version; defaults to `"1.0"` |
| `type` | Tag array, used for script pack list display, such as `Official`, `Alpha`, etc. |

### `Val.json` — Item Registry

```json
{
    "key": "/Res/icon_key",
    "flower": "/Res/icon_flower"
}
```

Keys are **item IDs**, and values are resource references to item icons. Only items registered in `Val.json` will display icons in the in-game inventory; unregistered items can still participate in addition/subtraction and condition checks, but will not display icons.

### `Main.json` — Scenes and Script Syntax

The root object of `Main.json` consists of several "scenes"; keys are scene names, and values are scene objects.
**The engine always starts the game from the `main` scene in `Main.json`**, so the script must contain the key `main`.

```json
{
    "main": { },
    "scene_a": { },
    "scene_b": { }
}
```

**Scene object field summary:**

| Key | Type | Required | Description |
|------|------|------|------|
| `bg` | string | No | Background resource reference; if omitted, the background will be cleared |
| `audio` | object | No | Audio settings, see below |
| `actor` | object | No | Character and dialogue, see below; if omitted, there is no sprite and no dialogue |
| `btn` | array | No | Choice button list, appears after dialogue is displayed; if omitted, tap the screen to advance |
| `add` | object | No | Item addition table `{ itemID: quantity }`, triggered by buttons whose `fun` is `"add"` |
| `sub` | object | No | Item subtraction table `{ itemID: quantity }`, triggered by buttons whose `fun` is `"sub"` |
| `if` | object | No | Conditional branch, see below |
| `next` | object | No | Default advance target `{ "dir": "...", "target": "..." }` |

**`audio` — Background Music**

```json
"audio": {
    "dir": "/Res/bgm_room",
    "mode": "loop"
}
```

| Key | Description |
|-----|------|
| `dir` | Audio file resource reference |
| `mode` | `"loop"` for loop playback; `"single"` for single playback |

**`actor` — Character and Dialogue**

```json
"actor": {
    "res": "/Res/hero",
    "side": "r",
    "name": "Xiaoyu",
    "msg": "The room is quiet..."
}
```

| Key | Description |
|-----|------|
| `res` | Sprite resource reference; may be omitted or the resource may not exist, in which case no sprite is displayed |
| `side` | Sprite position: `"l"` for left, `"r"` for right; defaults to `"l"` |
| `name` | Character name; if omitted, no name plate is displayed |
| `msg` | Character dialogue |

**`btn` — Choice Buttons**

```json
"btn": [
    {
        "id": "pick_key",
        "text": "Pick up the key",
        "fun": "add"
    },
    {
        "text": "Inspect the door",
        "fun": {
            "dir": "Main.json",
            "target": "door"
        }
    }
]
```

| Key | Description |
|-----|------|
| `id` | Button identifier, optional |
| `text` | Button text |
| `fun` | Button behavior, supports two forms |

Two forms of `fun`:

1. **String** (functional):
   - `"add"`: when clicked, add the items in this scene's `add` table to the inventory;
   - `"sub"`: when clicked, subtract the items in this scene's `sub` table from the inventory;
   - Any other string (e.g. `"next"`, which is also the default): when clicked, advance through the normal flow (evaluate `if`, follow `next`).
2. **Object** (jump-style): `{ "dir": "filename.json", "target": "sceneName" }`; when clicked, jump directly to the target scene, with priority higher than `if` and `next`.

**`if` — Conditional Branch**

```json
"if": {
    "mode": "above",
    "cond": {
        "key": 1
    },
    "then": { "dir": "Main.json", "target": "opening" },
    "else": { "dir": "Main.json", "target": "locked" }
}
```

| Key | Description |
|-----|------|
| `mode` | Check mode: `"above"` held quantity is **not less than** threshold; `"below"` **not more than** threshold; `"equal"` **exactly equal to** threshold |
| `cond` | Condition table `{ itemID: threshold }`; **all entries must be satisfied at the same time** to pass |
| `then` | Branch executed when the condition passes |
| `else` | Branch executed when the condition fails; can be omitted |

`then` / `else` branches support three forms:

1. **String**: e.g. `"next"`, advance through the normal flow;
2. **Jump object**: `{ "dir": "...", "target": "..." }`, jump to the target scene;
3. **Nested `if`**: implement multi-level checks, up to 3 levels of nesting.

### Scene Addressing Rules

All jumps (`btn.fun` object, `if.then/else`, `next`) use a unified address format:

```
"dir": "target JSON filename (including .json extension)",
"target": "scene key in that file"
```

- `dir` is the **relative path of the JSON file within the pack**, must include the `.json` extension, e.g. `"Main.json"`; subdirectories are allowed, e.g. `"Sub/Chapter2.json"`;
- `target` is the scene key in that file's root object, e.g. `"main"`;
- A script pack can contain any number of JSON scene files; cross-file jumps only require changing `dir`.

### Resource Reference Rules

`bg`, `actor.res`, `audio.dir`, and icons in `Val.json` all use unified resource references:

- Paths are **relative to the script pack root directory**; the leading `/` is optional, e.g. `"/Res/bg_room"`;
- **The extension can be omitted**: the engine automatically matches according to the format list;
  - Images: `webp` → `png` → `jpg` → `jpeg` → `gif` → `bmp`
  - Audio: `wav` → `mp3` → `ogg` → `m4a` → `flac` → `aac`
- You can also write the complete filename (including extension) for direct reference.

### Complete Scene Example

```json
{
    "main": {
        "bg": "/res/bg",
        "audio": {
            "dir": "/res/audio",
            "mode": "loop"
        },
        "actor": {
            "res": "/res/actor",
            "name": "Admin",
            "side": "l",
            "msg": "Hello World!"
        },
        "btn": [
            {
                "id": "id",
                "text": "text",
                "fun": "add"
            },
            {
                "id": "id",
                "text": "text",
                "fun": "sub"
            },
            {
                "id": "id",
                "text": "text",
                "fun": "if"
            },
            {
                "id": "id",
                "text": "text",
                "fun": {
                    "dir": "/end.json",
                    "target": "end"
                }
            }
        ],
        "add": {
            "item": 1
        },
        "sub": {
            "item": 1
        },
        "if": {
            "mode": "above",
            "cond": {
                "item": 1
            },
            "then": {
                "dir": "/end.json",
                "target": "end"
            },
            "else": {
                "dir": "/end.json",
                "target": "end"
            }
        },
        "next": {
            "dir": "/end.json",
            "target": "end"
        }
    }
}
```