package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final ClusterContext clusterContext;
    private final HorizontalLayout clusterInfoLayout = new HorizontalLayout();

    public MainLayout(ClusterContext clusterContext, UserService userService) {
        this.clusterContext = clusterContext;
        getElement().setAttribute("theme", Lumo.DARK);
        setPrimarySection(Section.DRAWER);
        addToNavbar(buildNavbar());
        addToDrawer(buildDrawer());

        clusterInfoLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        clusterInfoLayout.setSpacing(true);
        clusterInfoLayout.setPadding(false);

        if (clusterContext.getCluster() == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.findActiveCluster(username).ifPresent(clusterContext::setCluster);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        updateClusterInfo();
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
            Span noCluster = new Span("Nenhum cluster ativo");
            noCluster.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            clusterInfoLayout.add(noCluster);
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

    private HorizontalLayout buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();

        Div spacer = new Div();

        Button logout = new Button(VaadinIcon.SIGN_OUT.create(), e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().executeJs("window.location.href='/login'");
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.getElement().setAttribute("title", "Logout");

        HorizontalLayout navbar = new HorizontalLayout(toggle, spacer, clusterInfoLayout, logout);
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
        drawer.add(buildNavSection("VISÃO GERAL", buildVisaoGeralNav()));
        drawer.add(buildNavSection("OBSERVABILIDADE", buildObservabilidadeNav()));
        drawer.add(buildNavSection("CONFIGURAÇÃO", buildConfiguracaoNav()));

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
                new SideNavItem("Workloads", WorkloadsView.class, VaadinIcon.CUBES.create()),
                disabledNavItem("Namespaces", VaadinIcon.FOLDER_O),
                disabledNavItem("Deploys", VaadinIcon.ROCKET)
        );
        return nav;
    }

    private SideNav buildObservabilidadeNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(
                disabledNavItem("Logs", VaadinIcon.LIST),
                disabledNavItem("Métricas", VaadinIcon.CHART)
        );
        return nav;
    }

    private SideNav buildConfiguracaoNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(
                new SideNavItem("Clusters", ClustersView.class, VaadinIcon.SERVER.create()),
                disabledNavItem("Usuários", VaadinIcon.USERS),
                disabledNavItem("Configurações", VaadinIcon.COG)
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
