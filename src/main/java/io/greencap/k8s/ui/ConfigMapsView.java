package io.greencap.k8s.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.ConfigMapInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "config/configmaps", layout = MainLayout.class)
@PageTitle("ConfigMaps — GreenCap K8s")
@PermitAll
public class ConfigMapsView extends VerticalLayout implements BeforeEnterObserver {

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;

    private final Grid<ConfigMapInfo> configMapGrid = new Grid<>(ConfigMapInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public ConfigMapsView(ConfigurationService configurationService, ClusterContext clusterContext) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildConfigMapGrid();

        add(new H3("ConfigMaps"), noClusterMessage, configMapGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        configMapGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadConfigMaps();
        }
    }

    private void buildConfigMapGrid() {
        configMapGrid.addColumn(ConfigMapInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        configMapGrid.addColumn(cm -> cm.keyCount() + " keys").setHeader("Keys").setWidth("100px");
        configMapGrid.addColumn(ConfigMapInfo::namespace).setHeader("Namespace").setSortable(true);
        configMapGrid.addColumn(ConfigMapInfo::age).setHeader("Age").setWidth("80px");
        configMapGrid.setSizeFull();
        configMapGrid.setItems(Collections.emptyList());
        configMapGrid.setVisible(false);
    }

    private void loadConfigMaps() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        try {
            configMapGrid.setItems(configurationService.listConfigMaps(cluster, namespace));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            configMapGrid.setItems(Collections.emptyList());
        }
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
