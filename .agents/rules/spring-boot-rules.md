---
trigger: model_decision
description: Rules for Spring Boot backend development, minimal annotations, strictly no Lombok, constructor injection, and simple architecture.
---

# Spring Boot Development Rules

These rules govern all backend development in Java and Spring Boot.

Consult the authoritative specification in [docs/architecture-v2.md](../../docs/architecture-v2.md).

---

## 1. Minimal and Essential Annotations Only

Only use standard, essential Spring Boot annotations. Do not use advanced or unnecessary annotations that obscure how the application works.

Allowed and preferred annotations:
- **Controllers:** `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`
- **Services:** `@Service`
- **Repositories:** `@Repository` (or standard Spring Data `JpaRepository` interface)
- **Entities:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column` (when mapping requires it), `@Enumerated`
- **Transactions:** `@Transactional` (only when a service method modifies multiple database records in one unit of work)
- **Configuration:** `@Configuration`, `@Bean`, `@ConfigurationProperties`

Avoid complex annotations like `@Async` (unless configured simply via `ThreadPoolTaskExecutor`), `@Cacheable`, `@Aspect`, or custom annotation frameworks.

---

## 2. Strictly No Lombok

Do not use Project Lombok in any part of this project.
- Do NOT use `@Data`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, or `@Slf4j`.
- Always write standard Java constructors, getters, and setters explicitly.
- Write standard static loggers using SLF4J: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`.
- Explicit code helps a beginner understand Java object models without annotation magic.

---

## 3. Constructor-Based Dependency Injection

- Always inject dependencies through explicit constructors.
- Do NOT use field injection (`@Autowired` on private fields).
- Keep constructors simple and straightforward:

```java
@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final EvidenceService evidenceService;

    public EmailService(EmailRepository emailRepository, EvidenceService evidenceService) {
        this.emailRepository = emailRepository;
        this.evidenceService = evidenceService;
    }
}
```

---

## 4. Simple Architecture and Clean Layering

Organize backend code strictly into simple packages:
- `com.emailforensics.controller`: Handles HTTP requests, checks basic input, delegates to services, returns HTTP response entities.
- `com.emailforensics.service`: Contains business logic, orchestrates workflows, coordinates database and provider operations.
- `com.emailforensics.repository`: Spring Data JPA interfaces extending `JpaRepository`.
- `com.emailforensics.entity`: JPA entity classes matching the two PostgreSQL tables (`Email` and `Indicator`).
- `com.emailforensics.dto`: Simple Request and Response data transfer classes or Java records.
- `com.emailforensics.provider`: HTTP client classes for external threat intelligence providers.
- `com.emailforensics.model`: Shared internal data models (such as `ProviderObservation` and `RiskFactor`).
- `com.emailforensics.config`: Simple configuration classes (such as `AppProperties`, `AsyncConfig`, `CorsConfig`).
- `com.emailforensics.exception`: Custom exceptions and global exception handler.

Do not create generic base services, abstract controllers, or unnecessary intermediary layers.

---

## 5. Minimal and Practical Validation

- Validate only what is necessary for the MVP:
  - Check that an uploaded file is present and not empty.
  - Verify that the file ends with `.eml`.
  - Validate file size limit (default 10 MB).
  - Verify that the message can be parsed.
  - Check required database values before saving.
- Do not create custom validation annotation frameworks.

---

## 6. Simple Exception Handling

- Use a single `@RestControllerAdvice` class (`GlobalExceptionHandler`).
- Convert known application exceptions (such as `InvalidEmailException` or `ProviderException`) into a consistent JSON response:
  - `timestamp`
  - `status` (HTTP status code)
  - `code` (short error category string)
  - `message` (safe, user-friendly message)
- Never return raw Java stack traces, database details, or file paths in API error responses.

---

## 7. External HTTP Calls

- Use Spring Framework's synchronous fluent `RestClient` for external API communication.
- Do not introduce reactive programming (`WebClient`, WebFlux, Project Reactor).
- Configure reasonable connection timeouts (3 seconds) and read timeouts (8 seconds) for every provider client.
