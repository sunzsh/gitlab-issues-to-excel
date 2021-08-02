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
java -jar gitlab-issues-to-excel.jar GitLab地址 TOKEN 项目ID
```

### 导入Excel
> 导入仅支持：标记、截止日期、工时、指派人（目前不支持标题修改）
```
java -jar gitlab-issues-to-excel.jar GitLab地址 TOKEN 项目ID excel地址
```
