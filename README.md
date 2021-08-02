# gitlab-issues-to-excel
这是一个将GitLab某个项目的issues导出excel的工具，运行环境：java8

## 准备工作：
1. 一个GitLab地址
2. 一个支持"api"和"read_user"权限的访问令牌（为了方便，本文称之为：TOKEN）
3. 项目ID

## 使用方法

### 导出Excel
> 直接导出到桌面
```
java -jar gitlab-issues-to-excel.jar GitLab地址/api/v4 TOKEN 项目ID
```

### 导入Excel
> 导入仅支持：截止日期、工时、指派人（目前不支持标题修改）
```
java -jar gitlab-issues-to-excel.jar GitLab地址/api/v4 TOKEN 项目ID excel地址
```

## 标签导出说明
本工具对标签做了两种格式的支持：

* `XXX：yyy` （注：中文分号）： 这种格式的标签导出到excel后，XXX将作为表头、yyy将被导出为具体某一行的值，例如：3个issues分别有`优先级：P0`、`优先级：P1`、`优先级：P2`，那么经过本工具导出的excel中，有一列叫做“优先级”，其对应的3行的值分别为“P0”、“P1”、“P2”
* `xxxx`： 这种格式标签导出的excel中，表头就是`xxxx`，对应的有这个标签的issues行的这一列值为“是”，否则为空白
