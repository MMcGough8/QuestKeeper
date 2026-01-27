# QuestKeeper → Quarkus Migration Analysis

A comprehensive review of migrating QuestKeeper to Quarkus, with focus on enabling AI-powered dynamic world generation.

---

## Executive Summary

**Recommendation: Yes, migrate to Quarkus** - but as a phased evolution, not a big-bang rewrite.

Quarkus provides the infrastructure QuestKeeper needs to achieve its AI vision:
- **LangChain4j integration** for LLM-powered world generation
- **REST API** for future web/mobile clients
- **Reactive streams** for real-time AI narrative streaming
- **Database persistence** for generated content and player progress
- **Cloud-native deployment** for scalable multi-user support

The current pure-CLI architecture is well-designed but not built for the AI-powered, potentially multi-user future you envision.

---

## Current Architecture Assessment

### Strengths to Preserve

| Component | Value | Migration Strategy |
|-----------|-------|-------------------|
| **YAML-driven campaigns** | Data-driven design, easy content creation | Keep as template system for AI seeding |
| **GameState separation** | Clean mutable/immutable boundary | Perfect foundation for AI state management |
| **Combatant interface** | Polymorphic combat | Direct port to Quarkus CDI beans |
| **Template Method effects** | Extensible magic item system | Convert to CDI-injected strategy beans |
| **D&D 5e mechanics** | Accurate rules implementation | Port as-is (pure Java, no framework deps) |
| **Trial/MiniGame system** | Skill check mechanics | Enhance with AI-generated challenges |
| **Flag-based progression** | Flexible state tracking | Expand for AI-generated story branches |

### Current Limitations for AI Vision

| Limitation | Impact on AI Goals |
|------------|-------------------|
| **No REST API** | Can't serve web/mobile clients or external AI services |
| **No database** | Can't persist AI-generated content across sessions |
| **Synchronous CLI** | Can't stream AI responses in real-time |
| **Single-user** | Can't support multiplayer or shared worlds |
| **No caching** | Repeated AI calls for same content waste resources |
| **No async support** | LLM calls block the game loop |

---

## Benefits of Quarkus Migration

### 1. LangChain4j Integration (Critical for AI Vision)

Quarkus has first-class LangChain4j support with declarative AI services:

```java
@RegisterAiService
public interface DungeonMasterAI {

    @SystemMessage("""
        You are a creative D&D 5e Dungeon Master for QuestKeeper.
        Generate content that fits the campaign tone and maintains consistency
        with established world facts.

        Always respond in JSON format with:
        - "narrative": The story text to display
        - "game_state_updates": Array of state changes (validated by game engine)
        - "choices": Optional player choices
        """)
    @UserMessage("""
        Campaign: {campaignContext}
        Current Location: {location}
        Player: {playerState}
        Recent Events: {recentHistory}
        World Facts: {worldFacts}

        Player Action: {playerInput}

        Generate the DM response:
        """)
    DMResponse processPlayerAction(
        String campaignContext,
        String location,
        String playerState,
        String recentHistory,
        String worldFacts,
        String playerInput
    );
}
```

**Provider Options:**
- **Claude (Anthropic)** - Excellent for nuanced storytelling, long context
- **GPT-4** - Strong general-purpose, good structured output
- **Ollama (local)** - Privacy-focused, offline play support
- **Mistral** - Cost-effective, good performance

### 2. Streaming AI Responses

Real-time narrative streaming creates immersive storytelling:

```java
@RegisterAiService
public interface StreamingDM {

    @SystemMessage("You are a dramatic D&D narrator...")
    Multi<String> narrateScene(@UserMessage String scenePrompt);
}

// REST endpoint for streaming
@Path("/api/game")
public class GameResource {

    @Inject
    StreamingDM dm;

    @GET
    @Path("/narrate")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> streamNarration(@QueryParam("scene") String scene) {
        return dm.narrateScene(scene);
    }
}
```

### 3. Database Persistence with Panache

Store AI-generated content and player progress:

```java
@Entity
public class GeneratedLocation extends PanacheEntity {
    public String campaignId;
    public String name;
    public String description;
    public String readAloudText;

    @Column(columnDefinition = "jsonb")
    public String exitsJson;  // Dynamic exits

    @Column(columnDefinition = "jsonb")
    public String npcsJson;   // Generated NPCs

    public String generationPrompt;  // For regeneration
    public Instant createdAt;

    // Finder methods
    public static List<GeneratedLocation> findByCampaign(String campaignId) {
        return list("campaignId", campaignId);
    }
}
```

