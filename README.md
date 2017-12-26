## cabbageWeb
基于Java Web的，osu游戏数据查询机器人。

## 运行环境
OS: Ubuntu 17.04（理论上支持Docker的即可，甚至在Windows SubSystem Linux上测试通过）

Windows Server 2008/Win Server 2012（由于酷Q原因不支持Win Server 2003）

MySQL 5.7（没有研究过MySQL的版本差异）

JDK1.8（必须，使用了Lambda表达式/Try With Resource）

Tomcat 7.0.75（理论上支持JDK8以上的Web容器都可以）

## 开发环境
IDEA 2017.2.5

JDK1.8

MySQL 5.7

Tomcat 7.0.75

Maven （IDEA内置）

## 特性
从osu!API和官网获取数据，并绘制图片以QQ消息的形式发送。

对接了osu/酷Q提供的HTTP API，用到了正则表达式，以及Spring的AOP实现权限控制、性能统计、异常通知，mybatis则用到了结果集映射、动态SQL等基本技能。

## 感谢
感谢[Imouto koko](https://osu.ppy.sh/u/7679162)提供的消息队列实现思路、以及名片/BP/#1等大量图片元素的设计。
感谢[Pata-Mon](https://osu.ppy.sh/u/6149313)的赞助（
感谢[osu!mp乐园5号群](https://jq.qq.com/?_wv=1027&k=594UuXH)帮我找出bug/提出req的各位。
感谢[Koohii](https://github.com/Francesco149/koohii) 项目，作者的Java实现为我省去了很多麻烦。
