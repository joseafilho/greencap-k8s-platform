package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(buildNavbar());
        addToDrawer(buildDrawer());
    }

    private HorizontalLayout buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();

        H1 appName = new H1("GreenCap K8s");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(),
                e -> UI.getCurrent().getPage().setLocation("logout"));
        logout.addClassNames(LumoUtility.Margin.Right.MEDIUM);

        HorizontalLayout navbar = new HorizontalLayout(toggle, appName, logout);
        navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navbar.expand(appName);
        navbar.setWidthFull();
        return navbar;
    }

    private VerticalLayout buildDrawer() {
        Span appTitle = new Span("GreenCap K8s");
        appTitle.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Padding.MEDIUM);

        SideNav nav = new SideNav();
        nav.addItem(
                new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()),
                new SideNavItem("Clusters", ClustersView.class, VaadinIcon.SERVER.create()),
                new SideNavItem("Workloads", WorkloadsView.class, VaadinIcon.CUBES.create())
        );

        VerticalLayout drawer = new VerticalLayout(appTitle, nav);
        drawer.setSizeUndefined();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        return drawer;
    }
}
