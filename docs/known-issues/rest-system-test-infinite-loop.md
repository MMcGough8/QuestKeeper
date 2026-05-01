# Known issue: `RestSystemTest.restoresHalfHitDice` infinite loop

- **Failing test:** `com.questkeeper.core.RestSystemTest$LongRestTests.restoresHalfHitDice`
- **Test location:** `src/test/java/com/questkeeper/core/RestSystemTest.java:201-216`
- **Root cause location:** `src/main/java/com/questkeeper/character/Character.java:417-431` (`useHitDie()`)

## Symptom

`mvn test` hangs indefinitely. `jstack` shows the main thread RUNNABLE in
`restoresHalfHitDice`, ~98% CPU. Observed 20 minutes (~1195s) of CPU spinning
before being killed.

## Why it loops

```java
fighter.addExperience(2700);   // levels to 4, heals to full
fighter.takeDamage(20);
while (fighter.getAvailableHitDice() > 0) {
    fighter.useHitDie();   // short-circuits without decrementing at full HP
}
```

`useHitDie()` returns 0 without decrementing `availableHitDice` when
`currentHitPoints >= maxHitPoints`. After 2-3 dice are spent the fighter
heals to full, and every subsequent call returns 0 without decrementing.
The loop never terminates. This is a test bug, not a production bug.

## Proposed one-line fix

```java
fighter.takeDamage(100);
fighter.useHitDie();
```

## Discovery context

Found 2026-04-30 while running baseline `mvn test` after the
`Commandparser.java` -> `CommandParser.java` rename, preparing for synonym
expansion work. Unrelated to that task.
