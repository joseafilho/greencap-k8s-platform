package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.stereotype.Component;

@Component
public class KubernetesClientFactory {

    public KubernetesClient buildClient(String kubeconfigContent) {
        Config config = Config.fromKubeconfig(kubeconfigContent);
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
