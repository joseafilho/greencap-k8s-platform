# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 1 | Setup + Auth + Login | ✅ Concluído |
| 2 | Conexão de Clusters (kubeconfig) | ✅ Concluído |
| 3 | Visualização de Workloads | ✅ Concluído |
| 4 | Estabilização + Ambiente Local | 🔄 Em andamento |
| 5 | Logs em Tempo Real (WebSocket) | 🔲 Pendente |
| 6 | RBAC + Polimento + Docker final | 🔲 Pendente |
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

---

## Backlog

### Sprint 4 — Estabilização + Ambiente Local 🔄

> Foco: garantir que tudo que foi implementado nas sprints 1–3 roda corretamente e pode ser validado visualmente. Sem novas features de escrita.

- [ ] Remover campo `apiUrl` da entidade `Cluster` (ADR-0001) + migration Flyway
- [ ] Montar `docker-compose` local com PostgreSQL para desenvolvimento
- [ ] Validar ponta a ponta: login → clusters → workloads (pods, deployments, namespaces)
- [ ] Documentar aviso de kubeconfig com certs por caminho na UI (demanda #3)
- [ ] `DashboardView`: cards com resumo dos clusters registrados (demanda #5)

### Sprint 5 — Logs em Tempo Real
- [ ] `LogStreamService`: stream de logs de pod via Fabric8 `watchLog()`
- [ ] WebSocket (STOMP) para push dos logs para o browser
- [ ] `LogsView` ou painel lateral na `WorkloadsView`: seleciona pod → exibe log ao vivo

### Sprint 6 — RBAC + Polimento + Docker Final
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
| 3 | 💡 Melhoria | Kubeconfig com certs por caminho falha fora da máquina local — usar `kubectl config view --flatten --minify` | 📝 Documentado |
| 4 | 💡 Melhoria | Testes unitários: `UserService`, `ClusterService`, `WorkloadService` | 🔲 Sprint 6 |
| 5 | 💡 Melhoria | `DashboardView` ainda é placeholder — adicionar cards com resumo dos clusters | 🔲 Sprint 6 |

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
