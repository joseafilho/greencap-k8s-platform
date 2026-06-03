package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.PersistentVolumeClaimInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<PersistentVolumeClaimInfo> listPersistentVolumeClaims(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.persistentVolumeClaims().inAnyNamespace().list().getItems()
                    : client.persistentVolumeClaims().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(pvc -> new PersistentVolumeClaimInfo(
                            pvc.getMetadata().getName(),
                            pvc.getMetadata().getNamespace(),
                            pvc.getMetadata().getDeletionTimestamp() != null ? "Terminating"
                                    : Optional.ofNullable(pvc.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getResources())
                                    .map(r -> r.getRequests())
                                    .map(r -> r.get("storage"))
                                    .map(q -> q.toString())
                                    .orElse("—"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getAccessModes())
                                    .filter(modes -> !modes.isEmpty())
                                    .map(modes -> modes.get(0))
                                    .orElse("—"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getStorageClassName())
                                    .filter(sc -> !sc.isBlank())
                                    .orElse("—"),
                            NamespaceService.age(pvc.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list PVCs for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list PersistentVolumeClaims: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
