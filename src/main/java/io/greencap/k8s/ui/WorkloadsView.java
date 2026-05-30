package io.greencap.k8s.ui;

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
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NamespaceService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;
import java.util.List;

@Route(value = "workloads", layout = MainLayout.class)
@PageTitle("Workloads — GreenCap K8s")
@PermitAll
public class WorkloadsView extends VerticalLayout implements BeforeEnterObserver {

    private final ClusterService clusterService;
    private final NamespaceService namespaceService;
    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final ComboBox<Cluster> clusterCombo = new ComboBox<>("Cluster");
    private final ComboBox<String> namespaceCombo = new ComboBox<>("Namespace");
    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final Grid<DeploymentInfo> deployGrid = new Grid<>(DeploymentInfo.class, false);
    private final Grid<NamespaceInfo> nsGrid = new Grid<>(NamespaceInfo.class, false);

    public WorkloadsView(ClusterService clusterService, NamespaceService namespaceService,
                         WorkloadService workloadService, ClusterContext clusterContext) {
        this.clusterService = clusterService;
        this.namespaceService = namespaceService;
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);
        add(buildToolbar(), buildTabs());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        clusterCombo.setItems(clusterService.findAll());
        if (clusterContext.getCluster() != null) {
            clusterCombo.setValue(clusterContext.getCluster());
        }
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private HorizontalLayout buildToolbar() {
        clusterCombo.setItemLabelGenerator(Cluster::getName);
        clusterCombo.setPlaceholder("Selecione um cluster...");
        clusterCombo.setWidthFull();
        clusterCombo.addValueChangeListener(e -> onClusterChanged(e.getValue()));

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

        HorizontalLayout toolbar = new HorizontalLayout(clusterCombo, namespaceCombo, refreshBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(clusterCombo, namespaceCombo);
        return toolbar;
    }

    // ── Tabs & Grids ─────────────────────────────────────────────────────────

    private TabSheet buildTabs() {
        buildPodGrid();
        buildDeployGrid();
        buildNsGrid();

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add("Pods", podGrid);
        tabs.add("Deployments", deployGrid);
        tabs.add("Namespaces", nsGrid);
        return tabs;
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

    private void buildNsGrid() {
        nsGrid.addColumn(NamespaceInfo::name).setHeader("Nome").setSortable(true).setFlexGrow(1);
        nsGrid.addComponentColumn(n -> phaseBadge(n.phase())).setHeader("Status").setWidth("120px");
        nsGrid.addColumn(NamespaceInfo::age).setHeader("Idade").setWidth("80px");
        nsGrid.setSizeFull();
        nsGrid.setItems(Collections.emptyList());
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

    private void onClusterChanged(Cluster cluster) {
        clusterContext.setCluster(cluster);
        clusterContext.setNamespace("default");
        namespaceCombo.setEnabled(cluster != null);
        namespaceCombo.clear();

        if (cluster == null) {
            clearGrids();
            return;
        }

        try {
            List<NamespaceInfo> namespaceInfos = namespaceService.listNamespaces(cluster);
            List<String> namespaceNames = namespaceInfos.stream().map(NamespaceInfo::name).toList();

            namespaceCombo.setItems(namespaceNames);
            nsGrid.setItems(namespaceInfos);

            String preferred = namespaceNames.contains(clusterContext.getNamespace())
                    ? clusterContext.getNamespace()
                    : namespaceNames.stream().filter(n -> n.equals("default")).findFirst()
                            .orElse(namespaceNames.isEmpty() ? null : namespaceNames.get(0));

            if (preferred != null) {
                namespaceCombo.setValue(preferred);
            }
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            clearGrids();
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
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        refreshWorkloads();
        try {
            nsGrid.setItems(namespaceService.listNamespaces(cluster));
        } catch (KubernetesOperationException e) {
            nsGrid.setItems(Collections.emptyList());
        }
    }

    private void clearGrids() {
        podGrid.setItems(Collections.emptyList());
        deployGrid.setItems(Collections.emptyList());
        nsGrid.setItems(Collections.emptyList());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
