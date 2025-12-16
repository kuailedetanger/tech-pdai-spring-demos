# Logback 常用 Appender 整理（按使用场景分类）

| Appender 类型      | 全类名（class 属性值）                                       | 核心功能                                   | 适用场景                   | 关键配置项                                                   |
| ------------------ | ------------------------------------------------------------ | ------------------------------------------ | -------------------------- | ------------------------------------------------------------ |
| 控制台输出         | ch.qos.logback.core.ConsoleAppender                          | 日志输出到控制台（终端 / 命令行）          | 开发环境调试、临时排查问题 | encoder（日志格式）、filter（级别过滤）、charset（编码）     |
| 基础文件输出       | ch.qos.logback.core.FileAppender                             | 日志写入单个文件（无自动拆分）             | 临时日志记录（不推荐生产） | file（文件路径）、append（是否追加）、encoder                |
| 滚动文件输出       | ch.qos.logback.core.rolling.RollingFileAppender              | 按时间 / 大小自动拆分日志文件（回滚）      | 生产环境文件持久化（核心） | rollingPolicy（滚动策略）、file（活跃文件）、encoder         |
| 异步输出（包装器） | ch.qos.logback.classic.AsyncAppender                         | 异步输出日志到其他 Appender，不阻塞业务    | 高并发生产环境（性能优化） | appender-ref（指向实际输出 Appender）、queueSize（队列大小）、neverBlock |
| 数据库输出         | ch.qos.logback.core.db.DBAppender                            | 日志写入关系型数据库（MySQL/Oracle）       | 需按条件查询日志的场景     | connectionSource（数据库连接）、sql（插入日志的 SQL）、encoder |
| 邮件告警输出       | [ch.qos.logback.core.net](https://ch.qos.logback.core.net/).SMTPAppender | 触发指定级别日志（如 ERROR）时发送邮件     | 生产环境异常监控、告警     | smtpHost（邮件服务器）、to（收件人）、subject（邮件标题）、threshold（触发级别） |
| Syslog 输出        | [ch.qos.logback.core.net](https://ch.qos.logback.core.net/).SyslogAppender | 输出到 Linux/Unix 的 Syslog 系统日志服务器 | 多服务器日志集中管理       | syslogHost（Syslog 服务器地址）、facility（日志设施）、port（端口） |

# 二、Logback 官网查询 Appender 的方法

Logback 的官方文档是查询 Appender 最权威的来源，步骤如下：

### 1. 进入 Logback 官方文档主页

- 核心地址：

  https://logback.qos.ch/documentation.html

  

  （QOS.ch 是 Logback/SLF4J 的官方维护机构）