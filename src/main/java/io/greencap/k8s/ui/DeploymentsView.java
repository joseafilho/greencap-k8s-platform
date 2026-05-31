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
import com.vaadin.flow.theme.lumo.LumoUtility;
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

        noClusterMessage = buildNoClusterMessage();
        buildDeployGrid();

        add(noClusterMessage, deployGrid);
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

    private void buildDeployGrid() {
        deployGrid.addColumn(DeploymentInfo::name).setHeader("Nome").setSortable(true).setFlexGrow(2);
        deployGrid.addColumn(DeploymentInfo::namespace).setHeader("Namespace").setSortable(true);
        deployGrid.addComponentColumn(d -> replicasBadge(d.ready(), d.desired()))
                .setHeader("Réplicas").setWidth("100px");
        deployGrid.addColumn(DeploymentInfo::available).setHeader("Disponíveis").setWidth("110px");
        deployGrid.addColumn(DeploymentInfo::age).setHeader("Idade").setWidth("80px");
        deployGrid.setSizeFull();
        deployGrid.setItems(Collections.emptyList());
        deployGrid.setVisible(false);
    }

    private void loadDeployments() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        try {
            deployGrid.setItems(workloadService.listDeployments(cluster, namespace));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            deployGrid.setItems(Collections.emptyList());
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
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
