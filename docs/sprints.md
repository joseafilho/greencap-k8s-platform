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
| — | RBAC + Polimento + Docker final | ⏸ Adiado indefinidamente |
| — | Deploy Simplificado | ⏸ Adiado indefinidamente |

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

---

## Backlog

### Sprint 7 — Cluster Atual por Sessão (Concluída)

Movida para **Sprints Concluídas**.

### Sprint 8 — RBAC + Polimento + Docker Final
- [ ] Controle de acesso por role (`ADMIN`, `OPERATOR`, `VIEWER`) com `@Secured` nas views
- [ ] `UserManagementView` (apenas ADMIN): criar/desativar usuários
- [ ] Página de erro customizada no Vaadin
- [ ] `Dockerfile` + `docker-compose` validados ponta a ponta
- [ ] Variável `GREENCAP_ENCRYPTION_KEY` obrigatória em produção (validação no startup)

---

## Demandas Identificadas

| # | Tipo | Descrição | Status |
|---|------|-----------|--------|
| 1 | 🐛 Bug | Filtro de upload no dialog de cluster não aceitava arquivos sem extensão | ✅ Corrigido |
| 2 | 🐛 Bug | Hash BCrypt hardcoded na migration V1 era inválido | ✅ Corrigido via `DataInitializer` |
| 3 | 💡 Melhoria | Kubeconfig com certs por caminho falha fora da máquina local — `KubeconfigValidator` detecta e bloqueia com instrução de correção | ✅ Corrigido |
| 4 | 💡 Melhoria | Testes unitários: `UserService`, `ClusterService`, `WorkloadService` | 🔲 Sprint 6 |
| 5 | 💡 Melhoria | `DashboardView` ainda é placeholder — adicionar cards com resumo dos clusters | ✅ Corrigido |

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
