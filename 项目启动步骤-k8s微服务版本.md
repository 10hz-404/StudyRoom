# 校园自习室预约系统 K8s 云原生部署启动与运维指南

本指南详细描述了在 Windows 宿主机上基于 **Minikube (Kubernetes)** 部署和运行自习室预约系统（包括微服务群及 SkyWalking、ELK、Prometheus+Grafana 运维监控套件）所需的前置环境要求、脚本功能划分、新机器极简启动流程以及各个服务的访问端口清单。

---

## 💻 一、 启动前环境要求 (Prerequisites)

在全新机器上部署本系统之前，请确保您的物理机满足以下系统资源与开发工具要求：

### 1. 物理机硬件资源推荐
微服务集群加运维监控套件对系统资源的消耗较大，建议物理配置：
* **CPU**：8 核及以上（最低门槛为 4 核）。
* **物理内存**：推荐 $\ge$ 16 GB（因为 Minikube 容器需要分配 6 GB 内存，留给宿主机系统更充沛的空间）。
* **磁盘空间**：至少保留 20 GB 以上的可用空间（用于 Docker 镜像缓存与 Maven 依赖）。

### 2. 软件开发工具依赖
请提前安装好以下工具并配置好其系统环境变量（Path）：

| 工具名称 | 建议版本 | 用途 | 检查命令 |
| :--- | :--- | :--- | :--- |
| **Docker Desktop** | 20.10+ | 容器运行底座与构建引擎 | `docker --version` |
| **Minikube** | v1.30+ | 本地 Kubernetes 单节点集群 | `minikube version` |
| **kubectl** | 与 K8s 版本匹配 | K8s 集群管理与交互命令行工具 | `kubectl version --client` |
| **Maven** | 3.9.x+ | Java 后端多模块依赖编译与打包 | `mvn -version` |
| **JDK** | Java 21 | 微服务后端编译和运行的 Java 开发包 | `java -version` |
| **Node.js** | 18.x+ 及 npm | 前端 Vue3 项目的编译与依赖管理 | `node -v` |

---

## 📂 二、 脚本文件功能说明 (Script Descriptions)

项目 `第4阶段/code` 目录下提供了 5 个不同的引导脚本以适应不同的开发和运维场景，其具体作用和使用场景如下表所示：

| 脚本文件名称 | 执行平台 | 核心作用与使用场景 |
| :--- | :--- | :--- |
| 🚀 **`一键部署并启动代理.cmd`** | Windows CMD | **新机器/全新部署推荐**。一键自动检测并以 6G 内存+4 CPU 启动 Minikube，全自动执行前后端编译打包，构建 `v999` 本地镜像，部署全套 K8s 资源（含 MySQL 乱码修复与 512M limits 扩容包），并最终一键拉起包括微服务和全套监控在内的 7 个端口代理。 |
| ⚡ **`一键拉起所有端口代理.cmd`** | Windows CMD | **物理直连调试神器**。不重新编译，也不发布镜像。仅在物理机上直接一键拉起所有 7 个服务的物理端口代理通道，快速通过 `localhost` 在浏览器中访问网页和所有的运维监控后台。已解决 Windows 平台下的空格路径及换行闪退 Bug。 |
| 🔄 **`start-k8s-system.cmd`** | Windows CMD | **日常二次启动推荐**。跳过前后端的 Maven/npm 编译打包，直接在 Minikube 中检测并拉起 K8s 部署环境，启动微服务和监控容器并监控 Pod 的就绪状态。 |
| 💻 **`一键启动系统.cmd`** | Windows CMD | **本地裸机调试专用**。调用 docker-compose 启动物理机上的中间件，并直接以前台 Java 进程和 npm 运行服务。**它不依赖 Kubernetes/Minikube**，仅用于本地开发时快捷测试。 |

---

## 🚀 三、 全新机器极简运行指南 (Setup on a New Machine)

如果是一台**全新的机器**，请严格按照以下 3 个步骤操作即可一键完整拉起整个系统：

### 步骤 1：开启 Docker Desktop
在双击启动部署前，请确保您物理机上的 **Docker Desktop 已成功开启并处于正常运行状态**。

