# FFCraft Plugin

![ICON](./icon.png)

> FFCraft 的 Bukkit 插件端，为服务器提供媒体播放器管理功能。需配合客户端模组使用，让玩家在 Minecraft 世界中播放任意媒体内容。

## ✨ 功能特性

- **服务端统一管理** — 通过指令或配置文件创建、删除、配置播放器
- **全格式媒体支持** — 基于 FFmpeg，支持几乎所有音频与视频格式
- **任意形状屏幕** — 支持最多 64 个顶点的多边形显示区域
- **光影兼容** — 客户端屏幕可正确反射和响应光影包的光照效果
- **权限控制** — 完善的权限节点，区分管理员、编辑者与普通玩家
- **自动同步** — 玩家进入服务器时自动同步所有现有播放器

## 🛠 技术栈与前置

- **服务端**：Bukkit系列(包括Spigot, Paper)（1.18.2+）
- **语言**：Java
- **客户端要求**：玩家需安装 **FFCraft 模组**

## 📥 安装

### 服务器管理员

1. 确保服务器已安装 Bukkit系列 任意核心
2. 下载插件（从 [Releases](https://github.com/Sashwind888/FFCraftPlg/releases) 获取）
3. 放入服务器的 `plugins` 文件夹
4. Enjoy it !

### 玩家

玩家只需在客户端安装 **FFCraft 模组** 即可加入服务器并观看媒体内容。

## 📋 命令系统

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ffcraft create <name> [public\|private]` | `ffcraft.admin` | 创建播放器 |
| `/ffcraft list` | 所有人 | 列出可查看的播放器（私人播放器仅授权用户可见） |
| `/ffcraft info <uuid>` | 所有人 | 查看播放器详情（私人播放器需授权） |
| `/ffcraft rename <uuid> <newName>` | 播放器创建者 / admin | 重命名播放器 |
| `/ffcraft delete <uuid>` | `ffcraft.admin` | 删除播放器 |
| `/ffcraft setpublic <uuid> <true\|false>` | `ffcraft.admin` | 切换公开/私人模式 |
| `/ffcraft grant <playerId> <playerName>` | 播放器创建者 / admin | 授予操作权限（播放/暂停/切歌/音量） |
| `/ffcraft revoke <playerId> <playerName>` | 播放器创建者 / admin | 撤销操作权限 |
| `/ffcraft reload` | `ffcraft.admin` | 重新同步所有客户端 |


| 来源 | 公开播放器 | 私人播放器 |
|-----|----------|----------|
|/ffcraft list|所有人可见|仅 creator/admin/controlUsers 可见|
|/ffcraft info|所有人可见|未授权 → "未找到"|
|客户端同步|全量推送|按 viewer 过滤后推送|


- 权限节点可通过 LuckPerms 等权限插件赋予玩家
- 所有 `uuid` 参数均指播放器的唯一标识

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 **AGPLv3** 许可证。修改后的项目也必须以相同许可证开源。详情请查看 [LICENSE](LICENSE) 文件。

---

*希望这个项目可以帮到你喵~ --sashwind*  
*模组：[FFCraft](https://github.com/Sashwind888/FFCraft)*
