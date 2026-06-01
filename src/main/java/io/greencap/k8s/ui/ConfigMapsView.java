package io.greencap.k8s.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "config/configmaps", layout = MainLayout.class)
@PageTitle("ConfigMaps — GreenCap K8s")
@PermitAll
public class ConfigMapsView extends VerticalLayout {
}
