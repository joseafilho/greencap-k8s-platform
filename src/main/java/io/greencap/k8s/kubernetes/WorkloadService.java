package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<PodInfo> listPods(Cluster cluster, String namespace) {
        String kubeconfig = encryptionService.decrypt(cluster.getKubeconfigContent());
        try (KubernetesClient client = clientFactory.buildClient(kubeconfig)) {
            var items = isAllNamespaces(namespace)
                    ? client.pods().inAnyNamespace().list().getItems()
                    : client.pods().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(pod -> new PodInfo(
                            pod.getMetadata().getName(),
                            pod.getMetadata().getNamespace(),
                            Optional.ofNullable(pod.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pod.getSpec()).map(s -> s.getNodeName()).orElse("-"),
                            Optional.ofNullable(pod.getStatus())
                                    .map(s -> s.getContainerStatuses())
                                    .map(cs -> cs.stream().mapToInt(c -> c.getRestartCount()).sum())
                                    .orElse(0),
                            NamespaceService.age(pod.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list pods for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Erro ao listar pods: " + e.getMessage(), e);
        }
    }

    public List<DeploymentInfo> listDeployments(Cluster cluster, String namespace) {
        String kubeconfig = encryptionService.decrypt(cluster.getKubeconfigContent());
        try (KubernetesClient client = clientFactory.buildClient(kubeconfig)) {
            var items = isAllNamespaces(namespace)
                    ? client.apps().deployments().inAnyNamespace().list().getItems()
                    : client.apps().deployments().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(d -> new DeploymentInfo(
                            d.getMetadata().getName(),
                            d.getMetadata().getNamespace(),
                            Optional.ofNullable(d.getSpec()).map(s -> s.getReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getReadyReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getAvailableReplicas()).orElse(0),
                            NamespaceService.age(d.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list deployments for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Erro ao listar deployments: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
