package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.EventInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservabilityService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<EventInfo> listEvents(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            var items = isAllNamespaces(namespace)
                    ? client.v1().events().inAnyNamespace().list().getItems()
                    : client.v1().events().inNamespace(namespace).list().getItems();

            log.debug("Found {} events in namespace '{}' for cluster '{}'", items.size(), namespace, cluster.getName());

            return items.stream()
                    .sorted(Comparator.comparing(
                            (Event e) -> Optional.ofNullable(e.getLastTimestamp()).orElse(""),
                            Comparator.reverseOrder()))
                    .map(e -> new EventInfo(
                            Optional.ofNullable(e.getType()).orElse("Normal"),
                            Optional.ofNullable(e.getReason()).orElse("-"),
                            formatObject(e),
                            Optional.ofNullable(e.getMessage()).orElse("-"),
                            Optional.ofNullable(e.getCount()).orElse(1),
                            NamespaceService.age(
                                    Optional.ofNullable(e.getLastTimestamp())
                                            .orElse(e.getMetadata().getCreationTimestamp()))
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list events for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list events: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }

    private String formatObject(Event event) {
        var ref = event.getInvolvedObject();
        if (ref == null) return "-";
        String kind = Optional.ofNullable(ref.getKind()).orElse("?");
        String name = Optional.ofNullable(ref.getName()).orElse("?");
        return kind + "/" + name;
    }
}
