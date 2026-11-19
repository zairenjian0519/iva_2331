/*
 * Copyright © 2025 https://decoderx.cc/ All rights reserved.
 */
package org.jeecg.modules.iot.audioPickup.entity;

import lombok.Data;

import java.util.concurrent.Future;

/**
 * @author： DecoderX
 * @date： 2025/7/1
 */
@Data
public class RecordingTask {

    public Future<?> future;

    public Process process;
}