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
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.PersistentVolumeInfo;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

@Route(value = "infrastructure/pvs", layout = MainLayout.class)
@PageTitle("PersistentVolumes — GreenCap K8s")
@PermitAll
public class PersistentVolumesView extends VerticalLayout implements BeforeEnterObserver {

    private final StorageService storageService;
    private final ClusterContext clusterContext;
    private final UserService userService;

    private final Grid<PersistentVolumeInfo> grid = new Grid<>(PersistentVolumeInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public PersistentVolumesView(StorageService storageService, ClusterContext clusterContext, UserService userService) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("PersistentVolumes", this::loadPersistentVolumes), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadPersistentVolumes();
        }
    }

    private void buildGrid() {
        grid.addColumn(PersistentVolumeInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(pv -> statusBadge(pv.status())).setHeader("Status").setWidth("110px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::capacity).setHeader("Capacity").setWidth("100px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::accessMode).setHeader("Access Mode").setWidth("150px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::reclaimPolicy).setHeader("Reclaim Policy").setWidth("130px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::storageClass).setHeader("Storage Class").setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(pv -> claimLink(pv.claim())).setHeader("Claim").setFlexGrow(2).setResizable(true);
        grid.addColumn(PersistentVolumeInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(pv -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/persistentvolume/-/" + pv.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setSizeFull();
        grid.setItems(Collections.emptyList());
        grid.setVisible(false);
    }

    private boolean loadPersistentVolumes() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            grid.setItems(storageService.listPersistentVolumes(cluster));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            grid.setItems(Collections.emptyList());
            return false;
        }
    }

    private com.vaadin.flow.component.Component claimLink(String claim) {
        if ("—".equals(claim)) return new Span(claim);
        Button link = new Button(claim);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> {
            String[] parts = claim.split("/", 2);
            if (parts.length == 2) {
                String namespace = parts[0];
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                clusterContext.setNamespace(namespace);
                userService.updateActiveNamespace(username, namespace);
            }
            UI.getCurrent().navigate(PersistentVolumeClaimsView.class);
        });
        return link;
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Available"  -> badge.getElement().getThemeList().add("success");
            case "Failed"     -> badge.getElement().getThemeList().add("error");
            default           -> badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
