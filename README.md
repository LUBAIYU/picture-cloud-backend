## 智能云图平台项目介绍

> 作者：LUBAIYU

### 项目介绍

---

基于 Vue3 + Spring Boot + COS + WebSocket 的智能云图平台。

项目可以分为用户模块、图片模块、空间模块、分类模块、标签模块、评论模块等六大模块。

主要功能简介：

1.  用户能在公共图库上传与检索图片，创建个人私有空间上传专属图片
2. 企业可创建团队空间并邀请成员，实现图片的共享与实时协同编辑
3. 用户可以对图片进行评论，并且对评论进行点赞



### 技术选型

---

#### 后端

- Spring Boot 框架
- MySQL 数据库 + MyBatis-Plus 框架 + MyBatis X
- Jsoup HTML 解析库
- 谷歌 Guava RateLimiter 限流器
- 腾讯云 COS 对象存储
- ✨ Redis 分布式缓存 + Caffeine 本地缓存
- ✨ RabbitMQ 消息队列
- ✨ Sa-Token 权限控制
- ✨ Disruptor 高性能无锁队列
- ✨ ShardingSphere 分库分表 
- ✨ WebSocket 双向通信
- ✨ 阿里云 AI 绘图大模型接入 
- ✨ 百度云 AI 文本审核接入 



#### 前端

- Vue3 框架
- Vite 打包工具
- Ant Design Vue 组件库
- echarts 图表组件库
- Axios 请求库
- Vue-Router 路由组件
- Pinia 全局状态管理
- ✨ OpenAPI 前端代码生成



### 界面示例

---

![image-20250226221321418.png](https://p.sda1.dev/22/e7c428f603ed2b0ee015a249d930e012/image-20250226221321418.png)

![image-20250226221408435.png](https://p.sda1.dev/22/a8fbc8723ef21eaac9b822aa2b60d54f/image-20250226221408435.png)

![image-20250226221437458.png](https://p.sda1.dev/22/955f0b8506bcb57e092173bf3a26a519/image-20250226221437458.png)

![image-20250226221505912.png](https://p.sda1.dev/22/4bf581257b12d804bad83c4b56a53a18/image-20250226221505912.png)

![image-20250226221529785.png](https://p.sda1.dev/22/1ad49fa2e2a8ff1c6110096cd436a2d4/image-20250226221529785.png)

![image-20250226222833245.png](https://p.sda1.dev/22/427d2a4152ac043bb79c2f51703fad80/image-20250226222833245.png)