# 👥 USER-MANAGEMENT-SERVICE

Microservicio para gestión de **roles**, **membresías** e **invitaciones** en Sentinel Security Scanner.

---

## 📋 DESCRIPCIÓN

### Responsabilidades

- **Roles en Tenant**: TENANT_ADMIN, TENANT_USER
- **Roles en Proyecto**: PROJECT_ADMIN, PROJECT_MEMBER, PROJECT_VIEWER
- **Invitaciones**: A tenants y proyectos
- **Gestión de Membresías**: Agregar, remover, actualizar roles
- **Permisos**: Verificación de acceso (internal API)

### Event-Driven Architecture

**Eventos Publicados:**
- `user.invited` - Cuando se invita a un usuario
- `user.invitation.accepted` - Cuando se acepta una invitación
- `user.access.revoked` - Cuando se revoca acceso

**Eventos Consumidos:**
- `tenant.created` - Asigna owner como TENANT_ADMIN
- `project.created` - Asigna owner como PROJECT_ADMIN

---

## 🛠️ TECNOLOGÍAS

- **Java 21**
- **Spring Boot 3.2.12**
- **PostgreSQL 15**
- **RabbitMQ** (Event Bus)
- **Spring Data JPA**
- **Spring AMQP**
- **Lombok**

---

## 📦 INSTALACIÓN

### 1. **Clonar Repositorio**

```bash
git clone <repo-url>
cd user-management-service
```

### 2. **Configurar Base de Datos**

```sql
-- Crear base de datos
CREATE DATABASE sentinel_user_mgmt;

-- Ejecutar schema.sql
psql -U postgres -d sentinel_user_mgmt -f src/main/resources/schema.sql
```

### 3. **Configurar `application.properties`**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sentinel_user_mgmt
spring.datasource.username=postgres
spring.datasource.password=tu_password

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 4. **Compilar y Ejecutar**

```bash
# Compilar
./mvnw clean install

# Ejecutar
./mvnw spring-boot:run

# O usando Java
java -jar target/user-management-service-0.0.1-SNAPSHOT.jar
```

El servicio estará disponible en: **http://localhost:8083**

---

## 🔗 ENDPOINTS

### **Tenant Members**

#### Obtener miembros de un tenant
```http
GET /api/tenants/{tenantId}/members
Headers:
  X-User-Id: uuid
```

#### Invitar usuario a tenant
```http
POST /api/tenants/{tenantId}/members/invite
Headers:
  X-User-Id: uuid
  X-User-Email: string
Body:
{
  "email": "user@example.com",
  "role": "TENANT_USER",
  "resourceName": "Mi Tenant"
}
```

#### Remover miembro
```http
DELETE /api/tenants/{tenantId}/members/{memberId}
Headers:
  X-User-Id: uuid
```

#### Ver invitaciones pendientes
```http
GET /api/tenants/{tenantId}/members/pending
```

---

### **Invitations**

#### Aceptar invitación
```http
POST /api/invitations/accept
Headers:
  X-User-Id: uuid
Body:
{
  "token": "invitation_token"
}
```

#### Mis invitaciones
```http
GET /api/invitations/me
Headers:
  X-User-Email: string
```

#### Ver invitación por token
```http
GET /api/invitations/{token}
```

#### Revocar invitación
```http
DELETE /api/invitations/{invitationId}
Headers:
  X-User-Id: uuid
```

---

### **Internal API (Permisos)**

#### Verificar permiso
```http
POST /api/internal/permissions/check
Body:
{
  "userId": "uuid",
  "tenantId": "uuid",
  "projectId": "uuid",
  "permission": "MANAGE_TENANT"
}

Response:
{
  "allowed": true,
  "userId": "uuid",
  "tenantId": "uuid",
  "permission": "MANAGE_TENANT"
}
```

#### Obtener rol en tenant
```http
GET /api/internal/permissions/tenant/{tenantId}/user/{userId}/role

Response: "TENANT_ADMIN"
```

---

## 🧪 TESTING

### **Flujo Completo**

#### 1. **Registrar Usuario en Auth-Service**

```bash
POST http://localhost:8081/api/auth/register
{
  "email": "admin@example.com",
  "password": "Admin123!",
  "role": "USER"
}

# Auth-Service publica: auth.user.registered
# Tenant-Service crea tenant FREE
# Tenant-Service publica: tenant.created
# User-Mgmt-Service asigna TENANT_ADMIN al owner
```

#### 2. **Verificar Membresía**

```bash
GET http://localhost:8083/api/tenants/{tenantId}/members
X-User-Id: {userId}

# Debe retornar el owner como TENANT_ADMIN
```

#### 3. **Invitar Usuario**

```bash
POST http://localhost:8083/api/tenants/{tenantId}/members/invite
X-User-Id: {adminUserId}
X-User-Email: admin@example.com
{
  "email": "user2@example.com",
  "role": "TENANT_USER",
  "resourceName": "Mi Workspace"
}

# Publica: user.invited
# Notification-Service enviará email (si configurado)
```

#### 4. **Aceptar Invitación**

```bash
# Primero, registrar el nuevo usuario
POST http://localhost:8081/api/auth/register
{
  "email": "user2@example.com",
  "password": "User123!",
  "role": "USER"
}

# Luego, aceptar invitación
POST http://localhost:8083/api/invitations/accept
X-User-Id: {user2Id}
{
  "token": "invitation_token_from_email"
}

# Publica: user.invitation.accepted
```

