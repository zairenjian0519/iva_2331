/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.system.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author： DecoderX
 * @date： 2025/6/3
 */
@Data
@AllArgsConstructor
public class CommandResult {
    /**
     * 命令执行结果
     */
    String output;

    /**
     * 命令执行结果码
     */
    int exitCode;
}
