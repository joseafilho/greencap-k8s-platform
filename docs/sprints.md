# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 1 | Setup + Auth + Login | ✅ Concluído |
| 2 | Conexão de Clusters (kubeconfig) | ✅ Concluído |
| 3 | Visualização de Workloads | ✅ Concluído |
| 4 | Estabilização + Ambiente Local | ✅ Concluído |
| 5 | Redesign de Layout + UX | ✅ Concluído |
| 6 | Login, Logout + UX de autenticação | ✅ Concluído |
| 7 | Cluster Atual por Sessão | ✅ Concluído |
| 8 | Refinamento de Navegação + Workloads | ✅ Concluído |
| 9 | Rede, Configuração e Demo | ✅ Concluído |
| 10 | UI Language Standardization | ✅ Concluído |
| 11 | UI Polish — ícones e navegação | ✅ Concluído |
| 12 | Observabilidade: Events | ✅ Concluído |
| 13 | Observabilidade: Metrics + UX global | ✅ Concluído |
| 14 | Persistência do Namespace ativo | ✅ Concluído |
| 15 | Visualização de Manifest (YAML) | ✅ Concluído |

---

## Sprints Concluídas

### Sprint 1 — Setup + Auth + Login
- Projeto Gradle (Kotlin DSL), Java 21, Spring Boot 3.3, Vaadin Flow 24
- `LoginView` + `SecurityConfig` (extends `VaadinWebSecurity`)
- `UserService` implementando `UserDetailsService`
- `DataInitializer`: cria `admin/admin` no primeiro startup
- Flyway migrations: `users`, `clusters`, `audit_logs`
- `MainLayout` (AppLayout + SideNav + logout)

### Sprint 2 — Conexão de Clusters
- `EncryptionService`: kubeconfig encriptado com AES via Spring Security Crypto
- `ClusterService`: adiciona cluster, testa conexão (Fabric8), persiste status
- `ClustersView`: grid de clusters + dialog de adição (upload `.yaml`/`.yml` ou paste)
- `KubernetesClientFactory`: recebe kubeconfig plaintext (desacoplado da entidade)

### Sprint 3 — Visualização de Workloads
- `ClusterContext` (`@VaadinSessionScope`): cluster e namespace ativos na sessão
- `WorkloadService`: lista pods e deployments via Fabric8
- `NamespaceService`: lista namespaces
- `WorkloadsView`: seletor de cluster + namespace, TabSheet (Pods | Deployments | Namespaces), badges de status

### Sprint 4 — Estabilização + Ambiente Local
- `apiUrl` removido da entidade `Cluster` — kubeconfig é fonte única de verdade (ADR-0001, migration V4)
- `docker-compose.dev.yml` com PostgreSQL para desenvolvimento local
- `DashboardView`: cards com contagem por `ConnectionStatus`, badge colorido, navegação para `ClustersView`
- `KubeconfigValidator`: detecta certs por caminho no YAML e bloqueia o salvar com instrução de correção
- Filtro de extensão removido do `Upload` — aceita arquivo `config` sem extensão

### Sprint 5 — Redesign de Layout + UX
- Dark theme fixo via Lumo (`getElement().setAttribute("theme", Lumo.DARK)`)
- Logo `greencap.png` no topo da sidebar
- Cluster ativo exibido abaixo do logo (atualizado por `AfterNavigationObserver`)
- Sidebar com 3 seções: VISÃO GERAL, OBSERVABILIDADE, CONFIGURAÇÃO
- Itens futuros visíveis e acinzentados (desabilitados via `pointer-events: none`)
- `SecurityConfig` liberando `/greencap.png` para acesso público
- Ação de remover cluster com dialog de confirmação (`ClusterService.deleteCluster()`)

### Sprint 6 — Login, Logout + UX de autenticação
- Dark theme aplicado na `LoginView`
- Logo `greencap.png` centralizada acima do formulário (140px)
- Logout via invalidação da sessão HTTP (`WrappedSession.invalidate()`) — sem GET para `/logout`

