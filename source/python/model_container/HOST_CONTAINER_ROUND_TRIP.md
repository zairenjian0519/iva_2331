# RK3588 模型普通模式与容器模式往返操作

本文用于重复执行以下闭环：

```text
普通宿主机模型调试
  -> 制作唯一版本 OCI 镜像
  -> 安装只读模型容器
  -> 再回退普通模式继续调试
```

Python Engine 始终运行在宿主机。模型容器只提供只读模型文件，不运行推理、不访问 NPU、不开放端口。Python 中的模型路径始终保持：

```text
/userdata/sps_test/iva/weights/rknn
```

## 1. 判断当前模式

```bash
MODEL_DIR=/userdata/sps_test/iva/weights/rknn

findmnt -T "$MODEL_DIR"
mountpoint "$MODEL_DIR"
systemctl is-active ai-model-service.service ai-model-monitor.service \
  ai_server.service ai_engine.service
test -f /userdata/sps_test/iva/model-container/state/current.env && \
  cat /userdata/sps_test/iva/model-container/state/current.env
```

- `MODEL_DIR` 不是挂载点、文件系统是 `/userdata` 的 ext4：普通模式。
- `MODEL_DIR` 是带 `ro,nosuid,nodev` 的 overlay：容器模式。

## 2. 普通模式调试规则

只修改固定模型目录以及与模型配套的 Python manifest/业务代码。模型文件替换后重启 Engine：

```bash
sudo systemctl restart ai_engine.service
systemctl is-active ai_engine.service ai_server.service
curl --fail http://127.0.0.1:8899/healthcheck
journalctl -u ai_engine.service -n 100 --no-pager
```

不要修改 Python 模型根路径，不要手工创建 containerd snapshot。

## 3. 调试完成后重新制作模型容器

进入项目目录并确认当前确实是普通模式：

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
mountpoint /userdata/sps_test/iva/weights/rknn && {
  echo "模型目录仍是挂载点，不能执行 install.sh" >&2
  exit 1
}
```

校验模型角色和源文件：

```bash
/userdata/sps_test/iva_python/python_mpp/.venv/bin/python - <<'PY'
import json
from pathlib import Path

manifest = Path("../models/initial_models.json")
document = json.loads(manifest.read_text(encoding="utf-8"))
root = Path(document["model_root"])
for item in document["models"]:
    directory = root / item["directory"]
    model = directory / item.get("model", "model.rknn")
    labels = directory / item.get("labels", "labels.txt")
    if not model.is_file() or not labels.is_file():
        raise SystemExit(f"missing role={item['role']} model={model} labels={labels}")
    print(item["role"], model)
PY
```

生成唯一版本并构建、导入、安装：

```bash
VERSION="rk3588-rknn-$(date +%Y%m%d-%H%M%S)"
IMAGE_REF="docker.io/library/ai_model_service:${VERSION}"
ARCHIVE="/userdata/sps_test/iva/model-container/releases/ai_model_service-${VERSION}-arm64.tar"

sudo ./scripts/build-image.sh "$VERSION"
sudo ./scripts/import-image.sh "$IMAGE_REF" "$ARCHIVE"
sudo ./scripts/install.sh "$IMAGE_REF"
sudo ./scripts/verify.sh
```

`build-image.sh` 会比较构建前、复制后和镜像完成后的源 SHA256。模型目录在构建期间发生变化时，不会发布 release。

当前是普通模式时必须使用 `install.sh`。只有模型容器仍在运行、仅替换镜像时才使用：

```bash
sudo ./scripts/upgrade.sh "$NEW_IMAGE_REF"
```

## 4. 容器模式验收

```bash
sudo /usr/local/sbin/ai-model-monitor status
sudo ./scripts/verify.sh

findmnt -M /userdata/sps_test/iva/model-container/container-rootfs
findmnt -M /userdata/sps_test/iva/weights/rknn

sudo ctr --namespace ai-model tasks list
sudo ctr --namespace ai-model containers list
sudo ctr --namespace ai-model images list

systemctl is-active ai-model-service.service ai-model-monitor.service \
  ai_server.service ai_engine.service
curl --fail http://127.0.0.1:8899/healthcheck
```

模型目录必须是只读 overlay，bundle manifest 必须全部通过。不要在生产验收中执行会主动停止 task 的 `verify.sh --fault-test`。

## 5. 再次进入普通调试模式

正常路径使用本次安装生成的 `current.env` 和宿主机备份：

```bash
cd /userdata/sps_test/iva_python/duchun_rk3588_python/model_container
sudo ./scripts/rollback.sh
```

回退后验证：

```bash
mountpoint /userdata/sps_test/iva/weights/rknn && exit 1 || true
findmnt -T /userdata/sps_test/iva/weights/rknn

systemctl is-active ai_server.service ai_engine.service
curl --fail http://127.0.0.1:8899/healthcheck

test ! -e /etc/systemd/system/ai-model-service.service
test ! -e /etc/systemd/system/ai-model-monitor.service
```

回退不会删除 OCI release 和已导入镜像。调试完成后重新生成新的时间戳版本，再执行第 3 节的 `build/import/install/verify`。

## 6. 应急回退

仅当 `state/current.env` 或 `MODEL_BACKUP` 丢失、无法使用正常 `rollback.sh` 时，才复制当前已验证的容器 bundle 到宿主机：

```bash
sudo ./scripts/rollback-to-host-copy.sh --check
sudo ./scripts/rollback-to-host-copy.sh --apply
```

脚本从当前 `.ai-model-bundle/bundle.json` 动态读取版本、平台和文件数，不依赖固定版本。可选地锁定预期版本：

```bash
sudo EXPECTED_VERSION="$VERSION" ./scripts/rollback-to-host-copy.sh --check
```

应急回退会保留容器对象、镜像、控制文件和恢复脚本。完成日志位于：

```text
/userdata/sps_test/iva/model-container/host-rollback-<时间戳>/
```

## 7. 固定选择规则

| 当前状态 | 目标 | 命令 |
|---|---|---|
| 普通模式 | 安装新模型容器 | `install.sh` |
| 容器模式 | 直接切换新镜像 | `upgrade.sh` |
| 容器模式且备份完整 | 回到普通模式 | `rollback.sh` |
| 状态或备份损坏 | 应急复制到普通模式 | `rollback-to-host-copy.sh` |

## 8. 禁止操作

不要执行：

```bash
docker stop ai_model_service
docker rm ai_model_service
sudo ctr --namespace ai-model containers delete ai_model_service
```

不要手工修改 containerd snapshot，不要向只读模型挂载点复制文件，不要复用已有镜像标签。
