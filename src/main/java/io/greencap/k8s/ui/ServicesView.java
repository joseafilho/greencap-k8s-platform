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
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.dto.ServiceInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "networking/services", layout = MainLayout.class)
@PageTitle("Services — GreenCap K8s")
@PermitAll
public class ServicesView extends VerticalLayout implements BeforeEnterObserver {

    private final NetworkingService networkingService;
    private final ClusterContext clusterContext;

    private final Grid<ServiceInfo> serviceGrid = new Grid<>(ServiceInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public ServicesView(NetworkingService networkingService, ClusterContext clusterContext) {
        this.networkingService = networkingService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildServiceGrid();

        add(UiConstants.buildSectionHeader("Services", this::loadServices), noClusterMessage, serviceGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        serviceGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadServices();
        }
    }

    private void buildServiceGrid() {
        serviceGrid.addColumn(ServiceInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        serviceGrid.addComponentColumn(s -> typeBadge(s.type())).setHeader("Type").setWidth("140px").setResizable(true);
        serviceGrid.addColumn(ServiceInfo::clusterIP).setHeader("Cluster IP").setWidth("140px").setResizable(true);
        serviceGrid.addColumn(ServiceInfo::ports).setHeader("Port(s)").setFlexGrow(1).setResizable(true);
        serviceGrid.addColumn(ServiceInfo::namespace).setHeader("Namespace").setSortable(true).setResizable(true);
        serviceGrid.addColumn(ServiceInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        serviceGrid.addComponentColumn(s -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/service/" + s.namespace() + "/" + s.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);
        serviceGrid.setSizeFull();
        serviceGrid.setItems(Collections.emptyList());
        serviceGrid.setVisible(false);
    }

    private boolean loadServices() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            serviceGrid.setItems(networkingService.listServices(cluster, namespace));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            serviceGrid.setItems(Collections.emptyList());
            return false;
        }
    }

    private Span typeBadge(String type) {
        Span badge = new Span(type);
        badge.getElement().getThemeList().add("badge");
        switch (type) {
            case "LoadBalancer" -> badge.getElement().getThemeList().add("success");
            case "ClusterIP"    -> badge.getElement().getThemeList().add("contrast");
            default             -> {}
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
