**需要 Velocity 插件 [SwapServerVelocity](https://github.com/ESEAbsolute/SwapServerVelocityPlugin) 支持。**


该插件支持 Folia，并且主要为解决以 Folia 为服务端核心的群组服刷物品问题而生。

Folia 会区块卡死，当区块卡死时使用代理端插件可以正常生效，而服务端的指令并不可以。这就导致背包同步失败等一系列问题。

本插件把换服和排队的操作从代理端放到了服务端，避免了在卡死的区块内换服。

加入了两个命令
- `/server`
- `/queue`


权限节点
- `swapserver.server`：`/server` 换服命令使用权限
- `swapserver.queue`：`/queue` 排队命令使用权限
- `swapserver.server.xxx`：切换到 `xxx` 服务器的权限

所以，如果要禁止传送到玩家数据互通的其他服务器内，请阻止一切可以不通过异常区块所在服切换过去的通道。

例如说，现在有四个服务器，`server1`，`server2`，`server3` 和 `lobby`。其中 `lobby` 为登陆服，进入服务器时默认将玩家送入 `lobby`。`server1` 和 `server2` 背包互通，剩下的两两不互通。

此时需要在 `server3` 和 `lobby` 把用户组 `default` 的 `swapserver.server.server2` 权限禁用掉，否则玩家可能会退出游戏，进入 `lobby`，之后直接进入 `s2` 达成刷物品目的。


以及，别忘了把 velocity.command.server 的权限禁用掉。