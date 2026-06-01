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
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.dto.PodMetricInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "observability/metrics", layout = MainLayout.class)
@PageTitle("Metrics — GreenCap K8s")
@PermitAll
public class MetricsView extends VerticalLayout implements BeforeEnterObserver {

    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<PodMetricInfo> metricsGrid = new Grid<>(PodMetricInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public MetricsView(ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildMetricsGrid();

        add(UiConstants.buildSectionHeader("Metrics: Top Pods", this::loadMetrics), noClusterMessage, metricsGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        metricsGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadMetrics();
        }
    }

    private void buildMetricsGrid() {
        metricsGrid.addColumn(PodMetricInfo::name)
                .setHeader("Pod").setFlexGrow(1).setSortable(true).setResizable(true);
        metricsGrid.addColumn(PodMetricInfo::namespace)
                .setHeader("Namespace").setWidth("160px").setFlexGrow(0).setSortable(true).setResizable(true);
        metricsGrid.addColumn(m -> m.cpuMillicores() + "m")
                .setHeader("CPU").setWidth("110px").setFlexGrow(0).setSortable(true).setResizable(true)
                .setComparator(PodMetricInfo::cpuMillicores);
        metricsGrid.addColumn(m -> m.memoryMiB() + "Mi")
                .setHeader("Memory").setWidth("110px").setFlexGrow(0).setSortable(true).setResizable(true)
                .setComparator(PodMetricInfo::memoryMiB);
        metricsGrid.setSizeFull();
        metricsGrid.setItems(Collections.emptyList());
        metricsGrid.setVisible(false);
    }

    private boolean loadMetrics() {
        if (clusterContext.getCluster() == null) return false;
        try {
            metricsGrid.setItems(observabilityService.listPodMetrics(
                    clusterContext.getCluster(), clusterContext.getNamespace()));
            return true;
        } catch (KubernetesOperationException e) {
            String message = e.getMessage().contains("404") || e.getMessage().contains("metrics")
                    ? "Metrics server not available. Enable metrics-server in your cluster."
                    : e.getMessage();
            notify(message, NotificationVariant.LUMO_ERROR);
            metricsGrid.setItems(Collections.emptyList());
            return false;
        }
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
