# GreenCap K8s

A web platform for monitoring and managing external Kubernetes clusters. GreenCap does not provision clusters — it registers access credentials to clusters that exist outside the platform.

## Language

**Cluster**:
A registered access point to an external Kubernetes cluster. Stores an encrypted kubeconfig as the single credential needed to connect to it. GreenCap monitors and operates on Clusters but does not own or provision them.
_Avoid_: Connection, server, environment

**ConnectionStatus**:
The result of the last connection attempt to a Cluster. Not a persistent authoritative state — a snapshot of what was observed. Values: `UNKNOWN` (never tested), `CONNECTED` (last check succeeded), `DISCONNECTED` (cluster unreachable — timeout or no route), `ERROR` (cluster responded but rejected the request — invalid credentials or permission denied).
_Avoid_: Status, health, availability

**User**:
A person with access to the GreenCap platform. Has a role that determines what they can do.
_Avoid_: Account, member, principal

**Role**:
The access level of a User. `VIEWER` — read-only access to clusters and workloads. `OPERATOR` — read + write access to clusters and workload operations. `ADMIN` — full access including user management (create, deactivate).
_Avoid_: Permission, privilege, group

**Namespace**:
A logical isolation unit within a Cluster that groups Workloads. Not a Workload itself.
_Avoid_: Environment, project, partition

**Workload**:
A deployable unit running inside a Namespace. In GreenCap, the concrete types are Pod and Deployment.
_Avoid_: Resource, object, service

**Pod**:
The smallest Workload unit — one or more containers running together. Read-only in GreenCap (observed, not managed).
_Avoid_: Container, instance, process

**Deployment**:
A Workload that manages a set of replica Pods. Exposes desired, ready, and available replica counts.
_Avoid_: ReplicaSet, app, service

**ClusterProvider**:
Contextual metadata describing the Kubernetes distribution behind a Cluster (OKD, OpenShift, Kubernetes, Rancher). Does not alter GreenCap's behavior — used for display and identification only.
_Avoid_: Type, flavor, vendor

**Kubeconfig**:
The encrypted credential stored in a Cluster that contains everything needed to connect to the Kubernetes API (server URL, certificates, token). The single source of truth for cluster access — no separate URL field.
_Avoid_: Secret, token, certificate, credentials

**createdBy**:
Audit field on Cluster recording which User registered it. Does not imply ownership or restrict visibility — all Operators and Admins see all Clusters regardless of who created them.
_Avoid_: Owner, author, responsible

