# Diagrama de componentes

Snapshot del corte de **estabilización post-E1** (13/09). Refleja los módulos y capas
implementados; todavía no incluye `activities` ni el ABM administrativo de offerings y
catálogos.

```mermaid
flowchart TB
    subgraph shared["shared (transversal)"]
        config["config\nSecurityConfig, CorsConfig, OpenApiConfig,\nJpaAuditingConfig, RequestIdFilter"]
        security["security\nJwtService, JwtAuthenticationFilter,\nAuthenticatedUser, TenantContext"]
        exception["exception\nApiException, GlobalExceptionHandler"]
        audit["audit\nAuditableEntity, TenantOwnedEntity,\nAuditorAwareImpl"]
        validation["validation\n@ValidCuit, @ValidPassword"]
        dto["dto\nPageResponse&lt;T&gt;"]
    end

    subgraph tenancy["tenancy"]
        Tenant["Tenant"]
    end

    subgraph access["access"]
        User["User, Role"]
        AuthController["AuthController"]
        UserController["UserController"]
        AuthService["AuthService"]
        UserService["UserService"]
        CustomUserDetailsService["CustomUserDetailsService"]
    end

    subgraph customers["customers"]
        Company["Company"]
        Contact["Contact"]
        CompanyController["CompanyController"]
        ContactController["ContactController"]
        CompanyService["CompanyService"]
        ContactService["ContactService"]
        CustomerVisibilityPort["CustomerVisibilityPort<br/>(puerto)"]
    end

    subgraph offerings["offerings"]
        Venue["Venue"]
        EventService["EventService"]
        VenueController["VenueController"]
        EventServiceController["EventServiceController"]
    end

    subgraph catalogs["catalogs"]
        Stage["Stage"]
        EventType["EventType"]
        StageController["StageController"]
        EventTypeController["EventTypeController"]
    end

    subgraph opportunities["opportunities"]
        Opportunity["Opportunity + servicios N:N"]
        StageHistory["StageHistory (append-only)"]
        OpportunityController["OpportunityController"]
        OpportunityService["OpportunityService"]
        OpportunityAccessService["OpportunityAccessService<br/>(alcance SELLER)"]
        ChangeStageService["ChangeStageService"]
    end

    access --> tenancy
    customers --> access
    offerings --> tenancy
    catalogs --> tenancy
    opportunities --> access
    opportunities --> customers
    opportunities --> offerings
    opportunities --> catalogs
    OpportunityAccessService -. implementa .-> CustomerVisibilityPort

    access -.-> shared
    customers -.-> shared
    offerings -.-> shared
    catalogs -.-> shared
    opportunities -.-> shared
```

## Reglas de dependencia (ADR-001)

- Las flechas llenas van del módulo consumidor al `service` público del módulo
  consumido — nunca a su `repository` ni a su `domain`. Por ejemplo,
  `OpportunityService` llama a `CompanyService.getSummary(id)`, nunca a
  `CompanyRepository`.
- Todo FK de `Opportunity` hacia otro módulo (`companyId`, `contactId`, `salesRepId`,
  `venueId`, `stageId`, `originId`) es un `Long` plano, no un `@ManyToOne` — evita el
  acoplamiento de entidades JPA entre módulos.
- Dentro de un mismo módulo sí hay relaciones JPA normales: `Contact.company` es un
  `@ManyToOne` real porque `Contact` y `Company` viven los dos en `customers`.
- Las flechas punteadas hacia `shared` son de uso transversal (seguridad, auditoría,
  errores), no relaciones de negocio.
- La flecha punteada `OpportunityAccessService → CustomerVisibilityPort` representa
  inversión de dependencias: el puerto pertenece a `customers`, mientras
  `opportunities` resuelve las relaciones usando exclusivamente su propio repositorio.
