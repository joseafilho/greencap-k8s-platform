package io.greencap.k8s.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "config/secrets", layout = MainLayout.class)
@PageTitle("Secrets — GreenCap K8s")
@PermitAll
public class SecretsView extends VerticalLayout {
}
