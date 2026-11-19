package org.jeecg.modules.cameraManager.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CameraEventListenerTest {

    @Test
    public void algorithmConfigDoesNotRequestAnotherRestartAfterStatusCheck() {
        assertFalse(CameraEventListener.shouldRestartAiServer("algorithm_config", false));
        assertTrue(CameraEventListener.shouldRestartAiServer("algorithm_config", true));
        assertFalse(CameraEventListener.shouldRestartAiServer("schedule_job", false));
        assertTrue(CameraEventListener.shouldRestartAiServer("schedule_job", true));
        assertTrue(CameraEventListener.shouldRestartAiServer("normal_job", false));
    }

    @Test
    public void leavesShortOfflineReasonUnchanged() {
        assertEquals("network unreachable", CameraEventListener.limitOfflineReason("network unreachable"));
    }

    @Test
    public void limitsOfflineReasonToFiftyCodePoints() {
        StringBuilder reason = new StringBuilder();
        for (int index = 0; index < 30; index++) {
            reason.append("摄像头").appendCodePoint(0x1F4F7);
        }

        String result = CameraEventListener.limitOfflineReason(reason.toString());

        assertEquals(50, result.codePointCount(0, result.length()));
    }

    @Test
    public void preservesNullOfflineReason() {
        assertNull(CameraEventListener.limitOfflineReason(null));
    }
}
