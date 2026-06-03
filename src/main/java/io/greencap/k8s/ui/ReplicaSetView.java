package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import io.greencap.k8s.kubernetes.dto.ReplicaSetInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "workloads/replicasets", layout = MainLayout.class)
@PageTitle("ReplicaSets — GreenCap K8s")
@PermitAll
public class ReplicaSetView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<ReplicaSetInfo> grid = new Grid<>(ReplicaSetInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public ReplicaSetView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("ReplicaSets", this::loadReplicaSets), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadReplicaSets();
        }
    }

    private void buildGrid() {
        grid.addColumn(ReplicaSetInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(ReplicaSetInfo::namespace).setHeader("Namespace").setSortable(true).setResizable(true);
        grid.addComponentColumn(rs -> navigationLink(rs.owner(), DeploymentsView.class))
                .setHeader("Owner").setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(rs -> replicasBadge(rs.ready(), rs.desired()))
                .setHeader("Ready / Desired").setWidth("130px").setResizable(true);
        grid.addColumn(ReplicaSetInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(rs -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/replicaset/" + rs.namespace() + "/" + rs.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setSizeFull();
        grid.setItems(Collections.emptyList());
        grid.setVisible(false);
    }

    private boolean loadReplicaSets() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            grid.setItems(workloadService.listReplicaSets(cluster, namespace));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            grid.setItems(Collections.emptyList());
            return false;
        }
    }

    private com.vaadin.flow.component.Component navigationLink(String label, Class<? extends com.vaadin.flow.component.Component> target) {
        if ("—".equals(label)) return new Span(label);
        Button link = new Button(label);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> UI.getCurrent().navigate(target));
        return link;
    }

    private Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (desired > 0 && ready >= desired) {
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