#### 5. **Verificar Permisos (Internal)**

```bash
POST http://localhost:8083/api/internal/permissions/check
{
  "userId": "{userId}",
  "tenantId": "{tenantId}",
  "permission": "MANAGE_TENANT"
}

# Retorna: { "allowed": true } o false
```

---

## 🔄 EVENTOS RABBITMQ

### **Exchanges y Queues**

```yaml
user-mgmt-exchange:
  type: topic
  queues:
    - user_mgmt.user.invited.queue (routing: user.invited)
    - user_mgmt.invitation.accepted.queue (routing: user.invitation.accepted)
    - user_mgmt.access.revoked.queue (routing: user.access.revoked)

tenant-exchange:
  queues:
    - user_mgmt.tenant.created.queue (routing: tenant.created)

project-exchange:
  queues:
    - user_mgmt.project.created.queue (routing: project.created)
```

### **Verificar RabbitMQ**

```bash
# Abrir Management UI
http://localhost:15672
user: guest
pass: guest

# Verificar queues
- user_mgmt.tenant.created.queue
- user_mgmt.project.created.queue
- user_mgmt.user.invited.queue

# Ver mensajes procesados en la pestaña "Queues"
```

---

## 📊 BASE DE DATOS

### **Tablas**

1. **tenant_members**
   - Membresías en tenants
   - Roles: TENANT_ADMIN, TENANT_USER

2. **project_members**
   - Membresías en proyectos
   - Roles: PROJECT_ADMIN, PROJECT_MEMBER, PROJECT_VIEWER

3. **invitations**
   - Invitaciones pendientes/aceptadas
   - Estados: PENDING, ACCEPTED, EXPIRED, REVOKED
   - TTL: 7 días por defecto

### **Verificar Datos**

```sql
-- Ver miembros de un tenant
SELECT * FROM tenant_members WHERE tenant_id = 'uuid';

-- Ver invitaciones pendientes
SELECT * FROM invitations WHERE status = 'PENDING';

-- Ver miembros de un proyecto
SELECT * FROM project_members WHERE project_id = 'uuid';
```

---

## 🛡️ PERMISOS

### **Tenant Roles**

| Role | Permisos |
|------|----------|
| `TENANT_ADMIN` | Gestionar miembros, invitar, cambiar roles, eliminar tenant |
| `TENANT_USER` | Ver miembros, crear proyectos (según límites) |

### **Project Roles**

| Role | Permisos |
|------|----------|
| `PROJECT_ADMIN` | Gestionar miembros, crear scans, eliminar proyecto |
| `PROJECT_MEMBER` | Crear scans, ver resultados |
| `PROJECT_VIEWER` | Solo ver scans y resultados |

---

## 🔧 CONFIGURACIÓN AVANZADA

### **Expiración de Invitaciones**

```properties
# Cambiar TTL (días)
invitation.expiration.days=7

# URL base para links de invitación
invitation.base.url=http://localhost:3000/invitations/accept
```

### **Cleanup Automático**

Un scheduler ejecuta diariamente a las 3 AM para:
- Marcar invitaciones expiradas
- Eliminar invitaciones antiguas (>90 días)

---

## 🐛 TROUBLESHOOTING

### **Error: "User is already a member"**

- Verificar que el usuario no esté ya en `tenant_members`
- Verificar invitaciones pendientes

### **Error: "Permission denied"**

- Verificar que el usuario tenga rol `TENANT_ADMIN` o `PROJECT_ADMIN`
- Revisar headers `X-User-Id`

### **Eventos no se consumen**

```bash
# Verificar RabbitMQ
docker ps | grep rabbitmq

# Ver logs
tail -f logs/user-management-service.log | grep "Received.*event"

# Verificar queues en RabbitMQ UI
http://localhost:15672/#/queues
```

---

## 📈 MONITORING

### **Actuator Endpoints**

```http
GET http://localhost:8083/actuator/health
GET http://localhost:8083/actuator/metrics
```

### **Logs**

```bash
# Ver logs en tiempo real
tail -f logs/user-management-service.log

# Buscar errores
grep "ERROR" logs/user-management-service.log
```

---

## 🚀 ROADMAP

- [ ] Email notifications para invitaciones
- [ ] Webhook support para eventos
- [ ] Roles custom por tenant
- [ ] Audit log de cambios de permisos
- [ ] Bulk invitations
- [ ] SSO/SAML integration

---

## 📚 DOCUMENTACIÓN RELACIONADA

- [Contexto Maestro](../CONTEXTO_MAESTRO.md)
- [Auth Service](../auth-service/README.md)
- [Tenant Service](../tenant-service/README.md)
- [Project Service](../project-service/README.md)

---

## 👨‍💻 DESARROLLO

### **Testing Local**

```bash
# Compilar y ejecutar tests
./mvnw test

# Ejecutar con perfiles
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### **Docker (Futuro)**

```bash
# Build image
docker build -t sentinel/user-management-service .

# Run container
docker run -p 8083:8083 sentinel/user-management-service
```

---

**Puerto:** 8083  
**Base de Datos:** sentinel_user_mgmt  
**RabbitMQ Exchange:** user-mgmt-exchange

✅ **SERVICIO LISTO PARA PRODUCCIÓN**