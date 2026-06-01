package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NamespaceService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final ClusterContext clusterContext;
    private final NamespaceService namespaceService;
    private final HorizontalLayout clusterInfoLayout = new HorizontalLayout();
    private final HorizontalLayout namespaceLayout = new HorizontalLayout();
    private final ComboBox<String> namespaceCombo = new ComboBox<>();

    private Cluster lastLoadedCluster = null;
    private String currentPath = "";
    private boolean suppressNavigation = false;

    public MainLayout(ClusterContext clusterContext, UserService userService, NamespaceService namespaceService) {
        this.clusterContext = clusterContext;
        this.namespaceService = namespaceService;
        getElement().setAttribute("theme", Lumo.DARK);
        setPrimarySection(Section.DRAWER);

        clusterInfoLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        clusterInfoLayout.setSpacing(true);
        clusterInfoLayout.setPadding(false);

        buildNamespaceLayout();

        addToNavbar(buildNavbar());
        addToDrawer(buildDrawer());

        if (clusterContext.getCluster() == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.findActiveCluster(username).ifPresent(clusterContext::setCluster);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        currentPath = event.getLocation().getPath();
        updateClusterInfo();
        updateNamespaceSelector();
    }

    public void updateClusterInfo() {
        clusterInfoLayout.removeAll();
        Cluster cluster = clusterContext.getCluster();
        if (cluster != null) {
            Span label = new Span("Cluster:");
            label.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span name = new Span(cluster.getName());
            name.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);

            Span badge = new Span(cluster.getConnectionStatus().name());
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add("small");
            applyStatusTheme(badge, cluster.getConnectionStatus());

            clusterInfoLayout.add(label, name, badge);
        } else {
            Span noCluster = new Span("No active cluster");
            noCluster.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            clusterInfoLayout.add(noCluster);
        }
    }

    private void updateNamespaceSelector() {
        Cluster cluster = clusterContext.getCluster();
        namespaceLayout.setVisible(cluster != null);

        if (cluster == null) {
            lastLoadedCluster = null;
            return;
        }

        if (!cluster.equals(lastLoadedCluster)) {
            loadNamespacesForCluster(cluster);
        }
    }

    private void loadNamespacesForCluster(Cluster cluster) {
        try {
            List<String> names = namespaceService.listNamespaceNames(cluster);

            String current = clusterContext.getNamespace();
            String preferred;
            if (current != null && names.contains(current)) {
                preferred = current;
            } else if (names.contains("default")) {
                preferred = "default";
            } else {
                preferred = names.isEmpty() ? null : names.get(0);
            }

            suppressNavigation = true;
            namespaceCombo.setItems(names);
            if (preferred != null) {
                namespaceCombo.setValue(preferred);
                clusterContext.setNamespace(preferred);
            }
            suppressNavigation = false;

            lastLoadedCluster = cluster;
        } catch (KubernetesOperationException e) {
            Notification notification = Notification.show(
                    "Failed to load namespaces: " + e.getMessage(), UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void applyStatusTheme(Span badge, ConnectionStatus status) {
        switch (status) {
            case CONNECTED    -> badge.getElement().getThemeList().add("success");
            case ERROR        -> badge.getElement().getThemeList().add("error");
            case DISCONNECTED -> badge.getElement().getThemeList().add("contrast");
            default           -> {}
        }
    }

    private void buildNamespaceLayout() {
        Span label = new Span("Namespace:");
        label.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        namespaceCombo.setPlaceholder("Select...");
        namespaceCombo.setWidth("180px");
        namespaceCombo.getElement().getThemeList().add("small");
        namespaceCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !suppressNavigation) {
                clusterContext.setNamespace(e.getValue());
                UI.getCurrent().navigate(currentPath);
            }
        });

        namespaceLayout.add(label, namespaceCombo);
        namespaceLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        namespaceLayout.setSpacing(true);
        namespaceLayout.setPadding(false);
        namespaceLayout.setVisible(false);
    }

    private HorizontalLayout buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();

        Div spacer = new Div();

        Button logout = new Button(VaadinIcon.SIGN_OUT.create(), e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().executeJs("window.location.href='/login'");
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.getElement().setAttribute("title", "Logout");

        HorizontalLayout navbar = new HorizontalLayout(toggle, namespaceLayout, spacer, clusterInfoLayout, logout);
        navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navbar.expand(spacer);
        navbar.setWidthFull();
        return navbar;
    }

    private VerticalLayout buildDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeUndefined();
        drawer.setWidthFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        drawer.add(buildLogoSection());
        drawer.add(buildNavSection("PROJECT", buildVisaoGeralNav()));
        drawer.add(buildNavSection("OBSERVABILITY", buildObservabilidadeNav()));
        drawer.add(buildNavSection("SETTINGS", buildConfiguracaoNav()));

        return drawer;
    }

    private HorizontalLayout buildLogoSection() {
        Image logo = new Image("greencap.png", "GreenCap K8s");
        logo.setHeight("36px");

        Span appName = new Span("GreenCap K8s");
        appName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);

        HorizontalLayout logoRow = new HorizontalLayout(logo, appName);
        logoRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        logoRow.setSpacing(true);
        logoRow.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.MEDIUM
        );
        return logoRow;
    }

    private VerticalLayout buildNavSection(String label, SideNav nav) {
        Span sectionLabel = new Span(label);
        sectionLabel.addClassNames(
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Top.MEDIUM,
                LumoUtility.Padding.Bottom.XSMALL
        );

        VerticalLayout section = new VerticalLayout(sectionLabel, nav);
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        return section;
    }

    private SideNav buildVisaoGeralNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(
                new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()),
                buildWorkloadsNavItem(),
                buildRedeNavItem(),
                buildConfigNavItem(),
                disabledNavItem("Topologia", VaadinIcon.CLUSTER)
        );
        return nav;
    }

    private SideNavItem buildWorkloadsNavItem() {
        SideNavItem workloads = new SideNavItem("Workloads", PodsView.class, VaadinIcon.CUBES.create());
        workloads.addItem(new SideNavItem("Pods", PodsView.class, VaadinIcon.CUBE.create()));
        workloads.addItem(new SideNavItem("Deployments", DeploymentsView.class, VaadinIcon.ROCKET.create()));
        return workloads;
    }

    private SideNavItem buildRedeNavItem() {
        SideNavItem networking = new SideNavItem("Networking", ServicesView.class, VaadinIcon.CONNECT.create());
        networking.addItem(new SideNavItem("Services", ServicesView.class, VaadinIcon.SHARE.create()));
        return networking;
    }

    private SideNavItem buildConfigNavItem() {
        SideNavItem parameters = new SideNavItem("Parameters", ConfigMapsView.class, VaadinIcon.SLIDERS.create());
        parameters.addItem(new SideNavItem("ConfigMaps", ConfigMapsView.class, VaadinIcon.FILE_TEXT.create()));
        parameters.addItem(new SideNavItem("Secrets", SecretsView.class, VaadinIcon.LOCK.create()));
        return parameters;
    }

    private SideNav buildObservabilidadeNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(
                new SideNavItem("Events", EventsView.class, VaadinIcon.RECORDS.create()),
                new SideNavItem("Metrics", MetricsView.class, VaadinIcon.CHART.create())
        );
        return nav;
    }

    private SideNav buildConfiguracaoNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(
                new SideNavItem("Clusters", ClustersView.class, VaadinIcon.SERVER.create()),
                disabledNavItem("Users", VaadinIcon.USERS),
                disabledNavItem("Settings", VaadinIcon.COG)
        );
        return nav;
    }

    private SideNavItem disabledNavItem(String label, VaadinIcon icon) {
        SideNavItem item = new SideNavItem(label);
        item.setPrefixComponent(icon.create());
        item.getStyle()
                .set("opacity", "0.4")
                .set("pointer-events", "none");
        return item;
    }
}
