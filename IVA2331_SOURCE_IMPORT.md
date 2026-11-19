# IVA 2.3.31 源码与资源导入说明

本目录内容来自已验证安装包构建源：

```text
F:\RK3588-6NPU-TEST\rk3588-rk182x-iva-offline-3.0.9-generic-h264-rkmpp-container\rk3588-rk182x-iva-offline-3.0.9-arm64
```

目标是提供可上库的源码、网页资源、运行配置和安装相关脚本。

## 目录对应关系

| 目标目录 | 来源目录 | 内容 |
|---|---|---|
| `source/java` | `source/java` | Java/Jeecg 后端源码 |
| `source/ui` | `source/ui` | Vue 前端源码、构建配置和静态资源 |
| `source/python` | `source/python` | RK3588 Python 源码、测试和模型容器源码 |
| `source/rk182x-python` | `source/rk182x-python` | RK182X Python 源码和测试 |
| `source/shared` | `source/shared` | 前后端共享配置模块 |
| `runtime/kyscene/web` | `payload/kyscene/web` | 当前安装包使用的前端静态文件 |
| `runtime/kyscene/config` | `payload/kyscene/config` | Java/Kyscene 运行配置模板 |
| `runtime/kyscene/legacy-web` | `payload/kyscene/legacy-web` | 摄像头检测脚本和兼容 Web 资源 |
| `runtime/rk3588-python/config` | `payload/rk3588-python/config` | RK3588 Python 运行配置 |
| `runtime/rk182x-python/config` | `payload/rk182x-python/config` | RK182X Python 运行配置 |
| `runtime/shared` | `payload/shared` | 共享运行文件 |
| `systemd` | `systemd` | systemd unit 和 drop-in |
| `scripts` | `scripts` | 安装、校验、容器和运行辅助脚本 |
| `database` | `database` | schema、seed 和数据库配置脚本 |
| `package-reference` | 安装包根目录文件 | manifest、依赖说明和构建参考配置 |

## 未复制内容

为了避免把大文件和部署环境混入源码仓库，以下内容未复制：

- `dependencies/` 下的 APT 仓库、Python wheelhouse 和 RK182X Runtime。
- `payload/rk3588-python/model_container/releases/` 下的容器镜像。
- `payload/rk182x-models/` 和 RK3588/RK182X 模型文件。
- 构建输出压缩包和 `SHA256SUMS` 全量安装包清单。
- 设备密码、摄像头凭据、日志、告警记录和数据库运行数据。

## 构建注意事项

- Java 构建入口参考 `source/java/pom.xml`。
- Vue 构建入口参考 `source/ui/package.json`、`vite.config.ts` 和 `yarn.lock`。
- RK3588 Python 构建入口参考 `source/python/CMakeLists.txt` 和 `requirements-board.txt`。
- RK182X Python 构建入口参考 `source/rk182x-python/CMakeLists.txt` 和 `requirements-board.txt`。
- 当前运行网页使用 `runtime/kyscene/web`，前端源码修改后需要重新构建并替换该目录。
- 生产安装仍应使用正式离线安装包，不应直接将本目录当作完整部署包。

## 来源版本

对应安装包版本：

```text
3.0.9-history-clean-generic-h264-rkmpp-container-regex-permission-checksum-fix
```

该版本包含：

- Bash 正则判断修复。
- 容器脚本执行权限修复。
- 模型镜像校验文件 CRLF 修复。
- `model_container/systemd` 运行文件补齐。
