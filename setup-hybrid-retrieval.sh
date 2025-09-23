#!/bin/bash

# 混合检索设置脚本
# 用于启用混合检索功能

echo "🚀 设置混合检索功能..."

# 1. 创建数据库索引
echo "📊 创建数据库索引..."
psql -h localhost -U ragone_user -d ragone -f data/hybrid_retrieval_indexes.sql

if [ $? -eq 0 ]; then
    echo "✅ 数据库索引创建成功"
else
    echo "❌ 数据库索引创建失败"
    exit 1
fi

# 2. 更新配置文件启用混合检索
echo "⚙️ 更新配置文件..."

# 备份原配置文件
cp src/main/resources/application.yml src/main/resources/application.yml.backup

# 启用混合检索
sed -i 's/enabled: false/enabled: true/g' src/main/resources/application.yml

echo "✅ 配置文件更新完成"

# 3. 重启应用
echo "🔄 重启应用..."
./stop-dev.sh
sleep 2
./start-dev.sh

echo "🎉 混合检索功能设置完成！"
echo ""
echo "📋 配置说明："
echo "  - 混合检索: 已启用"
echo "  - 向量权重: 0.7"
echo "  - 关键词权重: 0.3"
echo "  - 最大结果数: 10"
echo "  - 重排序: 已启用"
echo ""
echo "🔧 如需调整配置，请编辑 src/main/resources/application.yml"
echo "📖 详细配置说明请参考 README.md"
