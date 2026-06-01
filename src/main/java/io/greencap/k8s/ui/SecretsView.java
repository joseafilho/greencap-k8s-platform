package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
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

        noClusterMessage = buildNoClusterMessage();
        buildSecretGrid();

        add(new H3("Secrets"), noClusterMessage, secretGrid);
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

    private VerticalLayout buildNoClusterMessage() {
        Span text = new Span("Nenhum cluster ativo. Selecione um cluster em Configuração → Clusters.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        Button goToClusters = new Button("Ir para Clusters", VaadinIcon.SERVER.create(),
                e -> UI.getCurrent().navigate(ClustersView.class));
        goToClusters.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(text, goToClusters);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }

    private void buildSecretGrid() {
        secretGrid.addColumn(SecretInfo::name).setHeader("Nome").setSortable(true).setFlexGrow(2);
        secretGrid.addComponentColumn(s -> typeBadge(s.type())).setHeader("Tipo").setFlexGrow(1);
        secretGrid.addColumn(s -> s.keyCount() + " keys").setHeader("Keys").setWidth("100px");
        secretGrid.addColumn(SecretInfo::namespace).setHeader("Namespace").setSortable(true);
        secretGrid.addColumn(SecretInfo::age).setHeader("Idade").setWidth("80px");
        secretGrid.setSizeFull();
        secretGrid.setItems(Collections.emptyList());
        secretGrid.setVisible(false);
    }

    private void loadSecrets() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        try {
            secretGrid.setItems(configurationService.listSecrets(cluster, namespace));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            secretGrid.setItems(Collections.emptyList());
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