### 4. Reactive Programming with Mutiny

Handle async AI calls without blocking:

```java
@ApplicationScoped
public class AIGameEngine {

    @Inject
    DungeonMasterAI dm;

    @Inject
    GameStateService gameState;

    public Uni<GameResponse> processAction(String playerId, String action) {
        return Uni.createFrom().item(() -> gameState.getContext(playerId))
            .onItem().transformToUni(context ->
                callAI(context, action)
            )
            .onItem().transform(aiResponse ->
                validateAndApply(aiResponse, playerId)
            )
            .onFailure().recoverWithItem(error ->
                fallbackResponse(error)
            );
    }
}
```

### 5. Multi-Client Architecture

Support CLI, web, and mobile simultaneously:

```
                    ┌─────────────────┐
                    │   Quarkus API   │
                    │   (REST + SSE)  │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼────┐        ┌─────▼─────┐       ┌─────▼─────┐
    │   CLI   │        │    Web    │       │  Mobile   │
    │ Client  │        │  Client   │       │   App     │
    └─────────┘        └───────────┘       └───────────┘
```

### 6. Performance & Deployment

| Aspect | Benefit |
|--------|---------|
| **Native compilation** | 50ms startup for serverless functions |
| **Low memory** | 35MB RSS enables cheap cloud hosting |
| **Container-ready** | Auto-generated Kubernetes manifests |
| **Dev Services** | Auto-provisioned Postgres/Redis in dev mode |

---

## Challenges and Considerations

### 1. Learning Curve

| Concept | Spring Equivalent | Effort |
|---------|-------------------|--------|
| CDI (`@Inject`, `@ApplicationScoped`) | `@Autowired`, `@Component` | Low - similar |
| RESTEasy Reactive | Spring MVC | Medium - different annotations |
| Panache | Spring Data JPA | Medium - different patterns |
| Mutiny (`Uni`, `Multi`) | Reactor (`Mono`, `Flux`) | Medium-High |
| MicroProfile Config | `@Value`, `application.yml` | Low |

**Mitigation:** Quarkus provides Spring compatibility extensions for gradual migration.

### 2. CLI Client Extraction

The current `GameEngine` mixes orchestration with terminal I/O. You'll need to:

1. **Extract game logic** into framework-agnostic services
2. **Create REST API** that exposes game actions
3. **Build new CLI client** that calls the API

```java
// Before: GameEngine does everything
public class GameEngine {
    private Scanner scanner;  // Terminal I/O mixed in

    public void processCommand(String input) {
        // Business logic + display logic combined
    }
}

// After: Separated concerns
@ApplicationScoped
public class GameService {
    // Pure business logic, no I/O
    public GameResponse processCommand(String playerId, String input) {
        return new GameResponse(narrative, stateChanges, choices);
    }
}

@Path("/api/game")
public class GameResource {
    @Inject GameService gameService;

    @POST
    @Path("/command")
    public GameResponse command(CommandRequest request) {
        return gameService.processCommand(request.playerId(), request.input());
    }
}

// New CLI client
public class QuestKeeperCLI {
    private final HttpClient client;

    public void run() {
        while (running) {
            String input = scanner.nextLine();
            GameResponse response = client.post("/api/game/command", input);
            display(response);
        }
    }
}
```

### 3. AI Consistency Challenges

Dynamic generation introduces consistency risks:

| Risk | Mitigation Strategy |
|------|---------------------|
| **Narrative contradictions** | Include "World Facts" in every prompt |
| **Item hallucinations** | Validate AI output against inventory |
| **Dead NPC references** | Check NPC status before displaying dialogue |
| **Rule violations** | Post-process with D&D 5e rules engine |
| **Tone drift** | Strong system prompts with examples |

**Architecture Pattern: Propose-Validate-Execute**

```java
@ApplicationScoped
public class AIResponseProcessor {

    @Inject
    RulesEngine rulesEngine;

    @Inject
    GameStateValidator validator;

    public ProcessedResponse process(AIResponse aiResponse, GameState state) {
        // 1. Narrative is always safe to display
        String narrative = aiResponse.getNarrative();

        // 2. Validate each proposed state change
        List<StateChange> approvedChanges = aiResponse.getStateUpdates()
            .stream()
            .filter(change -> rulesEngine.isValid(change, state))
            .filter(change -> validator.isConsistent(change, state))
            .toList();

        // 3. Apply only approved changes
        approvedChanges.forEach(state::apply);

        // 4. Return processed response
        return new ProcessedResponse(narrative, approvedChanges);
    }
}
```

