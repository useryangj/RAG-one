#!/bin/bash

# RAG-one 数据库迁移执行脚本
# 使用方法: ./run_migration.sh

echo "=== RAG-one 数据库迁移脚本 ==="
echo "正在连接到PostgreSQL数据库..."

# 设置数据库连接参数
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="ragone"
DB_USER="ragone_user"
DB_PASSWORD="ragone_password"

# 设置环境变量以避免密码提示
export PGPASSWORD=$DB_PASSWORD

echo "数据库连接信息:"
echo "- 主机: $DB_HOST:$DB_PORT"
echo "- 数据库: $DB_NAME"
echo "- 用户: $DB_USER"
echo ""

# 检查PostgreSQL连接
echo "检查数据库连接..."
if ! psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT version();" > /dev/null 2>&1; then
    echo "❌ 无法连接到数据库，请检查："
    echo "1. PostgreSQL服务是否运行"
    echo "2. 数据库连接参数是否正确"
    echo "3. 用户权限是否足够"
    exit 1
fi

echo "✅ 数据库连接成功"
echo ""

# 执行迁移脚本
echo "开始执行数据库迁移..."
echo "----------------------------------------"

if psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f database_migration.sql; then
    echo "----------------------------------------"
    echo "✅ 数据库迁移执行成功！"
    echo ""
    echo "接下来的步骤："
    echo "1. 重启Spring Boot应用"
    echo "2. 测试RAG问答功能"
    echo "3. 观察查询性能提升"
else
    echo "----------------------------------------"
    echo "❌ 数据库迁移执行失败！"
    echo ""
    echo "请检查错误信息并根据需要进行修复"
    exit 1
fi

# 清除密码环境变量
unset PGPASSWORD

echo ""
echo "=== 迁移脚本执行完成 ==="
