# MySQL MCP 配置

本项目使用 `@matpb/mysql-mcp-server` 作为 MySQL MCP Server。该服务通过 `npx` 按需启动，不需要把 Node 依赖安装进当前 Spring Boot 项目。

## 项目级配置

配置文件位于项目根目录：

```text
.mcp.json
```

默认服务名：

```text
mysql-travelplanner
```

首次使用前，请把 `.mcp.json` 中的以下字段改成你的本地 MySQL 信息：

```json
{
  "MYSQL_HOST": "127.0.0.1",
  "MYSQL_PORT": "3306",
  "MYSQL_USER": "your_username",
  "MYSQL_PASSWORD": "your_password",
  "MYSQL_DATABASE": "travel_planner"
}
```

## 安全约束

当前选择的是只读 MySQL MCP Server，主要用于：

- 查看表列表
- 查看表结构
- 执行只读 SQL 查询

不要把生产库高权限账号写入 `.mcp.json`。建议单独创建一个只读 MySQL 用户，只授予当前项目数据库的 `SELECT` 权限。

## 验证方式

重启支持项目级 MCP 的客户端后，打开本项目目录，检查是否出现 `mysql-travelplanner` 服务。可先执行以下只读查询验证连接：

```sql
SHOW TABLES;
```

如果使用的是 Codex 全局 MCP 配置，也可以把下面片段迁移到 `C:\Users\79873\.codex\config.toml`：

```toml
[mcp_servers.mysql-travelplanner]
command = "npx"
args = ["-y", "@matpb/mysql-mcp-server"]
env = { MYSQL_HOST = "127.0.0.1", MYSQL_PORT = "3306", MYSQL_USER = "your_username", MYSQL_PASSWORD = "your_password", MYSQL_DATABASE = "travel_planner", MYSQL_CONNECTION_LIMIT = "10", MYSQL_CONNECT_TIMEOUT = "60000", QUERY_TIMEOUT = "30000", MAX_ROWS = "1000" }
```
