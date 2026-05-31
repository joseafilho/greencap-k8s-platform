package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NamespaceService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Collections;
import java.util.List;

@Route(value = "workloads", layout = MainLayout.class)
@PageTitle("Workloads — GreenCap K8s")
@PermitAll
public class WorkloadsView extends VerticalLayout implements BeforeEnterObserver {

    private final NamespaceService namespaceService;
    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final ComboBox<String> namespaceCombo = new ComboBox<>("Namespace");
    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final Grid<DeploymentInfo> deployGrid = new Grid<>(DeploymentInfo.class, false);

    private final VerticalLayout noClusterMessage;
    private final HorizontalLayout toolbar;
    private final TabSheet tabs;

    public WorkloadsView(NamespaceService namespaceService, WorkloadService workloadService,
                         ClusterContext clusterContext) {
        this.namespaceService = namespaceService;
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = buildNoClusterMessage();
        toolbar = buildToolbar();
        tabs = buildTabs();

        add(noClusterMessage, toolbar, tabs);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        toolbar.setVisible(hasCluster);
        tabs.setVisible(hasCluster);
        if (hasCluster) {
            loadNamespaces();
        }
    }

    // ── No-cluster state ─────────────────────────────────────────────────────

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

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private HorizontalLayout buildToolbar() {
        namespaceCombo.setPlaceholder("Namespace...");
        namespaceCombo.setWidthFull();
        namespaceCombo.setEnabled(false);
        namespaceCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                clusterContext.setNamespace(e.getValue());
                refreshWorkloads();
            }
        });

        Button refreshBtn = new Button(VaadinIcon.REFRESH.create(), e -> refreshAll());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.getElement().setAttribute("title", "Atualizar");

        HorizontalLayout layout = new HorizontalLayout(namespaceCombo, refreshBtn);
        layout.setDefaultVerticalComponentAlignment(Alignment.END);
        layout.setWidthFull();
        layout.expand(namespaceCombo);
        layout.setVisible(false);
        return layout;
    }

    // ── Tabs & Grids ─────────────────────────────────────────────────────────

    private TabSheet buildTabs() {
        buildPodGrid();
        buildDeployGrid();

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.add("Pods", podGrid);
        tabSheet.add("Deployments", deployGrid);
        tabSheet.setVisible(false);
        return tabSheet;
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
    }

    // ── Badges ───────────────────────────────────────────────────────────────

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

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadNamespaces() {
        Cluster cluster = clusterContext.getCluster();
        try {
            List<String> namespaceNames = namespaceService.listNamespaceNames(cluster);

            namespaceCombo.setItems(namespaceNames);
            namespaceCombo.setEnabled(true);

            String preferred = namespaceNames.contains(clusterContext.getNamespace())
                    ? clusterContext.getNamespace()
                    : namespaceNames.stream().filter(n -> n.equals("default")).findFirst()
                            .orElse(namespaceNames.isEmpty() ? null : namespaceNames.get(0));

            if (preferred != null) {
                namespaceCombo.setValue(preferred);
            }
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshWorkloads() {
        Cluster cluster = clusterContext.getCluster();
        String ns = clusterContext.getNamespace();
        if (cluster == null || ns == null) return;

        try {
            podGrid.setItems(workloadService.listPods(cluster, ns));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            podGrid.setItems(Collections.emptyList());
        }

        try {
            deployGrid.setItems(workloadService.listDeployments(cluster, ns));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            deployGrid.setItems(Collections.emptyList());
        }
    }

    private void refreshAll() {
        if (clusterContext.getCluster() == null) return;
        refreshWorkloads();
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
