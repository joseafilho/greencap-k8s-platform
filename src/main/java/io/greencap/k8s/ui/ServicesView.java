package io.greencap.k8s.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "networking/services", layout = MainLayout.class)
@PageTitle("Services — GreenCap K8s")
@PermitAll
public class ServicesView extends VerticalLayout {
}
