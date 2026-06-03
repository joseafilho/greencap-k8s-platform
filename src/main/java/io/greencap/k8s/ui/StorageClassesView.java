package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
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
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "infrastructure/storageclasses", layout = MainLayout.class)
@PageTitle("Storage Classes — GreenCap K8s")
@PermitAll
public class StorageClassesView extends VerticalLayout implements BeforeEnterObserver {

    private final StorageService storageService;
    private final ClusterContext clusterContext;

    private final Grid<StorageClassInfo> grid = new Grid<>(StorageClassInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public StorageClassesView(StorageService storageService, ClusterContext clusterContext) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("Storage Classes", this::loadStorageClasses), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadStorageClasses();
        }
    }

    private void buildGrid() {
        grid.addColumn(StorageClassInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(StorageClassInfo::provisioner).setHeader("Provisioner").setFlexGrow(2).setResizable(true);
        grid.addColumn(StorageClassInfo::reclaimPolicy).setHeader("Reclaim Policy").setWidth("130px").setResizable(true);
        grid.addColumn(StorageClassInfo::volumeBindingMode).setHeader("Binding Mode").setWidth("180px").setResizable(true);
        grid.addColumn(StorageClassInfo::allowVolumeExpansion).setHeader("Allow Expansion").setWidth("150px").setResizable(true);
        grid.addColumn(StorageClassInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(sc -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/storageclass/-/" + sc.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setSizeFull();
        grid.setItems(Collections.emptyList());
        grid.setVisible(false);
    }

    private boolean loadStorageClasses() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            grid.setItems(storageService.listStorageClasses(cluster));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            grid.setItems(Collections.emptyList());
            return false;
        }
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
