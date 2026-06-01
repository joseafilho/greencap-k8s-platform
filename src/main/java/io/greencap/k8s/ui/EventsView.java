package io.greencap.k8s.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.dto.EventInfo;
import jakarta.annotation.security.PermitAll;

import java.util.Collections;

@Route(value = "observability/events", layout = MainLayout.class)
@PageTitle("Events — GreenCap K8s")
@PermitAll
public class EventsView extends VerticalLayout implements BeforeEnterObserver {

    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<EventInfo> eventGrid = new Grid<>(EventInfo.class, false);
    private final VerticalLayout noClusterMessage;

    public EventsView(ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildEventGrid();

        add(new H3("Events"), noClusterMessage, eventGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        eventGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadEvents();
        }
    }

    private void buildEventGrid() {
        eventGrid.addComponentColumn(e -> typeBadge(e.type()))
                .setHeader("Type").setWidth("110px").setFlexGrow(0).setResizable(true);
        eventGrid.addColumn(EventInfo::reason)
                .setHeader("Reason").setWidth("180px").setFlexGrow(0).setResizable(true);
        eventGrid.addColumn(EventInfo::involvedObject)
                .setHeader("Object").setWidth("240px").setFlexGrow(0).setResizable(true);
        eventGrid.addComponentColumn(e -> wrappedText(e.message()))
                .setHeader("Message").setFlexGrow(1).setResizable(true);
        eventGrid.addColumn(EventInfo::count)
                .setHeader("Count").setWidth("80px").setFlexGrow(0).setResizable(true);
        eventGrid.addColumn(EventInfo::age)
                .setHeader("Age").setWidth("70px").setFlexGrow(0).setResizable(true);
        eventGrid.setSizeFull();
        eventGrid.setItems(Collections.emptyList());
        eventGrid.setVisible(false);
    }

    private Span wrappedText(String text) {
        Span span = new Span(text);
        span.getStyle().set("white-space", "normal").set("word-break", "break-word");
        return span;
    }

    private void loadEvents() {
        try {
            eventGrid.setItems(observabilityService.listEvents(
                    clusterContext.getCluster(), clusterContext.getNamespace()));
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            eventGrid.setItems(Collections.emptyList());
        }
    }

    private Span typeBadge(String type) {
        Span badge = new Span(type);
        badge.getElement().getThemeList().add("badge");
        if ("Warning".equals(type)) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("success");
        }
        return badge;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
