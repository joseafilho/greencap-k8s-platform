package io.greencap.k8s.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "workloads/deployments", layout = MainLayout.class)
@PageTitle("Deployments — GreenCap K8s")
@PermitAll
public class DeploymentsView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<DeploymentInfo> deployGrid = new Grid<>(DeploymentInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public DeploymentsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildDeployGrid();

        add(UiConstants.buildSectionHeader("Deployments", this::loadDeployments), noClusterMessage, deployGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        deployGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadDeployments();
        }
    }

    private void buildDeployGrid() {
        deployGrid.addColumn(DeploymentInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        deployGrid.addColumn(DeploymentInfo::namespace).setHeader("Namespace").setSortable(true).setResizable(true);
        deployGrid.addComponentColumn(d -> replicasBadge(d.ready(), d.desired()))
                .setHeader("Replicas").setWidth("100px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::available).setHeader("Available").setWidth("110px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        deployGrid.setSizeFull();
        deployGrid.setItems(Collections.emptyList());
        deployGrid.setVisible(false);
    }

    private boolean loadDeployments() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            deployGrid.setItems(workloadService.listDeployments(cluster, namespace));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            deployGrid.setItems(Collections.emptyList());
            return false;
        }
    }

    private Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (ready >= desired && desired > 0) {
            badge.getElement().getThemeList().add("success");
        } else if (ready == 0) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