### 4. State Serialization for AI Context

You'll need to serialize `GameState` to prompt-friendly format:

```java
@ApplicationScoped
public class PromptContextBuilder {

    public String buildContext(GameState state, Character player, Campaign campaign) {
        return """
            ## Current Game State

            ### Player: %s
            - Class: %s | Level: %d
            - HP: %d/%d | AC: %d
            - Inventory: %s

            ### Location: %s
            %s

            ### NPCs Present
            %s

            ### Active Flags
            %s

            ### Recent History
            %s
            """.formatted(
                player.getName(),
                player.getCharacterClass(),
                player.getLevel(),
                player.getCurrentHitPoints(),
                player.getMaxHitPoints(),
                player.getArmorClass(),
                formatInventory(player.getInventory()),
                state.getCurrentLocation().getName(),
                state.getCurrentLocation().getDescription(),
                formatNPCs(state),
                formatFlags(state),
                formatHistory(state)
            );
    }
}
```

### 5. Cost Management

AI API calls cost money. Strategies:

| Strategy | Implementation |
|----------|----------------|
| **Caching** | Cache location descriptions, NPC greetings |
| **Tiered generation** | Use cheaper models for simple content |
| **Template fallbacks** | Use YAML templates when AI unavailable |
| **Token budgets** | Limit context size per request |
| **Local models** | Ollama for development, cloud for production |

```java
@ApplicationScoped
public class CachedDMService {

    @Inject
    @CacheName("dm-responses")
    Cache cache;

    @Inject
    DungeonMasterAI dm;

    public Uni<String> getLocationDescription(String locationId, String tone) {
        String cacheKey = locationId + ":" + tone;

        return cache.getAsync(cacheKey, key ->
            dm.generateLocationDescription(locationId, tone)
        );
    }
}
```

### 6. Testing AI Components

AI responses are non-deterministic. Testing strategies:

```java
@QuarkusTest
public class AIGameEngineTest {

    @InjectMock
    DungeonMasterAI mockDM;

    @Inject
    AIGameEngine engine;

    @Test
    void processesValidAIResponse() {
        // Mock deterministic response for testing
        when(mockDM.processPlayerAction(any(), any(), any(), any(), any(), any()))
            .thenReturn(new DMResponse(
                "You enter the tavern...",
                List.of(new StateChange("SET_FLAG", "entered_tavern", true)),
                List.of("Talk to barkeep", "Look around")
            ));

        GameResponse response = engine.processAction("player1", "enter tavern").await().indefinitely();

        assertTrue(response.narrative().contains("tavern"));
        assertTrue(response.stateChanges().stream()
            .anyMatch(c -> c.flag().equals("entered_tavern")));
    }

    @Test
    void rejectsInvalidStateChanges() {
        // AI tries to give player an item that doesn't exist
        when(mockDM.processPlayerAction(any(), any(), any(), any(), any(), any()))
            .thenReturn(new DMResponse(
                "You find a magic sword!",
                List.of(new StateChange("ADD_ITEM", "nonexistent_sword", 1)),
                List.of()
            ));

        GameResponse response = engine.processAction("player1", "search room").await().indefinitely();

        // Narrative displays but invalid state change is rejected
        assertTrue(response.narrative().contains("sword"));
        assertTrue(response.stateChanges().isEmpty());
    }
}
```

---

## Migration Strategy

### Phase 1: Foundation (2-4 weeks)

**Goal:** Get QuestKeeper running on Quarkus without AI, preserving current functionality.

1. **Create Quarkus project** with extensions:
   - `resteasy-reactive-jackson`
   - `hibernate-orm-panache`
   - `jdbc-postgresql`
   - `smallrye-health`

2. **Port core domain classes** (no changes needed):
   - `Character`, `NPC`, `Monster`, `Combatant`
   - `Item`, `Weapon`, `Armor`, `MagicItem`, all effects
   - `Dice`, `CombatSystem`, `CombatResult`
   - `Location`, `Trial`, `MiniGame`

3. **Create service layer:**
   - `GameService` - orchestration (extracted from GameEngine)
   - `CombatService` - combat management
   - `DialogueService` - NPC conversations
   - `CampaignService` - campaign loading (from YAML)

