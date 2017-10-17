# cabbageWeb
白菜的web版，从WebSocketAPI迁移到HTTP-API，用上了Spring+SpringMVC+MyBatis。


### 命令：
----
!help
发送帮助图片

!stat xxx
查询某人信息，如果没有使用过会自动录入数据库

!stat xxx #0
查询某人信息，不和数据库做对比（如果没有使用过，不录入数据库，用于处理新加群玩家）

!stat xxx #n
和n天前的数据对比（如果那天没有数据，自动取离n天前最近的那天的数据）

!bp xxx
查询某人今天更新的BP

!bp xxx #n
查询某人第n个BP（1<=n<=100）

!bps xxx #n
以文字方式查询某人第n个BP

!setid xxx
将某个id与自己QQ绑定，绑定后可使用：

+ !recent
查询自己在24小时内，最近的Ranked/Qualified图成绩

+ !rs
以文字方式查询!recent

+ !fp bid
打印自己在指定bid的图的#1，如果#1不是你则报错

+ !bpme

+ !bpme #n

+ !bpmes #n

+ !statme 

+ !statme #n

（以上介绍略）

----

管理命令:

!sudo add xxx,xxx:yyy
将xxx,xxx添加到yyy用户组

!sudo del xxx
将xxx的用户组重置为默认（creep）

!sudo check xxx
查询xxx的用户组

!sudo 褪裙 xxx
查询xxx用户组中多少人超过PP上线

!sudo bg xxx:http://123
将给定连接中的图另存为酷Q图片目录/resource/stat/xxx.png，

因此只能用于修改右下角标志(role-xxx)、用户特定bg(uid)、用户组特定bg(xxx)、scorerank背景(scorerank)。

!sudo recent xxx
查询他人的recent。

!sudo afk n:xxx
查询xxx用户组中，n天以上没有登录的玩家(以官网为准)

!sudo smoke @xxx  n
在白菜是管理的群，把被艾特的人禁言n秒

（艾特全体成员则遍历群成员并禁言）

!sudo smokeAll/unsmokeAll
开关全员禁言

!sudo listInvite
列举当前的加群邀请

!sudo handleInvite n
通过Flag为n的邀请

!sudo clearInvite
清空邀请列表

!sudo unbind qq
解绑某个QQ对应的id（找到该QQ对应的uid，并将QQ改为0）

!sudo fp bid
打印给定bid的#1






