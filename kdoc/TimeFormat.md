# TimeFormat 时间格式常量工具类

## 概述
提供标准化的日期时间格式模式常量，包含ISO、RFC标准及常用自定义格式。所有模式符遵循`SimpleDateFormat`规范。

## 功能特性
- 📅 国际标准格式（ISO 8601/RFC）
- 🌍 多语言区域支持
- ⏱️ 时间/日期组合格式
- 📁 文件/日志专用格式
- 🔧 便捷的格式化工具方法

## 常量列表

### 国际标准格式
| 常量                         | 格式示例                          | 说明                   |
|----------------------------|-------------------------------|----------------------|
| `DATE_TIME_ISO_8601`       | 2023-10-12T15:30:45.123+08:00 | ISO 8601完整格式（带毫秒和时区） |
| `DATE_TIME_ISO_8601_BASIC` | 2023-10-12T15:30:45+0800      | ISO基本格式（不带毫秒）        |
| `DATE_ONLY_ISO`            | 2023-10-12                    | 标准日期格式               |
| `TIME_ONLY_ISO`            | 15:30:45                      | 24小时制时间              |

### RFC标准格式
| 常量                   | 格式示例                            | 说明         |
|----------------------|---------------------------------|------------|
| `DATE_TIME_RFC_822`  | 12 Oct 2023 15:30:45 +0800      | 邮件标准格式     |
| `DATE_TIME_RFC_1123` | Thu, 12 Oct 2023 15:30:45 +0800 | HTTP协议日期格式 |

### 自定义日期格式
| 常量                | 格式示例             | 说明     |
|-------------------|------------------|--------|
| `DATE_FULL_ZH`    | 2023年10月12日      | 完整中文日期 |
| `DATE_DASHES`     | 2023-10-12       | 短横线分隔  |
| `DATE_SLASHES`    | 2023/10/12       | 斜线分隔   |
| `DATE_MONTH_NAME` | October 12, 2023 | 月份全称格式 |

### 时间相关格式
| 常量                   | 格式示例         | 说明          |
|----------------------|--------------|-------------|
| `TIME_24H_FULL`      | 15:30:45     | 24小时制完整时间   |
| `TIME_12H_WITH_AMPM` | 3:30 PM      | 12小时制带AM/PM |
| `TIME_WITH_MILLIS`   | 15:30:45.123 | 带毫秒时间       |

### 专用格式
| 常量                    | 格式示例                      | 应用场景 |
|-----------------------|---------------------------|------|
| `FILE_SAFE_TIMESTAMP` | 20231012_153045           | 文件命名 |
| `LOG_TIMESTAMP`       | [2023-10-12 15:30:45.123] | 日志记录 |
| `CHAT_MESSAGE_STYLE`  | Oct 12 15:30              | 聊天消息 |

## 使用示例

```kotlin
// 获取ISO格式的当前时间
val isoDate = TimeFormat.getFormatter(TimeFormat.DATE_TIME_ISO_8601).format(Date())

// 中文日期格式化
val chineseDate = TimeFormat.getFormatter(TimeFormat.DATE_FULL_ZH).format(Date())

// 日志时间戳
val logTime = TimeFormat.getFormatter(TimeFormat.LOG_TIMESTAMP).format(Date())