4. **Add REST endpoints:**
   ```
   POST /api/game/start          - Start new game
   POST /api/game/command        - Process player command
   GET  /api/game/state          - Get current state
   POST /api/game/save           - Save game
   POST /api/game/load           - Load game
   ```

5. **Add database persistence:**
   - `PlayerSession` entity (replaces file-based saves)
   - `GameStateEntity` with JSONB columns

6. **Build new CLI client** that calls REST API

**Deliverable:** QuestKeeper works identically but runs on Quarkus with REST API.

### Phase 2: AI Integration (4-6 weeks)

**Goal:** Add AI-powered enhancements to existing content.

1. **Add LangChain4j extension:**
   ```xml
   <dependency>
       <groupId>io.quarkiverse.langchain4j</groupId>
       <artifactId>quarkus-langchain4j-anthropic</artifactId>
   </dependency>
   ```

2. **Create AI services:**
   - `NarrativeEnhancer` - Enhance YAML descriptions with AI flavor
   - `DialogueGenerator` - Dynamic NPC responses for unknown topics
   - `CombatNarrator` - Dramatic combat descriptions

3. **Implement streaming responses:**
   ```
   GET /api/game/narrate (SSE) - Stream narrative text
   ```

4. **Add AI fallbacks:**
   - If AI unavailable, fall back to YAML content
   - Cache AI-generated content for reuse

**Deliverable:** Existing campaigns enhanced with AI narration.

### Phase 3: Dynamic Generation (6-8 weeks)

**Goal:** AI generates new content on-the-fly.

1. **World Generation AI:**
   - Generate new locations from seeds
   - Create NPCs with personalities and dialogue
   - Generate monsters appropriate to location

2. **Quest Generation:**
   - Create procedural quests based on world state
   - Generate mini-games/trials dynamically
   - Link quests to existing and generated content

3. **Consistency Engine:**
   - Track "World Facts" database
   - Validate all AI output
   - Implement correction loops

4. **Template System:**
   - YAML templates become "seeds" for generation
   - AI expands seeds into full content
   - Generated content saved to database

**Deliverable:** Players can request "generate me a mystery campaign" and get unique content.

### Phase 4: Production & Scale (4-6 weeks)

**Goal:** Production-ready deployment.

1. **Multi-user support:**
   - User authentication (OIDC/JWT)
   - Session management
   - Concurrent game sessions

2. **Observability:**
   - Prometheus metrics
   - Distributed tracing
   - AI usage monitoring

3. **Deployment:**
   - Native compilation
   - Kubernetes manifests
   - CI/CD pipeline

4. **Cost optimization:**
   - Response caching
   - Model selection by task
   - Usage quotas

---