### Sprint 7 — Cluster Atual por Sessão
- Radio button (coluna "Ativo") no grid de `ClustersView` — seleção imediata define cluster ativo
- `ClusterContext` atualizado com cluster selecionado + namespace resetado para `"default"`
- Remoção do cluster ativo limpa `ClusterContext` automaticamente
- Navbar superior exibe `Cluster: <nome> <badge ConnectionStatus>`; "Nenhum cluster ativo" quando vazio
- Navbar atualiza imediatamente ao selecionar o cluster (sem precisar navegar)
- `WorkloadsView` usa `ClusterContext` diretamente — sem combobox de cluster; aviso inline com botão de navegação quando sem cluster ativo
- Aba "Namespaces" removida de `WorkloadsView` (redundante com o combobox de namespace)
- Cluster ativo persistido por usuário no banco (`active_cluster_id` em `users`) — restaurado automaticamente após login
- `@EqualsAndHashCode(of = "id")` adicionado à entidade `Cluster`
- Foco automático no campo Nome ao abrir dialog de novo cluster
- Hint do textarea de kubeconfig reforça uso de `kubectl config view --flatten --minify`
- Migration `V5`: normaliza valores de `ClusterProvider` para `Kubernetes`/`OpenShift`
- Migration `V6`: adiciona `active_cluster_id` em `users`
- Correção: `decrypt()` movido para dentro do `try-catch` em `NamespaceService` e `WorkloadService`
- `ClusterProvider` enum renomeado para `Kubernetes`/`OpenShift` (sem uppercase)

### Sprint 8 — Refinamento de Navegação + Workloads
- `WorkloadsView` dividida em `PodsView` (`/workloads/pods`) e `DeploymentsView` (`/workloads/deployments`)
- Menu lateral "Workloads" vira item pai colapsável com sub-itens Pods e Deployments
- Item "Namespaces" renomeado para "Topologia" (placeholder desabilitado para sprint futura)
- Item "Deploys" removido do menu (substituído pelo sub-menu de Workloads)
- Namespace selector (`ComboBox`) movido da toolbar de `WorkloadsView` para a navbar global do `MainLayout`
- Namespace selector oculto quando não há cluster ativo; visível e persistente após ativar cluster
- Namespaces recarregados apenas quando o cluster muda — sem chamadas redundantes ao Kubernetes API
- Ao trocar cluster: namespace zera, lista recarrega, seleciona `default` (ou primeiro disponível)
- Trocar namespace na navbar re-navega para a view ativa, disparando `beforeEnter` e recarregando dados

### Sprint 9 — Rede, Configuração e Demo (em andamento)

- `samples/greencap-demo/` — aplicação 3-tier demo (nginx + httpbin + redis) com manifests Kubernetes cobrindo: Namespace, Deployments, Services (ClusterIP e NodePort), ConfigMap, Secret (Opaque), PVC e HPA
- `create.sh` — habilita `metrics-server` via minikube addon e aplica todos os manifests em ordem; aguarda rollout dos Deployments
- `delete.sh` — remove o namespace `greencap-demo` e todos os recursos filhos
- `CONTEXT.md` — novos termos: `Service`, `ConfigMap`, `Secret`, `Rede`, `Configuração`, `Topologia` (futuro — grafo animado de objetos do namespace e suas relações)
- Sidebar: grupos colapsáveis Rede (Services) e Configuração (ConfigMaps, Secrets) adicionados ao `MainLayout`
- `ServicesView` (`/networking/services`): grid com nome, tipo (badge), clusterIP, portas, namespace, idade
- `ConfigMapsView` (`/config/configmaps`): grid com nome, contagem de keys, namespace, idade — valores nunca expostos
- `SecretsView` (`/config/secrets`): grid com nome, tipo (badge), contagem de keys, namespace, idade — valores nunca decodificados
- `H3` de título adicionado em todas as views (Pods, Deployments, Services, ConfigMaps, Secrets)
- Validado manualmente com cluster minikube e namespace `greencap-demo`

### Sprint 10 — UI Language Standardization

- Padronização de todo texto visível ao usuário para inglês: labels, botões, mensagens, notificações e exceções
- `buildNoClusterMessage()` extraído para `UiConstants` — eliminando duplicação em 5 views
- Sidebar renomeado: OVERVIEW / OBSERVABILITY / SETTINGS + menu "Parameters" (era "Configuração")
- `CONTEXT.md` atualizado: `Networking` e `Parameters` como termos canônicos (substituindo `Rede` e `Configuração`)
- Issues: 01 refactor UiConstants · 02 MainLayout · 03 Workloads views · 04 Networking/Parameters views · 05 ClustersView · 06 exception messages
- Fix pós-testes: cards do Dashboard traduzidos + largura da coluna Active em ClustersView ajustada
- Validado manualmente com cluster minikube e namespace greencap-demo

### Sprint 15 — Visualização de Manifest (YAML)

