# 路线 A：从 4.4 交接到 4.5

核对日期：2026-10-26。基线来自方子豪的 WSL 项目 `/home/city/blockchain-course/spring-boot-web3j`。

## 已完成到哪里

| 讲义章节 | 当前成果 |
| --- | --- |
| 4.1 | 后端项目目录与调用路径已确认，项目使用 Java 21、Spring Boot 和 Web3j。 |
| 4.2 | 新建并为后端仲裁测试账户领取 POT；配置 RPC、gas 参数和私钥环境变量占位符。 |
| 4.3 | 复制实验三 `Escrow.sol`、`FZHToken.sol`，生成 `Escrow.java`、`FZHToken.java`、`IERC20.java` 等封装。讲义的 `ZSToken` 在本组替换为 `FZHToken`。 |
| 4.4 | `SimpleStorage` 部署成功，`set(100)` 交易成功，`get()` 返回 `100`。 |
| 4.5 | 尚未编写 `EscrowService.java`、`EscrowController.java`，以下接口为接下来要实现的约定。 |
| 4.6 | 尚未通过本后端部署 Escrow，尚未开始两笔订单的完整业务验收。 |

这里记录代码与运行核对结果；报告截图仍以实际保存的截图为准。截图 4A-3 要包含讲义三条 curl 的响应以及 set 交易详情。

## 小组分工与文件边界

| 成员 | 讲义建议分工 | 接下来负责的内容 |
| --- | --- | --- |
| 方子豪 | 合约封装与 Service | 维护 Solidity 和生成封装；实现 `EscrowService` 的部署、加载、单笔查询、仲裁及辅助方法；集成事件列表方法。 |
| 黄政军 | Controller 与事件查询 | 实现 `EscrowController`；实现 `EscrowService.listOrders()` 的事件扫描与解析；核对六类 HTTP 接口的参数和响应。 |
| 黄培生 | Remix 买卖家操作、验收记录与报告 | 等后端接口齐备后组织 4.6 的买家/卖家操作，整理实际交易、截图、README 与报告。 |

`listOrders()` 虽然位于 Service 文件，仍属于黄政军的事件查询分工。两人修改同一文件时，各自只改负责的方法和必需的 import，再通过分支合并，避免用整份文件覆盖另一人的实现。

## 黄政军如何开始

从自己的 Ubuntu 终端执行；在尚未存在同名目录的父目录中 clone：

```bash
git clone https://github.com/fzh4379/spring-boot-web3j.git
cd spring-boot-web3j
git switch -c feature/hzj-controller-events
```

clone 取得后端基线；`git switch -c` 在本机新建工作分支。先本地提交，再把分支推到自己的 fork 并向本仓库提交 PR，或在获得本仓库写权限后直接推送分支。当前交接不要求共享账号或私钥。

方子豪后续使用 `feature/fzh-escrow-service` 分支。两人先约定下面的方法签名，合并后统一编译和验收。`master` 保留可运行基线，完成一个可检查的改动再合并。

## 本机编译与启动

需要 Java 21。按讲义生成封装并查看文件列表：

```bash
bash gradlew generateContractWrappers
ls -R src/main/java/com/wetech/demo/web3j/contracts/
```

仓库已包含本次生成的封装；修改 Solidity 后仍需重新生成。Gradle Wrapper 保留当前电脑使用的 Gradle 8.13 腾讯镜像地址。

`src/main/resources/application.properties` 保留以下配置：

```properties
web3j.client-address=https://rpc-testnet.potos.hk
web3j.private-key=${WEB3J_PRIVATE_KEY}
web3j.gas-price=1000000
web3j.gas-limit=6721975
```

每位成员在本机使用自己的专用 POTOS 测试账户。以下是讲义命令的占位写法，在本机替换占位文字；真实私钥不能写进源码、提交或聊天：

```bash
export WEB3J_PRIVATE_KEY='0x替换为你本机专用测试账户的64位十六进制私钥'
bash gradlew bootRun
```

环境变量只属于当前终端及其子进程；后端应从设置变量的同一终端启动。`bootRun` 会持续运行，出现 `Started Application ...` 后即可在另一个终端发 curl 请求，不需要等到 Gradle 进度为 100%。

### 方子豪电脑上已测通的 WSL 代理补充

这是本机网络的兼容操作，属于讲义之外的补充。实测 Windows 代理为 `127.0.0.1:7897`；WSL 为 NAT 网络，直连 RPC 超时，WSL 中访问 `127.0.0.1:7897` 被拒绝，经 Windows 宿主机 `172.30.32.1:7897` 可读取到链 ID `60600`。已使用项目实际 Web3j 依赖验证该路径。

方子豪本次会话的启动方式：

```bash
export JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=172.30.32.1 -Dhttps.proxyPort=7897"
bash gradlew bootRun --console=plain
```

`JAVA_TOOL_OPTIONS` 给 Java 指定 HTTPS 代理；`--console=plain` 关闭动态进度条。组员电脑不一定需要代理，不能照搬这个宿主机 IP 或端口；如确需代理，先按自己的代理软件设置及 WSL 网关核对。WSL 重启后宿主机地址也可能变化。

## 4.5 接口与 Service 方法约定