## Architectural Vision: AI-Powered QuestKeeper

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           QUARKUS APPLICATION                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────────────┐│
│  │ REST API     │   │ SSE Streams  │   │ WebSocket (future multiplayer)││
│  │ /api/game/*  │   │ /api/stream/*│   │ /ws/game                     ││
│  └──────┬───────┘   └──────┬───────┘   └──────────────┬───────────────┘│
│         │                  │                          │                 │
│         └──────────────────┼──────────────────────────┘                 │
│                            │                                            │
│  ┌─────────────────────────▼─────────────────────────────────────────┐ │
│  │                      GAME SERVICE LAYER                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐ │ │
│  │  │ GameService │  │CombatService│  │DialogueSvc  │  │ TrialSvc  │ │ │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘ │ │
│  └─────────┼────────────────┼────────────────┼───────────────┼───────┘ │
│            │                │                │               │         │
│  ┌─────────▼────────────────▼────────────────▼───────────────▼───────┐ │
│  │                       AI INTEGRATION LAYER                         │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────────┐ │ │
│  │  │ DungeonMasterAI │  │ WorldGenerator  │  │ ConsistencyEngine  │ │ │
│  │  │ (LangChain4j)   │  │ (LangChain4j)   │  │ (Validator)        │ │ │
│  │  └────────┬────────┘  └────────┬────────┘  └─────────┬──────────┘ │ │
│  └───────────┼────────────────────┼─────────────────────┼────────────┘ │
│              │                    │                     │              │
│  ┌───────────▼────────────────────▼─────────────────────▼────────────┐ │
│  │                        DOMAIN LAYER                                │ │
│  │  ┌──────────┐ ┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐  │ │
│  │  │Character │ │Combat  │ │Inventory│ │Campaign │ │ D&D 5e Rules │  │ │
│  │  │ + NPC    │ │System  │ │+ Items  │ │+ Trials │ │    Engine    │  │ │
│  │  └──────────┘ └────────┘ └─────────┘ └─────────┘ └──────────────┘  │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                   │                                    │
│  ┌────────────────────────────────▼────────────────────────────────┐  │
│  │                      PERSISTENCE LAYER                           │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │  │
│  │  │ PostgreSQL  │  │   Redis     │  │ YAML Templates          │  │  │
│  │  │ (Panache)   │  │   (Cache)   │  │ (Campaign Seeds)        │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
       ┌────▼────┐            ┌─────▼─────┐           ┌─────▼─────┐
       │   CLI   │            │    Web    │           │  Mobile   │
       │ Client  │            │  Client   │           │   App     │
       └─────────┘            └───────────┘           └───────────┘
```

---

## AI-Powered Features Roadmap

### "Never the Same Twice" Features

| Feature | How It Works |
|---------|--------------|
| **Dynamic Descriptions** | Same location has different flavor text each visit based on time, weather, player history |
| **Evolving NPCs** | NPCs remember past interactions, change attitudes, have their own goals |
| **Procedural Quests** | Generate quests based on player actions and world state |
| **Adaptive Difficulty** | AI adjusts encounter difficulty based on player performance |
| **Emergent Storylines** | AI creates plot threads from player actions |
| **Unique Items** | Generate custom magic items with balanced effects |
| **Living World** | Events happen off-screen, world changes between sessions |

### Generation Modes

```java
public enum GenerationMode {
    TEMPLATE_ONLY,      // Pure YAML, no AI (offline mode)
    AI_ENHANCED,        // YAML + AI flavor (default)
    FULLY_DYNAMIC,      // AI generates everything from seeds
    HYBRID              // Mix of pre-written and generated content
}
```

### Example: Dynamic Campaign Start

```java
@RegisterAiService
public interface CampaignGenerator {

    @SystemMessage("""
        You are a D&D 5e campaign designer. Generate unique, engaging campaigns
        that feel hand-crafted but are procedurally different each time.

        Always include:
        - A compelling hook
        - 3-5 initial locations
        - Key NPCs with distinct personalities
        - A central mystery or conflict
        - Multiple possible paths to resolution
        """)
    GeneratedCampaign generateCampaign(
        @UserMessage String playerPreferences,
        @V("tone") String tone,           // "comedic", "horror", "epic"
        @V("theme") String theme,         // "mystery", "heist", "exploration"
        @V("difficulty") String difficulty // "beginner", "intermediate", "advanced"
    );
}

// Usage
GeneratedCampaign campaign = generator.generateCampaign(
    "I want a mystery with pirates and ancient ruins",
    "dark adventure",
    "mystery",
    "intermediate"
);
```

---

## Summary: Benefits vs Challenges

### Benefits

| Benefit | Impact | Priority |
|---------|--------|----------|
| **LangChain4j integration** | Enables entire AI vision | Critical |
| **REST API** | Multi-client support | High |
| **Streaming (SSE)** | Real-time AI narration | High |
| **Database persistence** | Store generated content | High |
| **Reactive programming** | Non-blocking AI calls | Medium |
| **Native compilation** | Fast serverless deployment | Medium |
| **Dev Services** | Easy local development | Medium |
| **Cloud-native** | Scalable deployment | Low (for now) |

### Challenges

| Challenge | Difficulty | Mitigation |
|-----------|------------|------------|
| **Extract game logic from CLI** | Medium | Phased approach, keep tests passing |
| **Learn CDI/Mutiny** | Medium | Spring compatibility layer available |
| **AI consistency** | High | Validation layer, fact database |
| **Cost management** | Medium | Caching, model selection, quotas |
| **Testing AI components** | Medium | Mock AI services, deterministic tests |
| **Maintaining D&D accuracy** | Low | Existing rules engine validates all output |

### Verdict

**The migration is worth it.** QuestKeeper's current architecture is excellent for a CLI game but cannot support the AI-powered, multi-client, "never the same twice" vision without significant changes. Quarkus provides:

1. The best Java LLM integration available (LangChain4j)
2. Modern reactive architecture for streaming AI
3. Production-ready features (health, metrics, security)
4. Cloud-native deployment options

The existing domain model (Character, Combat, Items, D&D rules) ports directly with no changes. The main work is extracting the game orchestration from the CLI and building the AI integration layer.

**Start with Phase 1** - get the game running on Quarkus with REST API while preserving all current functionality. This validates the approach with minimal risk before adding AI complexity.
