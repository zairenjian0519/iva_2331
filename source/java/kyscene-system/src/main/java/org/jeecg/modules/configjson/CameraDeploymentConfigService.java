package org.jeecg.modules.configjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonParser;
import org.jeecg.modules.cameraManager.entity.CameraManager;
import org.jeecg.modules.cameraManager.service.ICameraManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class CameraDeploymentConfigService {
    private static final Path DEFAULT_PATH = Paths.get("/userdata/sps_test/iva/config/camera_deployment.json");
    private final Path configPath;
    private final Supplier<Set<String>> cameraIds;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    @Autowired
    public CameraDeploymentConfigService(ICameraManagerService cameraManagerService) {
        this(DEFAULT_PATH, () -> cameraManagerService.list().stream()
                .map(CameraManager::getId).collect(Collectors.toSet()));
    }

    CameraDeploymentConfigService(Path configPath, Supplier<Set<String>> cameraIds) {
        this.configPath = configPath;
        this.cameraIds = cameraIds;
    }

    public void removeAssignments(Collection<String> deletedCameraIds) throws IOException {
        Set<String> deleted = new HashSet<>(deletedCameraIds);
        if (!deleted.isEmpty()) update(id -> deleted.contains(id));
    }

    public void reconcile() throws IOException {
        Set<String> existing = cameraIds.get();
        update(id -> !existing.contains(id));
    }

    private void update(java.util.function.Predicate<String> remove) throws IOException {
        if (!Files.exists(configPath)) return;
        Path lockPath = configPath.resolveSibling(configPath.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            JsonNode parsed = mapper.readTree(configPath.toFile());
            if (!(parsed instanceof ObjectNode)) throw new IOException("camera deployment must be a JSON object");
            ObjectNode document = (ObjectNode) parsed;
            JsonNode camerasNode = document.get("cameras");
            if (camerasNode == null) return;
            if (!(camerasNode instanceof ObjectNode)) throw new IOException("camera deployment cameras must be an object");
            ObjectNode cameras = (ObjectNode) camerasNode;
            boolean changed = false;
            Iterator<String> names = cameras.fieldNames();
            while (names.hasNext()) {
                String id = names.next();
                if (remove.test(id)) {
                    names.remove();
                    changed = true;
                }
            }
            if (!changed) return;
            Path temporary = Files.createTempFile(configPath.getParent(), ".camera-deployment-", ".tmp");
            try {
                byte[] content = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
                try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.wrap(content);
                    while (buffer.hasRemaining()) output.write(buffer);
                    output.force(true);
                }
                Files.move(temporary, configPath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
