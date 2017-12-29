## 白菜。
基于Java Web的，osu!游戏数据查询机器人。

## 运行环境
OS: 
目前使用：Ubuntu 17.04（理论上支持Docker的即可，甚至在Windows SubSystem Linux上测试通过）

曾用：Win Server 2008/2012（由于酷Q原因不支持Win Server 2003）

MySQL 5.7（没有研究过MySQL的版本差异）

JRE1.8（必须，使用了Lambda表达式/Try With Resource，不兼容1.8以下的Java版本。）

Tomcat 7.0.75（理论上支持Java8以上的Web容器都可以）

酷Q Pro最新版，以及HTTP API插件

RAM：推荐为3G及以上（会缓存加入群的最近50条消息，用于防撤回功能；对于开启复读禁言的群会缓存100条，会占用1+GB 的Heap Space；会缓存数据库内所有图片素材，约200+MB 的Heap Space。）


## 特性
从osu!API和官网获取数据，并绘制图片以QQ消息的形式发送。

对接了osu/酷Q提供的HTTP API，用到了正则表达式，以及Spring的AOP实现权限控制、性能统计、异常通知，mybatis则用到了结果集映射、动态SQL等基本技能。

## 鸣谢（不分先后）
感谢[Imouto koko](https://osu.ppy.sh/u/7679162)提供的消息队列实现思路、参与名片/BP/#1等大量图片元素的设计、数不清的功能需求。

感谢[Pata-Mon](https://osu.ppy.sh/u/6149313)的赞助，以及在发布前夕帮我指出了设计思路中的致命错误。

感谢[osu!mp乐园5号群](https://jq.qq.com/?_wv=1027&k=594UuXH)帮我找出bug的各位，也感谢mp5管理层提出的功能需求，没有你们提的需求就没有如今完善的玩家管理。

感谢[Koohii](https://github.com/Francesco149/koohii) 项目，尽管作者不喜欢Java，但是依然用Java实现了oppai的全部功能，帮我略去调用外部命令的麻烦。

感谢[coolq-http-api](https://github.com/richardchien/coolq-http-api) 项目，作者提供了完善的文档、稳定的更新，实现了酷Q和其他语言的打通，如果没有本项目，很多群管理功能都无法实现。
