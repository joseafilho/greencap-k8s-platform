package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
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
import io.greencap.k8s.kubernetes.ClusterContext;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final ClusterContext clusterContext;
    private final Span clusterNameSpan = new Span();

    public MainLayout(ClusterContext clusterContext) {
        this.clusterContext = clusterContext;
        getElement().setAttribute("theme", Lumo.DARK);
        setPrimarySection(Section.DRAWER);
        addToNavbar(buildNavbar());
        addToDrawer(buildDrawer());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (clusterContext.getCluster() != null) {
            clusterNameSpan.setText(clusterContext.getCluster().getName());
            clusterNameSpan.setVisible(true);
        } else {
            clusterNameSpan.setVisible(false);
        }
    }

    private HorizontalLayout buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();

        Div spacer = new Div();

        Button logout = new Button(VaadinIcon.SIGN_OUT.create(),
                e -> UI.getCurrent().getPage().setLocation("logout"));
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.getElement().setAttribute("title", "Logout");

        HorizontalLayout navbar = new HorizontalLayout(toggle, spacer, logout);
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

    private VerticalLayout buildLogoSection() {
        Image logo = new Image("greencap.png", "GreenCap K8s");
        logo.setHeight("36px");

        Span appName = new Span("GreenCap K8s");
        appName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);

        HorizontalLayout logoRow = new HorizontalLayout(logo, appName);
        logoRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        logoRow.setSpacing(true);
        logoRow.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Top.MEDIUM);

        clusterNameSpan.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Bottom.SMALL
        );
        clusterNameSpan.setVisible(false);

        VerticalLayout section = new VerticalLayout(logoRow, clusterNameSpan);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
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
