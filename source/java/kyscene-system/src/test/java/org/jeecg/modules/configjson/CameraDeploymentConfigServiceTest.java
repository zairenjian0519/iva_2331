package org.jeecg.modules.configjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CameraDeploymentConfigServiceTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deletionRemovesOnlySpecifiedOverride() throws Exception {
        Path config = folder.newFile("deployment.json").toPath();
        Files.write(config, ("{\"default\":0,\"cameras\":{\"deleted\":1,\"offline\":2},"
                + "\"rk182x_cards\":{\"1\":{\"device_id\":\"card-a\"}}}").getBytes(StandardCharsets.UTF_8));
        CameraDeploymentConfigService service = new CameraDeploymentConfigService(
                config, () -> Collections.singleton("offline"));

        service.removeAssignments(Arrays.asList("deleted", "absent"));
        JsonNode result = mapper.readTree(config.toFile());
        assertFalse(result.path("cameras").has("deleted"));
        assertEquals(2, result.path("cameras").path("offline").asInt());
        assertEquals("card-a", result.path("rk182x_cards").path("1").path("device_id").asText());
    }

    @Test
    public void reconcileKeepsOfflineCameraFoundInDatabase() throws Exception {
        Path config = folder.newFile("deployment.json").toPath();
        Files.write(config, "{\"cameras\":{\"offline\":1,\"stale\":2}}".getBytes(StandardCharsets.UTF_8));
        CameraDeploymentConfigService service = new CameraDeploymentConfigService(
                config, () -> Collections.singleton("offline"));

        service.reconcile();
        JsonNode result = mapper.readTree(config.toFile()).path("cameras");
        assertTrue(result.has("offline"));
        assertFalse(result.has("stale"));
    }

    @Test(expected = IOException.class)
    public void malformedConfigIsNotOverwritten() throws Exception {
        Path config = folder.newFile("deployment.json").toPath();
        Files.write(config, "{bad json".getBytes(StandardCharsets.UTF_8));
        new CameraDeploymentConfigService(config, Collections::emptySet)
                .removeAssignments(Collections.singleton("deleted"));
    }
}
