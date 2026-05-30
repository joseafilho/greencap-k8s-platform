package io.greencap.k8s.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard — GreenCap K8s")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        add(new H2("Dashboard"));
        add(new Paragraph("Bem-vindo ao GreenCap K8s. Adicione um cluster para começar."));
    }
}