| HTTP 接口 | 参数 | 必须返回 |
| --- | --- | --- |
| `POST /api/escrow/deploy` | 无 | `contractAddress`、`transactionHash`、`deployBlock` |
| `POST /api/escrow/load` | `address`、`fromBlock` | `contractAddress` |
| `GET /api/escrow/address` | 无 | `contractAddress`、`arbiter` |
| `GET /api/escrow/orders/{id}` | 路径中的订单 id | `buyer`、`seller`、`token`、`amount`、`deadline`、文字形式 `status`、`description` |
| `GET /api/escrow/orders` | 无 | 事件列表：`id`、`buyer`、`seller`、`amount`、`blockNumber`、`transactionHash` |
| `POST /api/escrow/orders/{id}/resolve` | 路径中的 id、查询参数 `sellerWins` | `transactionHash`、`blockNumber`、`status` |

讲义 Service 骨架的方法签名：

```java
CompletableFuture<Escrow> deploy();
void load(String address, BigInteger fromBlock);
CompletableFuture<Map<String, String>> getOrder(BigInteger id);
List<Map<String, String>> listOrders() throws Exception;
CompletableFuture<TransactionReceipt> resolve(BigInteger id, boolean sellerWins);
```

此外使用 Lombok getter 提供 `getContractAddress()`、`getDeployBlock()`。为满足 `/address` 返回仲裁人，建议补充约定 `CompletableFuture<String> getArbiter()`，由方子豪实现；这条辅助方法是本组接口约定，讲义骨架未直接给出。以上均为接下来要实现的方法，目前仓库不声称已有这些业务接口。

实现时保留讲义骨架的注释与顺序，`Escrow` 的生成类路径为 `com.wetech.demo.web3j.contracts.escrow.Escrow`。状态顺序为 `Created`、`Paid`、`Completed`、`Refunded`、`Disputed`，单笔查询需要返回对应文字。

### 事件列表的要求

黄政军按讲义实现 `listOrders()`：从实际 `deployBlock` 扫描到最新区块，使用 `Escrow.ORDERCREATED_EVENT` 编码 topic，并用 `Escrow.getOrderCreatedEventFromLog(log)` 解析。按讲义说明，POTOS 的 `eth_getLogs` 每次只扫描从 `fromBlock` 开始的 10 个区块，因此要按 10 个区块分批查询，最后一段取实际最新高度。

`load` 时必须传原 Escrow 的真实部署高度。不要把 SimpleStorage 的区块或实验三旧 Escrow 的部署高度混入即将在 4.6 新部署的 Escrow。

## 4.4 已核对的运行数据

这些属于示例 SimpleStorage，用于证明后端已跑通；后续 4.6 需要另部署 Escrow 并记录新地址。

| 项目 | 实际结果 |
| --- | --- |
| 网络 | POTOS Testnet，chain ID `60600` |
| 后端测试账户（公开地址） | `0xc4a9E32003EB58EdBA150Cf3bB86fE559328B68f` |
| SimpleStorage 地址 | `0x6e5c1681DeFc13C1424094F0D786692E25cb248f` |
| set 交易哈希 | `0xd9f1c6e43971a2688e6de393f6caad97313423b710802ef0d69effb08d79415d` |
| set 所在区块 | `2689655` |
| set 调用 | `set(100)`，方法选择器 `0x60fe47b1` |
| 交易状态 / gasUsed | 成功，`0x1` / `27264` |
| get 接口结果 | `{"contractAddress":"0x6e5c1681DeFc13C1424094F0D786692E25cb248f","value":"100"}` |

[set 交易详情](https://scan-testnet.potos.hk/tx/0xd9f1c6e43971a2688e6de393f6caad97313423b710802ef0d69effb08d79415d)。上述结果已在核对当日通过 RPC 回执、交易 input、区块浏览器 API 及本机 HTTP 接口交叉核对。

重新启动后端时，示例 Service 内存中保存的合约实例会丢失。要复用上述示例合约，可以执行原示例已有的加载接口，再只读查询：

```bash
curl -X POST "http://localhost:8080/api/storage/load?address=0x6e5c1681DeFc13C1424094F0D786692E25cb248f"
curl -X GET http://localhost:8080/api/storage/value/get
```

本基线尚没有后端部署的 Escrow 地址。4.6 按讲义使用组长实验三的代币 `0x0Fe014B2e51b3F5bC209480cEbAe22E9AA83f303`；不要混用实验二旧代币。创建订单前仍需核对当前 `balanceOf`，不能把历史余额当作当前余额。

## 本次交接提交前的检查

2026-10-26 已执行 `bash gradlew compileJava --offline --console=plain`，结果为 `BUILD SUCCESSFUL`。Solidity 编译与封装生成任务为 `UP-TO-DATE`，修改后的 Java 配置重新编译通过。

同时检查了待提交的 20 个文本源文件：私钥配置为环境变量占位符，Java 配置不再回退到硬编码测试私钥，本机凭据文件已被忽略；公开的 set 交易哈希单独识别为交易数据。配置和交接文档的空白检查通过。生成器输出的 Java 文件有部分尾部空白，`FZHToken.sol` 原文件使用 CRLF 换行，本次均保留原样，未因此重写合约或生成代码。这些检查不代表尚未实现的 4.5、4.6 已通过验收。

## 提交前仍需完成

- 方子豪完成 Service，黄政军完成 Controller 和事件查询，合并后编译并核对六类接口。
- 保留完整代码截图 4A-4，并依据实际实现回答解释 4A-3。
- 依讲义 4.6 验收业务流、保存真实回执及截图，更新小组交易记录和报告。
- README / 报告如使用了 AI 辅助，按课程要求如实披露。本交接文档与配置整理包含 AI 辅助，需由小组复核。
