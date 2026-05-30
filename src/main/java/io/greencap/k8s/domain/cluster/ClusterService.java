package io.greencap.k8s.domain.cluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserRepository;
import io.greencap.k8s.kubernetes.KubernetesClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final EncryptionService encryptionService;
    private final KubernetesClientFactory clientFactory;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Cluster> findAll() {
        return clusterRepository.findAllByOrderByCreatedAtDesc();
    }

    public Cluster createCluster(String name, ClusterProvider provider, String apiUrl, String kubeconfigContent) {
        Cluster cluster = new Cluster();
        cluster.setName(name);
        cluster.setProvider(provider);
        cluster.setApiUrl(StringUtils.isBlank(apiUrl) ? null : apiUrl.trim());

        cluster.setConnectionStatus(testWithPlaintext(kubeconfigContent));
        cluster.setKubeconfigContent(encryptionService.encrypt(kubeconfigContent));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(cluster::setCreatedBy);

        return clusterRepository.save(cluster);
    }

    public ConnectionStatus testConnection(Cluster cluster) {
        if (StringUtils.isBlank(cluster.getKubeconfigContent())) {
            return ConnectionStatus.UNKNOWN;
        }
        String plaintext = encryptionService.decrypt(cluster.getKubeconfigContent());
        ConnectionStatus status = testWithPlaintext(plaintext);
        cluster.setConnectionStatus(status);
        clusterRepository.save(cluster);
        return status;
    }

    private ConnectionStatus testWithPlaintext(String kubeconfigContent) {
        try (KubernetesClient client = clientFactory.buildClient(kubeconfigContent)) {
            client.namespaces().list();
            return ConnectionStatus.CONNECTED;
        } catch (Exception e) {
            log.debug("Cluster connection test failed: {}", e.getMessage());
            return ConnectionStatus.ERROR;
        }
    }

    public User getClusterOwner(Cluster cluster) {
        return cluster.getCreatedBy();
    }
}
