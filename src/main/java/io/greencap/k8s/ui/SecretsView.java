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
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.SecretInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "config/secrets", layout = MainLayout.class)
@PageTitle("Secrets — GreenCap K8s")
@PermitAll
public class SecretsView extends VerticalLayout implements BeforeEnterObserver {

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;

    private final Grid<SecretInfo> secretGrid = new Grid<>(SecretInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public SecretsView(ConfigurationService configurationService, ClusterContext clusterContext) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildSecretGrid();

        add(UiConstants.buildSectionHeader("Secrets", this::loadSecrets), noClusterMessage, secretGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        secretGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadSecrets();
        }
    }

    private void buildSecretGrid() {
        secretGrid.addColumn(SecretInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        secretGrid.addComponentColumn(s -> typeBadge(s.type())).setHeader("Type").setFlexGrow(1).setResizable(true);
        secretGrid.addColumn(s -> s.keyCount() + " keys").setHeader("Keys").setWidth("100px").setResizable(true);
        secretGrid.addColumn(SecretInfo::namespace).setHeader("Namespace").setSortable(true).setResizable(true);
        secretGrid.addColumn(SecretInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        secretGrid.addComponentColumn(s -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/secret/" + s.namespace() + "/" + s.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);
        secretGrid.setSizeFull();
        secretGrid.setItems(Collections.emptyList());
        secretGrid.setVisible(false);
    }

    private boolean loadSecrets() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            secretGrid.setItems(configurationService.listSecrets(cluster, namespace));
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            secretGrid.setItems(Collections.emptyList());
            return false;
        }
    }

    private Span typeBadge(String type) {
        Span badge = new Span(type);
        badge.getElement().getThemeList().add("badge");
        if ("Opaque".equals(type)) {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