### 步骤 2：一键部署并启动代理
前往项目文件夹 `D:\JYU\10、软件架构技术\fina_exam\第4阶段\code`
* 直接双击运行 **`一键部署并启动代理.cmd`**。
* **说明**：该脚本会自动检测 Minikube 状态。如果发现没有启动，它会自动以 **6GB 内存与 4 核 CPU** 的配置拉起 Minikube 实例。为避免虚拟机内部由于网络代理受限而导致拉取基础镜像失败，脚本内置了 **【宿主机本地预拉取基础镜像并 load 载入 Minikube 缓存】** 的自愈方案。随后自动进行前后端打包、本地 `v999` 镜像编译并对微服务执行滚动更新（rollout restart）发布，并在就绪后拉起 7 个物理端口代理。

### 步骤 3：后续日常开发启动
日常开发时（只要不重新清理集群或重启电脑），下次启动就不需要重走第二步的编译流程了，只需：
1. 双击运行 **`start-k8s-system.cmd`**（快速拉起 K8s 内部所有的微服务与监控组件）。
2. 双击运行 **`一键拉起所有端口代理.cmd`**（快速在物理机打通 7 个 localhost 的浏览器访问端口）。

---

## 🔗 四、 系统服务与运维监控端口清单 (Port Bindings)

当代理建立成功后，您可以在物理宿主机上直接使用浏览器通过以下地址访问对应的微服务与运维监控中心：

### 1. 业务系统访问入口
| 服务名称 | 物理机访问地址 | 默认用户名/密码 | 说明 |
| :--- | :--- | :--- | :--- |
| **系统前端 Web 界面** | 🔗 [http://localhost:30088](http://localhost:30088) | - | 网页端单页面应用程序 |
| **API 统一路由网关** | 🔗 [http://localhost:30080](http://localhost:30080) | - | 后端微服务群统一入口，负责路由及鉴权分发 |
| **Nacos 注册配置中心** | 🔗 [http://localhost:30848/nacos](http://localhost:30848/nacos) | `nacos` / `nacos` | 管理服务动态注册和配置推送 |

### 2. 运维监控组件访问入口
| 监控组件名称 | 物理机访问地址 | 默认用户名/密码 | 说明与监控用途 |
| :--- | :--- | :--- | :--- |
| **SkyWalking UI** | 🔗 [http://localhost:31800](http://localhost:31800) | - | **分布式链路追踪看板**。监控微服务间的调用耗时、拓扑依赖关系。已完成 Agent 自动拷贝织入。 |
| **Kibana (ELK)** | 🔗 [http://localhost:35601](http://localhost:35601) | - | **分布式日志检索控制台**。集中展示日志检索服务。*(注：为节约本地单机的物理内存，本轻量级部署方案默认未运行 Filebeat 日志抓取容器，微服务容器日志在 console 正常输出，Kibana 仅作组件启动就绪展示)* |
| **Prometheus 控制台** | 🔗 [http://localhost:30090](http://localhost:30090) | - | **度量指标收集控制台**。监控微服务运行的 CPU/内存占用，管理告警规则状态。*(已在微服务父 pom.xml 中引入 micrometer-prometheus 依赖并在 application.yml 中配置 actuator 暴露，打通指标拉取通路)* |
| **Grafana 监控看板** | 🔗 [http://localhost:32000](http://localhost:32000) | `admin` / `admin` | **时序数据指标可视化面板**。以精美的图表图形实时展示系统整体健康度。 |

---

## 🚦 五、 验证与日常排查常用命令

* **查看所有 Pod 运行状态**：
  ```bash
  kubectl get pods -n study-room
  ```
  *(当所有微服务的 `READY` 列显示为 `1/1` 且 STATUS 是 `Running` 时，代表系统完全可用。已将微服务 memory 限制安全上调至 512Mi 规避了 OOMKilled 重启)*

* **验证告警规则配置状态**：
  在 K8s 内部直接查询 Prometheus 规则：
  ```bash
  kubectl exec <prometheus-pod-name> -n study-room -- wget -qO- http://127.0.0.1:9090/api/v1/rules
  ```
  *(证实 `ServiceDown`、`Http5xxRateHigh` 和 `HighMemoryUsage` 三个内置告警规则是否已成功激活并在评估运行中)*
