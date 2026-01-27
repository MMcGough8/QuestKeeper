# Quarkus Deep Dive

A comprehensive technical guide to the Quarkus framework for cloud-native Java development.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Core Concepts and Philosophy](#core-concepts-and-philosophy)
3. [Architecture: Build-Time Optimization](#architecture-build-time-optimization)
4. [Getting Started](#getting-started)
5. [Key Features](#key-features)
   - [Dev Mode and Live Reload](#dev-mode-and-live-reload)
   - [Native Image Compilation](#native-image-compilation)
   - [Reactive Programming with Mutiny](#reactive-programming-with-mutiny)
   - [REST Endpoints with RESTEasy Reactive](#rest-endpoints-with-resteasy-reactive)
   - [Database Access with Panache](#database-access-with-panache)
   - [CDI Dependency Injection](#cdi-dependency-injection)
   - [Configuration System](#configuration-system)
6. [AI/LLM Integration with LangChain4j](#aillm-integration-with-langchain4j)
7. [Performance Benchmarks](#performance-benchmarks)
8. [Migration from Spring Boot](#migration-from-spring-boot)
9. [Testing Support](#testing-support)
10. [Production Readiness](#production-readiness)
11. [Extension Ecosystem](#extension-ecosystem)
12. [Best Practices](#best-practices)
13. [References](#references)

---

## Executive Summary

Quarkus is a cloud-native Java framework created by Red Hat, designed for Kubernetes and serverless environments. Its "supersonic subatomic Java" philosophy centers on dramatically reducing startup time and memory footprint through **build-time optimization** rather than runtime reflection.

### Key Value Propositions

| Metric | Quarkus Native | Quarkus JVM | Traditional Java |
|--------|---------------|-------------|------------------|
| Startup Time | ~0.046s | ~1.2s | ~3s |
| Memory (RSS) | ~35 MB | ~163 MB | ~278 MB |
| Container Size | ~50 MB | ~130 MB | ~250 MB+ |

### When to Use Quarkus

- **Microservices**: Fast startup, low memory footprint
- **Serverless/FaaS**: Sub-second cold starts
- **Kubernetes-native**: Built-in container and orchestration support
- **Event-driven architectures**: First-class reactive support
- **AI/ML applications**: Excellent LangChain4j integration

---

## Core Concepts and Philosophy

### What is Quarkus?

Quarkus is a full-stack, Kubernetes-native Java framework designed to optimize Java for containers, serverless, and cloud-native applications. It fundamentally rethinks how Java frameworks should operate in modern environments.

### The "Supersonic Subatomic Java" Philosophy

This tagline captures Quarkus's core value proposition:
- **Supersonic**: Extremely fast startup times (milliseconds in native mode)
- **Subatomic**: Minimal memory footprint

### Design Principles

1. **Container First**: Optimized for low memory usage and fast startup
2. **Unified Configuration**: Single configuration file for all extensions
3. **Standards-Based**: Built on proven standards (JAX-RS, CDI, JPA, etc.)
4. **Developer Joy**: Live reload, Dev Services, unified configuration
5. **Imperative and Reactive**: Support both programming models seamlessly

### Key Differentiators from Spring Boot

| Aspect | Quarkus | Spring Boot |
|--------|---------|-------------|
| **Optimization Model** | Build-time (augmentation phase) | Runtime (reflection, classpath scanning) |
| **Native Compilation** | First-class GraalVM support | Supported but not primary focus |
| **Startup Time** | 0.046s native / 1.2s JVM | 0.5s native / 3s JVM |
| **Memory (RSS)** | 35MB native / 163MB JVM | 110MB native / 278MB JVM |
| **DI Framework** | CDI (Jakarta EE) | Spring IoC |
| **Configuration** | MicroProfile Config | Spring Boot Config |
| **Dev Experience** | Live reload, Dev Services | Spring DevTools |
| **Standards** | Jakarta EE, MicroProfile | Proprietary + some standards |

---

## Architecture: Build-Time Optimization

### The "Shift-Left" Philosophy

Traditional Java frameworks perform these tasks **every time** the application starts:
- Classpath scanning for annotations
- Configuration parsing
- Metamodel building (CDI bean graphs, JPA entity models)
- Dynamic proxy generation
- Reflection-based dependency injection

Quarkus performs these tasks **once** during build time in an **augmentation phase**, producing optimized bytecode.

### How Build-Time Optimization Works

```
┌─────────────────────────────────────────────────────────────────┐
│                     BUILD TIME (Augmentation)                    │
├─────────────────────────────────────────────────────────────────┤
│  1. Parse application code and dependencies                      │
│  2. Resolve CDI beans, JPA entities, configurations             │
│  3. Generate optimized bytecode                                 │
│  4. Eliminate dead code                                         │
│  5. Pre-compute metamodels                                      │
│  6. Generate reflection configuration for native                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     RUNTIME (Minimal)                           │
├─────────────────────────────────────────────────────────────────┤
│  Just execute pre-computed logic - no scanning, no reflection   │
└─────────────────────────────────────────────────────────────────┘
```

### Extension System Architecture

Quarkus extensions are different from regular dependencies:
- They participate in the build-time augmentation
- They can generate bytecode, register resources for native compilation
- Extensions contain two parts:

```
quarkus-extension/
├── deployment/          # Build-time logic (augmentation)
│   └── src/main/java/
│       └── BuildSteps.java    # @BuildStep methods
└── runtime/             # Runtime code
    └── src/main/java/
        └── RuntimeClasses.java
```

### Benefits of Build-Time Processing

1. **Faster Startup**: No runtime classpath scanning
2. **Lower Memory**: No reflection metadata in memory
3. **Smaller Binaries**: Dead code elimination
4. **Native Compatible**: Pre-computed reflection configuration
5. **Fail Fast**: Configuration errors caught at build time

---

## Getting Started

### Creating a New Project

**Using the CLI:**
```bash
# Install Quarkus CLI
curl -Ls https://sh.quarkus.io/install | bash

# Create new project
quarkus create app com.example:my-app \
    --extension='resteasy-reactive,hibernate-orm-panache,jdbc-postgresql'

cd my-app
```

**Using Maven:**
```bash
mvn io.quarkus.platform:quarkus-maven-plugin:create \
    -DprojectGroupId=com.example \
    -DprojectArtifactId=my-app \
    -Dextensions='resteasy-reactive,hibernate-orm-panache,jdbc-postgresql'
```

**Using the Web UI:**
Visit [code.quarkus.io](https://code.quarkus.io) to generate a project with selected extensions.

### Project Structure

```
my-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       └── GreetingResource.java
│   │   ├── resources/
│   │   │   └── application.properties
│   │   └── docker/
│   │       ├── Dockerfile.jvm
│   │       └── Dockerfile.native
│   └── test/
│       └── java/
│           └── com/example/
│               └── GreetingResourceTest.java
├── pom.xml
└── README.md
```

### Running the Application

```bash
# Development mode with live reload
./mvnw quarkus:dev

# Build and run JVM mode
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Build native executable
./mvnw package -Pnative
./target/my-app-1.0-runner
```

---

## Key Features

### Dev Mode and Live Reload

Start with: `./mvnw quarkus:dev`

**Features:**
- **Instant Hot Reload**: Code changes apply in milliseconds without restart
- **Dev Services**: Automatic container provisioning for databases, message brokers, etc.
- **Continuous Testing**: Press `r` to run affected tests on every change
- **Dev UI**: Interactive dashboard at `/q/dev-ui`

**Dev Services Example:**
```xml
<!-- Just add the extension - no config needed for dev -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
<!-- PostgreSQL container starts automatically in dev mode -->
```

**Dev UI Features:**
- View all CDI beans
- Browse configuration options
- Execute database queries
- View OpenAPI documentation
- Manage Dev Services

### Native Image Compilation

Quarkus provides first-class support for GraalVM native image compilation.

**Build Commands:**
```bash
# Build native executable (requires GraalVM installed)
./mvnw package -Pnative

# Build using container (no local GraalVM needed)
./mvnw package -Pnative -Dquarkus.native.container-build=true

# Specify builder image
./mvnw package -Pnative \
    -Dquarkus.native.container-build=true \
    -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-java21
```

**Native Image Considerations:**

| Aspect | Support |
|--------|---------|
| Reflection | Requires `@RegisterForReflection` |
| Dynamic class loading | Not supported |
| Resources | Must be explicitly declared |
| Serialization | Requires configuration |

**Registering for Reflection:**
```java
@RegisterForReflection
public class MyDTO {
    public String name;
    public int value;
}

// Or register multiple classes
@RegisterForReflection(targets = {MyDTO.class, OtherDTO.class})
public class ReflectionConfiguration {}
```

**Resource Registration:**
```properties
# application.properties
quarkus.native.resources.includes=templates/**,data/*.json
```

### Reactive Programming with Mutiny

Quarkus uses **Mutiny** as its reactive library, providing two core types:

- `Uni<T>`: Single result (like CompletableFuture or Mono)
- `Multi<T>`: Stream of results (like Publisher or Flux)

**Basic Usage:**
```java
@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        return Uni.createFrom().item("Hello from Mutiny!")
                  .onItem().delayIt().by(Duration.ofMillis(100));
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> stream() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .map(tick -> "Tick: " + tick);
    }
}
```

**Common Mutiny Operations:**
```java
// Transform items
Uni<String> upper = uni.onItem().transform(String::toUpperCase);

// Chain async operations
Uni<Order> order = getUser(userId)
    .onItem().transformToUni(user -> getOrder(user));

// Handle errors
Uni<String> safe = uni.onFailure().recoverWithItem("default");

// Combine multiple Unis
Uni<Tuple2<User, Order>> combined = Uni.combine()
    .all().unis(getUser(id), getOrder(id))
    .asTuple();

// Convert to/from CompletableFuture
CompletableFuture<String> future = uni.subscribeAsCompletionStage();
Uni<String> fromFuture = Uni.createFrom().completionStage(future);
```

### REST Endpoints with RESTEasy Reactive

Quarkus uses RESTEasy Reactive for building REST APIs with JAX-RS annotations.

**Basic Resource:**
```java
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    public List<User> getAll() {
        return userService.findAll();
    }

    @GET
    @Path("/{id}")
    public User getById(@PathParam("id") Long id) {
        return userService.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @POST
    public Response create(CreateUserRequest request) {
        User user = userService.create(request);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @PUT
    @Path("/{id}")
    public User update(@PathParam("id") Long id, UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        userService.delete(id);
    }
}
```

**Reactive Endpoints:**
```java
@Path("/api/orders")
public class OrderResource {

    @Inject
    OrderService orderService;

    @GET
    public Uni<List<Order>> getAll() {
        return orderService.findAllAsync();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Order> streamOrders() {
        return orderService.streamNewOrders();
    }
}
```

**Exception Handling:**
```java
@Provider
public class ErrorMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        int status = 500;
        if (exception instanceof NotFoundException) {
            status = 404;
        } else if (exception instanceof IllegalArgumentException) {
            status = 400;
        }

        return Response.status(status)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }
}
```

### Database Access with Panache

Panache simplifies Hibernate ORM with either Active Record or Repository patterns.

**Active Record Pattern:**
```java
@Entity
@Table(name = "users")
public class User extends PanacheEntity {
    // id field is inherited from PanacheEntity

    @Column(unique = true)
    public String username;

    public String email;

    @Column(name = "created_at")
    public Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // Static finder methods
    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static List<User> findActive() {
        return list("active", true);
    }

    public static long countByEmail(String domain) {
        return count("email like ?1", "%" + domain);
    }
}

// Usage
User user = new User();
user.username = "john";
user.email = "john@example.com";
user.persist();

User found = User.findByUsername("john");
List<User> all = User.listAll();
User.deleteById(1L);
```

**Repository Pattern:**
```java
@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public List<User> findActive() {
        return list("active", true);
    }

    public List<User> search(String query) {
        return find("username like ?1 or email like ?1",
                    "%" + query + "%").list();
    }
}

// Usage
@Inject
UserRepository userRepository;

User user = new User();
userRepository.persist(user);
User found = userRepository.findByUsername("john");
```

**Custom UUID Primary Key:**
```java
@Entity
public class Order extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public UUID id;

    public String description;
    public BigDecimal amount;
}
```

**Pagination and Sorting:**
```java
// Pagination
PanacheQuery<User> query = User.findAll();
List<User> page1 = query.page(Page.of(0, 25)).list();
List<User> page2 = query.nextPage().list();

// Sorting
List<User> sorted = User.listAll(Sort.by("createdAt").descending());

// Combined
List<User> result = User.find("active", Sort.by("username"), true)
    .page(Page.of(0, 10))
    .list();
```

### CDI Dependency Injection

Quarkus uses CDI (Contexts and Dependency Injection) from Jakarta EE.

**Scope Annotations:**

| Annotation | Lifecycle |
|------------|-----------|
| `@ApplicationScoped` | One instance per application |
| `@RequestScoped` | One instance per HTTP request |
| `@SessionScoped` | One instance per HTTP session |
| `@Dependent` | New instance for each injection point |
| `@Singleton` | Like ApplicationScoped but not proxied |

**Basic Usage:**
```java
@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    EmailService emailService;

    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.username = request.username();
        user.email = request.email();
        userRepository.persist(user);
        emailService.sendWelcome(user);
        return user;
    }
}
```

**Producer Methods:**
```java
@ApplicationScoped
public class Producers {

    @Produces
    @ApplicationScoped
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Produces
    @RequestScoped
    public AuditContext auditContext(@Context HttpHeaders headers) {
        String userId = headers.getHeaderString("X-User-Id");
        return new AuditContext(userId);
    }
}
```

**Qualifiers:**
```java
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface Premium {}

@ApplicationScoped
public class StandardEmailService implements EmailService { }

@Premium
@ApplicationScoped
public class PremiumEmailService implements EmailService { }

// Usage
@Inject
EmailService standard;  // Gets StandardEmailService

@Inject
@Premium
EmailService premium;   // Gets PremiumEmailService
```

**CDI vs Spring Comparison:**

| Spring | CDI (Quarkus) |
|--------|---------------|
| `@Component` | `@ApplicationScoped` |
| `@Service` | `@ApplicationScoped` |
| `@Repository` | `@ApplicationScoped` |
| `@Autowired` | `@Inject` |
| `@Configuration` + `@Bean` | `@Produces` |
| `@Qualifier` | `@Qualifier` |
| `@Value` | `@ConfigProperty` |

### Configuration System

Quarkus uses MicroProfile Config with profile support.

**application.properties:**
```properties
# Application settings
quarkus.application.name=my-app
quarkus.http.port=8080

# Custom properties
greeting.message=Hello
greeting.name=World

# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=user
quarkus.datasource.password=password

# JPA
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true

# Profile-specific (dev)
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb_dev
%dev.greeting.message=Hello Dev

# Profile-specific (test)
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
%test.quarkus.hibernate-orm.database.generation=drop-and-create

# Profile-specific (prod)
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.quarkus.hibernate-orm.database.generation=none
```

**Injecting Configuration:**
```java
@ApplicationScoped
public class GreetingService {

    @ConfigProperty(name = "greeting.message")
    String message;

    @ConfigProperty(name = "greeting.name", defaultValue = "World")
    String name;

    @ConfigProperty(name = "greeting.enabled")
    Optional<Boolean> enabled;

    public String greet() {
        return message + ", " + name + "!";
    }
}
```

**Configuration Classes:**
```java
@ConfigMapping(prefix = "greeting")
public interface GreetingConfig {
    String message();

    @WithDefault("World")
    String name();

    Optional<Boolean> enabled();

    @WithName("max-length")
    int maxLength();
}

// Usage
@Inject
GreetingConfig config;

String msg = config.message();
```

**Environment Variable Override:**
```bash
# Properties can be overridden via environment variables
# greeting.message -> GREETING_MESSAGE
# quarkus.datasource.password -> QUARKUS_DATASOURCE_PASSWORD

GREETING_MESSAGE="Hello Production" java -jar app.jar
```

---

## AI/LLM Integration with LangChain4j

Quarkus has excellent LangChain4j integration through the `quarkus-langchain4j` extensions.

### Supported Providers

| Provider | Extension | Configuration Prefix |
|----------|-----------|---------------------|
| OpenAI | `quarkus-langchain4j-openai` | `quarkus.langchain4j.openai` |
| Anthropic Claude | `quarkus-langchain4j-anthropic` | `quarkus.langchain4j.anthropic` |
| Ollama (local) | `quarkus-langchain4j-ollama` | `quarkus.langchain4j.ollama` |
| Hugging Face | `quarkus-langchain4j-hugging-face` | `quarkus.langchain4j.huggingface` |
| Google Vertex AI | `quarkus-langchain4j-vertex-ai` | `quarkus.langchain4j.vertex-ai` |
| Azure OpenAI | `quarkus-langchain4j-azure-openai` | `quarkus.langchain4j.azure-openai` |
| Mistral | `quarkus-langchain4j-mistral` | `quarkus.langchain4j.mistral` |

### Basic Setup

**Add Dependencies:**
```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-openai</artifactId>
    <version>0.17.0</version>
</dependency>
```

**Configuration:**
```properties
# OpenAI
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4
quarkus.langchain4j.openai.chat-model.temperature=0.7

# Or Anthropic Claude
quarkus.langchain4j.anthropic.api-key=${ANTHROPIC_API_KEY}
quarkus.langchain4j.anthropic.chat-model.model-name=claude-sonnet-4-20250514
quarkus.langchain4j.anthropic.chat-model.temperature=0.3
quarkus.langchain4j.anthropic.chat-model.max-tokens=2048
quarkus.langchain4j.anthropic.timeout=30s
```

### Creating AI Services

**Declarative Approach with @RegisterAiService:**
```java
@RegisterAiService
public interface ContentAssistant {

    @SystemMessage("""
        You are a helpful content assistant for a social media platform.
        Always be concise and engaging. Keep responses under 280 characters
        unless specifically asked for longer content.
        """)
    String improveContent(@UserMessage String content);

    @SystemMessage("Analyze the sentiment of the following text.")
    SentimentResult analyzeSentiment(@UserMessage String text);

    @SystemMessage("""
        Generate 3 variations of a social media post based on the topic.
        Return as JSON array with 'text' and 'tone' fields.
        """)
    List<PostSuggestion> generatePosts(@UserMessage String topic);
}

// Usage - just inject!
@ApplicationScoped
public class ContentService {

    @Inject
    ContentAssistant assistant;

    public String improve(String content) {
        return assistant.improveContent(content);
    }
}
```

**Type-Safe Responses:**
```java
public record SentimentResult(
    String sentiment,      // POSITIVE, NEGATIVE, NEUTRAL
    double confidence,     // 0.0 to 1.0
    String explanation
) {}

public record PostSuggestion(
    String text,
    String tone           // casual, professional, humorous
) {}
```

### Advanced Features

**Memory (Conversation History):**
```java
@RegisterAiService
@SessionScoped
public interface ChatAssistant {

    @SystemMessage("You are a helpful assistant.")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
```

**Tools (Function Calling):**
```java
@ApplicationScoped
public class WeatherTools {

    @Tool("Get the current weather for a location")
    public String getWeather(String location) {
        // Call weather API
        return weatherService.getCurrentWeather(location);
    }

    @Tool("Get weather forecast for the next N days")
    public String getForecast(String location, int days) {
        return weatherService.getForecast(location, days);
    }
}

@RegisterAiService(tools = WeatherTools.class)
public interface WeatherAssistant {

    @SystemMessage("You help users with weather information. Use the available tools.")
    String chat(@UserMessage String message);
}
```

**RAG (Retrieval Augmented Generation):**
```java
// 1. Add embedding store extension
// quarkus-langchain4j-chroma, quarkus-langchain4j-pinecone, etc.

// 2. Ingest documents
@ApplicationScoped
public class DocumentIngestor {

    @Inject
    EmbeddingStore embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    public void ingest(Path documentPath) {
        Document document = FileSystemDocumentLoader.loadDocument(documentPath);
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
    }
}

// 3. Create RAG-enabled service
@RegisterAiService(retrievalAugmentor = DefaultRetrievalAugmentor.class)
public interface KnowledgeAssistant {

    @SystemMessage("Answer questions using the provided context. If unsure, say so.")
    String answer(@UserMessage String question);
}
```

**Streaming Responses:**
```java
@RegisterAiService
public interface StreamingAssistant {

    @SystemMessage("You are a helpful assistant.")
    Multi<String> chatStream(@UserMessage String message);
}

// Usage in REST endpoint
@GET
@Path("/chat/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<String> streamChat(@QueryParam("message") String message) {
    return assistant.chatStream(message);
}
```

### Configuration Reference

```properties
# Model settings
quarkus.langchain4j.openai.chat-model.model-name=gpt-4
quarkus.langchain4j.openai.chat-model.temperature=0.7
quarkus.langchain4j.openai.chat-model.max-tokens=2048
quarkus.langchain4j.openai.chat-model.top-p=1.0
quarkus.langchain4j.openai.chat-model.presence-penalty=0.0
quarkus.langchain4j.openai.chat-model.frequency-penalty=0.0

# Timeouts
quarkus.langchain4j.openai.timeout=30s

# Logging
quarkus.langchain4j.openai.log-requests=true
quarkus.langchain4j.openai.log-responses=true

# Embedding model
quarkus.langchain4j.openai.embedding-model.model-name=text-embedding-ada-002
```

---

## Performance Benchmarks

### Startup Time Comparison

| Framework | Mode | Startup Time | Speedup vs Spring JVM |
|-----------|------|--------------|----------------------|
| **Quarkus** | Native | **0.046 seconds** | **65x faster** |
| **Quarkus** | JVM | **1.211 seconds** | **2.5x faster** |
| Spring Boot | Native | 0.536 seconds | 5.5x faster |
| Spring Boot | JVM | 2.973 seconds | baseline |

### Memory Footprint (RSS)

| Framework | Mode | Memory | Reduction vs Spring JVM |
|-----------|------|--------|------------------------|
| **Quarkus** | Native | **35 MB** | **8x smaller** |
| **Quarkus** | JVM | **163 MB** | **1.7x smaller** |
| Spring Boot | Native | 110 MB | 2.5x smaller |
| Spring Boot | JVM | 278 MB | baseline |

### Throughput (Requests/Second)

| Framework | Requests/sec | Latency (avg) |
|-----------|--------------|---------------|
| **Quarkus Reactive** | **7,845** | **4.9 ms** |
| Quarkus Imperative | 6,200 | 6.2 ms |
| Spring Boot WebFlux | 5,800 | 6.8 ms |
| Spring Boot MVC | 5,370 | 7.4 ms |

### Container Image Sizes

| Configuration | Size |
|---------------|------|
| Quarkus Native (distroless) | ~50 MB |
| Quarkus Native (UBI minimal) | ~90 MB |
| Quarkus JVM (UBI minimal) | ~130 MB |
| Spring Boot Native | ~90 MB |
| Spring Boot JVM | ~250 MB+ |

### First Request Latency (Cold Start)

Critical for serverless/FaaS:

| Framework | Mode | First Request |
|-----------|------|---------------|
| **Quarkus** | Native | **~100 ms** |
| Quarkus | JVM | ~1.5 s |
| Spring Boot | Native | ~700 ms |
| Spring Boot | JVM | ~3.5 s |

---

## Migration from Spring Boot

### Spring Compatibility Extensions

Quarkus provides compatibility layers for gradual migration:

```xml
<!-- Spring Web annotations -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-web</artifactId>
</dependency>

<!-- Spring DI annotations -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-di</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-data-jpa</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-security</artifactId>
</dependency>

<!-- Spring Boot properties -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-boot-properties</artifactId>
</dependency>

<!-- Spring Scheduled -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-scheduled</artifactId>
</dependency>

<!-- Spring Cache -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-cache</artifactId>
</dependency>
```

### What's Supported

| Feature | Support Level |
|---------|--------------|
| `@RestController`, `@RequestMapping` | Full |
| `@GetMapping`, `@PostMapping`, etc. | Full |
| `@PathVariable`, `@RequestParam`, `@RequestBody` | Full |
| `@Component`, `@Service`, `@Repository` | Full |
| `@Autowired`, `@Qualifier` | Full |
| `@Value` | Full |
| `JpaRepository` | Basic methods |
| `@Query` | Full |
| `@PreAuthorize`, `@Secured` | Full |
| `@Scheduled` | Full |
| `@Cacheable`, `@CacheEvict` | Full |

### What's NOT Supported

| Feature | Alternative |
|---------|-------------|
| `BeanPostProcessor` | CDI extensions |
| `BeanFactoryPostProcessor` | Build-time processing |
| Full Spring AOP (`@Aspect`) | CDI interceptors |
| Spring Boot Actuator | Quarkus health/metrics |
| Spring WebFlux | Mutiny |
| Spring Data Specifications | Panache queries |
| Spring Profiles | Quarkus profiles (`%dev.`, `%prod.`) |
| `@ConfigurationProperties` nested | `@ConfigMapping` |

### Migration Strategy

**Option 1: Compatibility Layer (Fastest)**
1. Add Spring compatibility extensions
2. Copy existing code
3. Update configuration property names
4. Test and deploy
5. Gradually replace with native Quarkus

**Option 2: Full Rewrite (Best Performance)**
1. Create new Quarkus project
2. Rewrite controllers to JAX-RS
3. Rewrite services with CDI
4. Use Panache for data access
5. Leverage native compilation

**Option 3: Microservice Extraction**
1. Keep existing Spring Boot monolith
2. Extract new services as Quarkus
3. Gradually migrate existing services
4. Best for large production systems

### Configuration Property Mapping

| Spring Boot | Quarkus |
|-------------|---------|
| `server.port` | `quarkus.http.port` |
| `spring.datasource.url` | `quarkus.datasource.jdbc.url` |
| `spring.datasource.username` | `quarkus.datasource.username` |
| `spring.jpa.hibernate.ddl-auto` | `quarkus.hibernate-orm.database.generation` |
| `spring.jpa.show-sql` | `quarkus.hibernate-orm.log.sql` |
| `logging.level.root` | `quarkus.log.level` |
| `spring.profiles.active` | `quarkus.profile` |

---

## Testing Support

### @QuarkusTest

```java
@QuarkusTest
public class UserResourceTest {

    @Test
    public void testGetAllUsers() {
        given()
            .when().get("/api/users")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0));
    }

    @Test
    public void testCreateUser() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "newuser", "email": "new@example.com"}
                """)
            .when().post("/api/users")
            .then()
            .statusCode(201)
            .body("username", equalTo("newuser"));
    }
}
```

### Dev Services (Automatic Test Containers)

```java
@QuarkusTest
public class UserRepositoryTest {
    // PostgreSQL container starts automatically!
    // No Testcontainers configuration needed

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    public void testPersist() {
        User user = new User();
        user.username = "test";
        userRepository.persist(user);

        assertNotNull(user.id);
        assertEquals("test", User.findById(user.id).username);
    }
}
```

### Mocking with @InjectMock

```java
@QuarkusTest
public class OrderServiceTest {

    @InjectMock
    PaymentService paymentService;

    @Inject
    OrderService orderService;

    @Test
    public void testProcessOrder() {
        when(paymentService.charge(any())).thenReturn(PaymentResult.SUCCESS);

        Order order = orderService.process(new OrderRequest());

        assertEquals(OrderStatus.COMPLETED, order.status);
        verify(paymentService).charge(any());
    }
}
```

### Testing Native Executables

```java
@QuarkusIntegrationTest
public class NativeUserResourceIT extends UserResourceTest {
    // Runs against the native executable
    // Same tests, different runtime
}
```

### Test Profiles

```java
@QuarkusTest
@TestProfile(MockExternalServicesProfile.class)
public class IntegrationTest {
    // Tests run with mock profile configuration
}

public class MockExternalServicesProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "external.service.url", "http://localhost:8089",
            "feature.flag.enabled", "false"
        );
    }
}
```

---

## Production Readiness

### Kubernetes Integration

**Automatic Manifest Generation:**
```properties
# Enable Kubernetes extension
quarkus.kubernetes.deployment-target=kubernetes

# Service configuration
quarkus.kubernetes.service-type=ClusterIP
quarkus.kubernetes.replicas=3

# Resource limits
quarkus.kubernetes.resources.requests.memory=256Mi
quarkus.kubernetes.resources.requests.cpu=100m
quarkus.kubernetes.resources.limits.memory=512Mi
quarkus.kubernetes.resources.limits.cpu=500m

# Labels and annotations
quarkus.kubernetes.labels.app=my-app
quarkus.kubernetes.labels.version=1.0.0
```

**Build and Deploy:**
```bash
# Generate manifests
./mvnw package

# Manifests generated at target/kubernetes/kubernetes.yml

# Or build and push image
./mvnw package -Dquarkus.container-image.push=true
```

### Container Image Building

```properties
# Jib (no Docker daemon needed)
quarkus.container-image.builder=jib
quarkus.container-image.group=my-org
quarkus.container-image.name=my-app
quarkus.container-image.tag=1.0.0

# Registry
quarkus.container-image.registry=ghcr.io
quarkus.container-image.username=${REGISTRY_USER}
quarkus.container-image.password=${REGISTRY_TOKEN}
```

### Health Checks

```java
@Liveness
@ApplicationScoped
public class ApplicationLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("Application is live");
    }
}

@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection conn = dataSource.getConnection()) {
            return HealthCheckResponse.up("Database connection OK");
        } catch (SQLException e) {
            return HealthCheckResponse.down("Database unavailable");
        }
    }
}
```

**Endpoints:**
- `/q/health/live` - Liveness probes
- `/q/health/ready` - Readiness probes
- `/q/health` - All health checks

### Metrics (Micrometer/Prometheus)

```properties
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/q/metrics
```

**Custom Metrics:**
```java
@ApplicationScoped
public class OrderService {

    @Inject
    MeterRegistry registry;

    Counter ordersCreated;

    @PostConstruct
    void init() {
        ordersCreated = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(registry);
    }

    public Order createOrder(OrderRequest request) {
        Order order = processOrder(request);
        ordersCreated.increment();
        return order;
    }
}
```

### Distributed Tracing (OpenTelemetry)

```properties
quarkus.application.name=my-app
quarkus.otel.exporter.otlp.endpoint=http://jaeger:4317
quarkus.otel.exporter.otlp.traces.endpoint=http://jaeger:4317
```

### Security (OIDC/JWT)

```properties
# Keycloak/OIDC
quarkus.oidc.auth-server-url=https://keycloak/realms/my-realm
quarkus.oidc.client-id=my-app
quarkus.oidc.credentials.secret=${OIDC_SECRET}

# Path-based security
quarkus.http.auth.permission.authenticated.paths=/api/*
quarkus.http.auth.permission.authenticated.policy=authenticated

quarkus.http.auth.permission.public.paths=/api/public/*
quarkus.http.auth.permission.public.policy=permit
```

```java
@Path("/api/secure")
public class SecureResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public String me() {
        return "Hello, " + jwt.getName();
    }

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String admin() {
        return "Admin area";
    }
}
```

---

## Extension Ecosystem

### Popular Extensions

| Category | Extension | Purpose |
|----------|-----------|---------|
| **Web** | `resteasy-reactive` | REST endpoints |
| **Web** | `resteasy-reactive-jackson` | JSON serialization |
| **Database** | `hibernate-orm-panache` | ORM with simplified API |
| **Database** | `jdbc-postgresql` | PostgreSQL driver |
| **Database** | `reactive-pg-client` | Reactive PostgreSQL |
| **Caching** | `cache` | Application caching |
| **Caching** | `redis-client` | Redis integration |
| **Messaging** | `smallrye-reactive-messaging-kafka` | Kafka |
| **Messaging** | `smallrye-reactive-messaging-rabbitmq` | RabbitMQ |
| **Security** | `oidc` | OpenID Connect |
| **Security** | `security-jwt` | JWT authentication |
| **Observability** | `micrometer-prometheus` | Metrics |
| **Observability** | `opentelemetry` | Tracing |
| **AI** | `langchain4j-openai` | OpenAI integration |
| **AI** | `langchain4j-anthropic` | Claude integration |
| **Scheduling** | `scheduler` | Scheduled jobs |
| **Email** | `mailer` | Email sending |

### Finding Extensions

```bash
# List available extensions
quarkus extension list

# Search for extensions
quarkus extension list --search=kafka

# Add extension to project
quarkus extension add resteasy-reactive-jackson

# Remove extension
quarkus extension remove resteasy-reactive-jackson
```

---

## Best Practices

### Project Structure

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── resource/          # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Data access (if not using Panache entities)
│   │   ├── entity/            # JPA entities
│   │   ├── dto/               # Data transfer objects
│   │   ├── config/            # Configuration classes
│   │   └── exception/         # Custom exceptions, mappers
│   └── resources/
│       ├── application.properties
│       └── import.sql         # Initial data
└── test/
    └── java/com/example/
        └── resource/          # Test classes
```

### Configuration Best Practices

1. **Use profiles** for environment-specific config
2. **Externalize secrets** via environment variables
3. **Set sensible defaults** for development
4. **Document** all custom properties

```properties
# Good: Externalized, with defaults
database.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/devdb}
api.key=${API_KEY}  # Required, no default

# Bad: Hardcoded secrets
database.password=mysecretpassword
```

### Performance Tips

1. **Use reactive endpoints** for I/O-bound operations
2. **Enable native compilation** for production
3. **Configure connection pools** appropriately
4. **Use caching** for frequently accessed data
5. **Profile with Dev UI** during development

### Native Image Tips

1. **Register reflection** for DTOs used with JSON
2. **Include resources** explicitly
3. **Test native builds** in CI/CD
4. **Use container builds** for reproducibility

```java
// Register all DTOs for reflection
@RegisterForReflection(targets = {
    UserDTO.class,
    OrderDTO.class,
    // ... all DTOs
})
public class ReflectionConfig {}
```

---

## References

### Official Documentation
- [Quarkus Guides](https://quarkus.io/guides/)
- [Quarkus Extensions](https://quarkus.io/extensions/)
- [Quarkus GitHub](https://github.com/quarkusio/quarkus)

### LangChain4j
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Quarkus LangChain4j Extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)

### Migration
- [Spring DI Migration Guide](https://quarkus.io/guides/spring-di)
- [Spring Web Migration Guide](https://quarkus.io/guides/spring-web)
- [Spring Data JPA Migration Guide](https://quarkus.io/guides/spring-data-jpa)

### Performance
- [Native Image Guide](https://quarkus.io/guides/building-native-image)
- [Performance Tips](https://quarkus.io/guides/performance-measure)

### Community
- [Quarkus Blog](https://quarkus.io/blog/)
- [Quarkus Zulip Chat](https://quarkusio.zulipchat.com/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus)
