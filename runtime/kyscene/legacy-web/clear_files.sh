#!/bin/bash

# 检查是否传入参数
if [ "$#" -eq 0 ]; then
  echo "用法: $0 路径1 路径2 ..."
  exit 1
fi

# 遍历传入的路径
for path in "$@"; do
  if [ -d "$path" ]; then
    echo "正在清空路径: $path（不删除隐藏文件/目录）"
    # 删除非隐藏的文件和目录（不包括以.开头的）
    find "$path" -mindepth 1 -maxdepth 1 ! -name ".*" -exec rm -rf {} \;
    echo "已完成: $path"
  else
    echo "路径不存在或不是目录: $path"
  fi
done