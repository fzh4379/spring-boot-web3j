# Spring Boot + Web3J Example

This is a simple example of a Spring Boot application that integrates with Web3J to interact with Ethereum smart contracts.

## 课程大作业路线 A：4.4 交接基线

本仓库基于 [kyonRay/spring-boot-web3j](https://github.com/kyonRay/spring-boot-web3j)，保存方子豪已完成的 4.1–4.4 后端准备工作，供第 4.5 节协作开发使用。

- 已加入实验三的 `Escrow.sol`、`FZHToken.sol` 及生成的 Java 封装，保留合约原有注释。
- `application.properties` 使用 `${WEB3J_PRIVATE_KEY}`，运行时由各自本机的环境变量提供私钥。
- 已在 POTOS Testnet 跑通 `SimpleStorage` 的部署、`set(100)` 和 `get()`，读取结果为 `100`。
- **4.5 的 `EscrowService`、`EscrowController` 尚未实现；4.6 业务验收尚未开始。**

启动方法、已核对的数据、分工与接口约定见 [4.5 交接说明](docs/HANDOFF_4.5.md)。

## Overview

This application demonstrates how to:
- Deploy a smart contract to an Ethereum blockchain
- Load an existing smart contract
- Interact with a smart contract (get and set values)
- Expose smart contract functionality via REST API

## Prerequisites

- Java 21 or higher
- The bundled Gradle Wrapper (`gradlew`); a separate Gradle installation is not required
- An Ethereum client (like Ganache, Geth, or a testnet connection)

## Smart Contract

The application uses a simple `SimpleStorage` contract that allows storing and retrieving a single value:

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract SimpleStorage {
    uint256 private storedData;
    
    event DataChanged(uint256 newValue);
    
    function set(uint256 x) public {
        storedData = x;
        emit DataChanged(x);
    }
    
    function get() public view returns (uint256) {
        return storedData;
    }
}
```

## Building and Running

1. Clone the repository
2. Configure your Ethereum client in `src/main/resources/application.properties`
   The committed private-key property is an environment-variable placeholder. Before starting the server, set `WEB3J_PRIVATE_KEY` locally using your own funded POTOS test account; see the handoff guide. Never replace the placeholder with a real key in a committed file.
3. Build the application:
   ```
   ./gradlew build
   ```
4. Run the application:
   ```
   ./gradlew bootRun
   ```

## API Endpoints

### Deploy a new contract
```
POST /api/storage/deploy
```

### Load an existing contract
```
POST /api/storage/load?address={contractAddress}
```

### Get the stored value
```
GET /api/storage/value/get
```

### Set a new value
```
POST /api/storage/value/set?value={newValue}
```

### Get the contract address
```
GET /api/storage/address
```

## Example Usage

1. Deploy a new contract:
   ```
   curl -X POST http://localhost:8080/api/storage/deploy
   ```

2. Set a value:
   ```
   curl -X POST "http://localhost:8080/api/storage/value/set?value=100"
   ```

3. Get the value:
   ```
   curl -X GET http://localhost:8080/api/storage/value/get
   ```

## Configuration

The application can be configured in `src/main/resources/application.properties`:

```properties
# Server configuration
server.port=8080

# Web3j configuration
web3j.client-address=https://rpc-testnet.potos.hk
web3j.private-key=${WEB3J_PRIVATE_KEY}
web3j.gas-price=1000000
web3j.gas-limit=6721975
```

## Notes

- This is a simple example and not intended for production use
- Supply the private key through the local `WEB3J_PRIVATE_KEY` environment variable; the Java configuration has no hardcoded fallback key
- The application assumes you have an Ethereum client running at the configured address