- `ManifestService`: busca e serializa YAML via Fabric8 `Serialization.asYaml()` para pod, deployment, service, configmap, secret
- `ManifestView`: página `/yaml/{resourceType}/{namespace}/{name}` com YAML em `<pre>` monospace, botão Back e título com tipo/nome do recurso
- Coluna de ação com ícone `CODE` (tamanho `UiConstants.ICON_SIZE`) em todas as 5 views de listagem (Pods, Deployments, Services, ConfigMaps, Secrets)
- Bug fix: trocar namespace com ManifestView ativa dispara `go(PREVIOUS_PAGE)` ao invés de recarregar a view, evitando YAML desatualizado
- `CONTEXT.md`: termo `Manifest` adicionado ao glossário
- Validado manualmente com aceite do usuário

### Sprint 14 — Persistência do Namespace ativo

- Campo `activeNamespace` (String) adicionado à entidade `User`
- Migration `V7__add_active_namespace_to_users.sql`: `ALTER TABLE users ADD COLUMN active_namespace VARCHAR(255)`
- `UserService.updateActiveNamespace()` e `findActiveNamespace()` seguindo padrão de `activeCluster`
- `MainLayout`: ao trocar namespace no ComboBox, persiste via `updateActiveNamespace`
- `MainLayout`: no login, restaura `activeNamespace` do banco antes de carregar a lista de namespaces
- Fallback silencioso: namespace salvo → "default" → primeiro da lista (lógica pré-existente)
- Validado: compilação e testes passando

### Sprint 13 — Observabilidade: Metrics + UX global

- `PodMetricInfo` record DTO com campos: name, namespace, cpuMillicores, memoryMiB
- `ObservabilityService.listPodMetrics()`: usa `client.top().pods().metrics(namespace)`, agrega containers por pod, ordena por CPU desc
- `MetricsView` (`/observability/metrics`): grid com CPU (ex: "250m") e Memory (ex: "128Mi"), colunas sortáveis e redimensionáveis
- Menu OBSERVABILITY: item "Metrics" ativado
- `CONTEXT.md`: termo `PodMetric` adicionado ao glossário
- UX global: colunas redimensionáveis em todas as views (Pods, Deployments, Services, ConfigMaps, Secrets, Clusters, Events, Metrics)
- Botão de refresh no canto superior direito de todas as listagens via `UiConstants.buildSectionHeader()`
- Notificação "Data updated" apenas em refresh bem-sucedido (`BooleanSupplier`)
- `UiConstants.ICON_SIZE = "28px"` — constante centralizada usada em todos os ícones de ação
- Vaadin Copilot desabilitado em dev via JVM system property no `bootRun`
- Validado manualmente com namespace `greencap-demo`

### Sprint 12 — Observabilidade: Events

- `EventInfo` record DTO com campos: type, reason, involvedObject, message, count, age
- `ObservabilityService.listEvents()`: lista eventos via `client.v1().events()` (core/v1), ordenados por `lastTimestamp` decrescente
- `EventsView` (`/observability/events`): grid com colunas redimensionáveis, badge Normal=verde/Warning=vermelho, Message com word-wrap
- Menu OBSERVABILITY: item "Logs" renomeado e ativado como "Events"
- `CONTEXT.md` atualizado: termo `Event` adicionado ao glossário
- Fix: namespace não resetava para "default" no F5 — `MainLayout` agora preserva o namespace do `ClusterContext` se ainda válido
- Validado manualmente com namespace `greencap-demo`

### Sprint 11 — UI Polish — ícones e navegação

- Ícones de ação (testar conexão e remover) em `ClustersView` aumentados: `LUMO_ICON` + ícone SVG em `28px`
- Seção "OVERVIEW" do menu lateral renomeada para "PROJECT"
- Duração das notificações aumentada de 4s para 6s (`UiConstants.NOTIFICATION_DURATION_MS`)
- Mensagem de teste de conexão corrigida: "Connection to X successful" (era "OK")
- Issue de identidade visual (paleta de cores GreenCap) descartada nesta sprint — requer avaliação da abordagem de theming sem dependência de Node.js/Vite
- Validado manualmente pelo usuário

## Backlog

### RBAC + Polimento + Docker Final
- [ ] Controle de acesso por role (`ADMIN`, `OPERATOR`, `VIEWER`) com `@Secured` nas views
- [ ] `UserManagementView` (apenas ADMIN): criar/desativar usuários
- [ ] Página de erro customizada no Vaadin
- [ ] `Dockerfile` + `docker-compose` validados ponta a ponta
- [ ] Variável `GREENCAP_ENCRYPTION_KEY` obrigatória em produção (validação no startup)

---

## Legenda

| Ícone | Significado |
|-------|-------------|
| ✅ | Concluído |
| ⏸ | Pausado |
| 🔲 | Pendente |
| 📝 | Documentado |
| 🐛 | Bug |
| 💡 | Melhoria |
