# RAG智能问答后端系统
> SpringBoot + Elasticsearch向量检索 + 智谱GLM大模型 + SSE流式输出

## ✨项目简介
独立开发知识库RAG智能助手，实现文档切片、向量入库、相似度检索，结合大模型生成答案，采用SSE流式返回回答，Redis维护会话上下文。

## 🛠技术栈
- 后端：SpringBoot
- 数据库：MySQL、Redis
- 检索引擎：Elasticsearch向量检索
- 大模型：智谱GLM API，Prompt工程优化
- 通信：SSE流式输出

## 📌核心功能
1. 文档文本分割、向量化，存入ES向量库
2. 用户提问，向量检索匹配相关知识库片段
3. 组装Prompt调用GLM大模型，SSE流式返回结果
4. Redis缓存会话，保存多轮对话上下文

## 🚀运行说明
1. 修改application.yml配置，填写MySQL、Redis、智谱GLM API Key
2. 启动Elasticsearch服务
3. 启动SpringBoot项目，访问前端页面测试问答功能

> 项目为个人实战项目，密钥信息已脱敏。
