# Runtime dependency inventory

Target baseline: same-profile OK3588 AArch64 boards, Ubuntu 22.04.4, running
kernel 5.10.0, a 40 GB virtio root disk at /dev/vda, and matching
MPP, RGA, RK3588 NPU, PCIe, and Rockchip FFmpeg BSP support. The installer
checks these prerequisites but does not replace the BSP.
Existing MySQL 8.0 and Mosquitto 2.0 installations retain their versions;
bundled versions are used only when those services are absent.

| Component | Offline source | Acceptance check |
| --- | --- | --- |
| MySQL 8, Mosquitto, Redis, Nginx, Java 17, Python 3.10 | ARM64 APT repository | Installed versions, services, 102 tables, Nginx routes |
| RK3588 RKNNLite2 and application Python modules | RK3588 wheelhouse and APT repository | Imports, MPP extension, 11 model roles |
| RK182X RKNN3Lite 1.0.0 and pinned Python modules | 32-wheel ARM64 wheelhouse | Offline installation, imports, model-pair loading |
| RK182X transfer proxy, API library and udev rule | RK182X SDK V1.0.0 | AArch64 ELF, device discovery, isolated RK1820 model initialization |
| ZLMediaKit and host-side RK3588 models | Validated ARM64 binaries and package payload | Media health, model manifest and model hashes |
| MPP, RGA, NPU, FFmpeg hardware codecs | Target BSP | Device nodes, libraries, codecs |

Exact APT package versions and SHA256 values are listed in
`dependencies/apt-package-manifest.tsv`; RKNN3 wheels are listed in
`dependencies/rknn3-wheel-manifest.tsv`. `SHA256SUMS` covers every release
file. The target writes its installed versions, card mapping, model and
service checks to `verify-report.txt` in its installation backup directory.
RKNN3Lite 1.0.0 hard-codes `/usr/lib/librknn3_api.so`; installation verifies
that exact declaration and redirects only the new venv copy to the private
library. The SDK wheel itself and any system library remain unchanged.

MySQL credentials, administrator password, media secret, certificates, and
the device record are generated on the target. The package excludes source
camera credentials, recordings, alarms, and machine identity. Grafana and
RK1828 model artifacts are outside this release.
