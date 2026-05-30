package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterProvider;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.cluster.CreateClusterRequest;
import jakarta.annotation.security.PermitAll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Route(value = "clusters", layout = MainLayout.class)
@PageTitle("Clusters — GreenCap K8s")
@PermitAll
public class ClustersView extends VerticalLayout {

    private final ClusterService clusterService;
    private final Grid<Cluster> grid = new Grid<>(Cluster.class, false);

    public ClustersView(ClusterService clusterService) {
        this.clusterService = clusterService;
        setSizeFull();
        add(buildToolbar(), buildGrid());
        refreshGrid();
    }

    private HorizontalLayout buildToolbar() {
        Button addBtn = new Button("Adicionar Cluster", VaadinIcon.PLUS.create(),
                e -> openAddDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(new H2("Clusters"), addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.expand(new H2("Clusters"));
        toolbar.setWidthFull();
        return toolbar;
    }

    private Grid<Cluster> buildGrid() {
        grid.addColumn(Cluster::getName).setHeader("Nome").setSortable(true).setFlexGrow(1);
        grid.addColumn(c -> c.getProvider().name()).setHeader("Provider").setWidth("120px");
        grid.addColumn(Cluster::getApiUrl).setHeader("API URL").setFlexGrow(2);
        grid.addComponentColumn(c -> statusBadge(c.getConnectionStatus()))
                .setHeader("Status").setWidth("140px");
        grid.addComponentColumn(this::buildActions).setHeader("Ações").setWidth("100px");
        grid.setSizeFull();
        return grid;
    }

    private Span statusBadge(ConnectionStatus status) {
        Span badge = new Span(status.name());
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case CONNECTED    -> badge.getElement().getThemeList().add("success");
            case ERROR        -> badge.getElement().getThemeList().add("error");
            case DISCONNECTED -> badge.getElement().getThemeList().add("contrast");
            default           -> {}
        }
        return badge;
    }

    private Button buildActions(Cluster cluster) {
        Button testBtn = new Button(VaadinIcon.CONNECT.create(), e -> testConnection(cluster));
        testBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        testBtn.getElement().setAttribute("title", "Testar conexão");
        return testBtn;
    }

    private void testConnection(Cluster cluster) {
        ConnectionStatus status = clusterService.testConnection(cluster);
        refreshGrid();
        if (status == ConnectionStatus.CONNECTED) {
            notify("Conexão com " + cluster.getName() + " OK", NotificationVariant.LUMO_SUCCESS);
        } else {
            notify("Falha ao conectar em " + cluster.getName(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openAddDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Novo Cluster");
        dialog.setWidth("560px");

        TextField nameField = new TextField("Nome");
        nameField.setRequired(true);
        nameField.setWidthFull();

        Select<ClusterProvider> providerSelect = new Select<>();
        providerSelect.setLabel("Provider");
        providerSelect.setItems(ClusterProvider.values());
        providerSelect.setValue(ClusterProvider.KUBERNETES);
        providerSelect.setWidthFull();

        TextField apiUrlField = new TextField("API URL (opcional)");
        apiUrlField.setWidthFull();
        apiUrlField.setPlaceholder("https://api.mycluster.example.com:6443");

        TextArea kubeconfigArea = new TextArea("Kubeconfig YAML");
        kubeconfigArea.setWidthFull();
        kubeconfigArea.setMinHeight("200px");
        kubeconfigArea.setPlaceholder("Cole o conteúdo do kubeconfig ou faça upload do arquivo...");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".yaml", ".yml", ".kubeconfig", ".conf", ".config");
        upload.setMaxFileSize(512 * 1024);
        upload.setDropLabel(new Span("Arraste o kubeconfig aqui"));
        upload.addSucceededListener(e -> {
            try {
                String content = new String(buffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                kubeconfigArea.setValue(content);
            } catch (IOException ex) {
                notify("Erro ao ler arquivo", NotificationVariant.LUMO_ERROR);
            }
        });

        FormLayout form = new FormLayout(nameField, providerSelect, apiUrlField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout content = new VerticalLayout(form, upload, kubeconfigArea);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        Button saveBtn = new Button("Salvar", e -> {
            if (nameField.isEmpty()) {
                nameField.setErrorMessage("Nome é obrigatório");
                nameField.setInvalid(true);
                return;
            }
            if (kubeconfigArea.isEmpty()) {
                notify("Kubeconfig é obrigatório", NotificationVariant.LUMO_ERROR);
                return;
            }

            Cluster saved = clusterService.createCluster(new CreateClusterRequest(
                    nameField.getValue().trim(),
                    providerSelect.getValue(),
                    apiUrlField.getValue(),
                    kubeconfigArea.getValue()
            ));

            dialog.close();
            refreshGrid();

            String statusMsg = saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                    ? "conectado com sucesso"
                    : "adicionado (sem conexão — verifique o kubeconfig)";
            notify("Cluster " + saved.getName() + " " + statusMsg,
                    saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                            ? NotificationVariant.LUMO_SUCCESS
                            : NotificationVariant.LUMO_WARNING);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(clusterService.findAll());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
