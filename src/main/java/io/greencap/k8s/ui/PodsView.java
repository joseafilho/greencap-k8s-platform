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
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "workloads/pods", layout = MainLayout.class)
@PageTitle("Pods — GreenCap K8s")
@PermitAll
public class PodsView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public PodsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = buildNoClusterMessage();
        buildPodGrid();

        add(noClusterMessage, podGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        podGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadPods();
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

    private void buildPodGrid() {
        podGrid.addColumn(PodInfo::name).setHeader("Nome").setSortable(true).setFlexGrow(2);
        podGrid.addColumn(PodInfo::namespace).setHeader("Namespace").setSortable(true);
        podGrid.addComponentColumn(p -> phaseBadge(p.phase())).setHeader("Status").setWidth("120px");
        podGrid.addColumn(PodInfo::node).setHeader("Node").setFlexGrow(1);
        podGrid.addColumn(PodInfo::restarts).setHeader("Restarts").setWidth("90px");
        podGrid.addColumn(PodInfo::age).setHeader("Idade").setWidth("80px");
        podGrid.setSizeFull();
        podGrid.setItems(Collections.emptyList());
        podGrid.setVisible(false);
    }

    private void loadPods() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        try {
            podGrid.setItems(workloadService.listPods(cluster, namespace));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            podGrid.setItems(Collections.emptyList());
        }
    }

    private Span phaseBadge(String phase) {
        Span badge = new Span(phase);
        badge.getElement().getThemeList().add("badge");
        switch (phase) {
            case "Running", "Active" -> badge.getElement().getThemeList().add("success");
            case "Pending"           -> badge.getElement().getThemeList().add("contrast");
            case "Failed"            -> badge.getElement().getThemeList().add("error");
            default                  -> {}
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
