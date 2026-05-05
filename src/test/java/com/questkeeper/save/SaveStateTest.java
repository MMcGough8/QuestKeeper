package com.questkeeper.save;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.character.features.FightingStyle;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SaveState and CharacterData classes.
 *
 * @author Marc McGough
 */
@DisplayName("SaveState System")
class SaveStateTest {

    @TempDir
    Path tempDir;

    private Character createTestCharacter() {
        Character character = new Character("Thorin", Race.DWARF, CharacterClass.FIGHTER);
        character.setAbilityScore(Ability.STRENGTH, 16);
        character.setAbilityScore(Ability.DEXTERITY, 12);
        character.setAbilityScore(Ability.CONSTITUTION, 14);
        character.setAbilityScore(Ability.INTELLIGENCE, 10);
        character.setAbilityScore(Ability.WISDOM, 13);
        character.setAbilityScore(Ability.CHARISMA, 8);
        character.addSkillProficiency(Skill.ATHLETICS);
        character.addSkillProficiency(Skill.INTIMIDATION);
        return character;
    }

    @Nested
    @DisplayName("CharacterData")
    class CharacterDataTests {

        @Test
        @DisplayName("should serialize character identity")
        void serializesIdentity() {
            Character character = createTestCharacter();
            CharacterData data = CharacterData.fromCharacter(character);

            assertEquals("Thorin", data.getName());
            assertEquals("DWARF", data.getRace());
            assertEquals("FIGHTER", data.getCharacterClass());
        }

        @Test
        @DisplayName("should serialize ability scores")
        void serializesAbilityScores() {
            Character character = createTestCharacter();
            CharacterData data = CharacterData.fromCharacter(character);
            Character restored = data.toCharacter();

            // Check base scores are preserved
            assertEquals(16, restored.getBaseAbilityScore(Ability.STRENGTH));
            assertEquals(12, restored.getBaseAbilityScore(Ability.DEXTERITY));
            assertEquals(14, restored.getBaseAbilityScore(Ability.CONSTITUTION));
        }

        @Test
        @DisplayName("should serialize skill proficiencies")
        void serializesSkillProficiencies() {
            Character character = createTestCharacter();
            CharacterData data = CharacterData.fromCharacter(character);
            Character restored = data.toCharacter();

            assertTrue(restored.isProficientIn(Skill.ATHLETICS));
            assertTrue(restored.isProficientIn(Skill.INTIMIDATION));
            assertFalse(restored.isProficientIn(Skill.STEALTH));
        }

        @Test
        @DisplayName("should serialize combat state")
        void serializesCombatState() {
            Character character = createTestCharacter();
            character.takeDamage(5);
            character.setTemporaryHitPoints(3);
            character.setArmorBonus(6);  // Chain mail
            character.setShieldBonus(2);

            CharacterData data = CharacterData.fromCharacter(character);
            Character restored = data.toCharacter();

            assertEquals(character.getCurrentHitPoints(), restored.getCurrentHitPoints());
            assertEquals(character.getTemporaryHitPoints(), restored.getTemporaryHitPoints());
            assertEquals(6, restored.getArmorBonus());
            assertEquals(2, restored.getShieldBonus());
        }

        @Test
        @DisplayName("should serialize progression")
        void serializesProgression() {
            Character character = createTestCharacter();
            character.addExperience(500);

            CharacterData data = CharacterData.fromCharacter(character);
            Character restored = data.toCharacter();

            assertEquals(character.getLevel(), restored.getLevel());
            assertEquals(500, restored.getExperiencePoints());
        }

        @Test
        @DisplayName("should round-trip through Map conversion")
        void roundTripsMapConversion() {
            Character original = createTestCharacter();
            original.takeDamage(3);
            original.addSkillProficiency(Skill.PERCEPTION);

            CharacterData data = CharacterData.fromCharacter(original);
            var map = data.toMap();
            CharacterData restored = CharacterData.fromMap(map);
            Character finalChar = restored.toCharacter();

            assertEquals(original.getName(), finalChar.getName());
            assertEquals(original.getCurrentHitPoints(), finalChar.getCurrentHitPoints());
            assertTrue(finalChar.isProficientIn(Skill.PERCEPTION));
        }
    }

    @Nested
    @DisplayName("SaveState Core")
    class SaveStateCoreTests {

        @Test
        @DisplayName("should create from character")
        void createsFromCharacter() {
            Character character = createTestCharacter();
            SaveState save = new SaveState(character, "muddlebrook");

            assertEquals("muddlebrook", save.getCampaignId());
            assertEquals("Thorin - muddlebrook", save.getSaveName());
            assertNotNull(save.getTimestamp());
        }

        @Test
        @DisplayName("should restore character")
        void restoresCharacter() {
            Character original = createTestCharacter();
            SaveState save = new SaveState(original, "muddlebrook");
            Character restored = save.restoreCharacter();

            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getRace(), restored.getRace());
            assertEquals(original.getCharacterClass(), restored.getCharacterClass());
        }

        @Test
        @DisplayName("should throw when restoring without character")
        void throwsOnMissingCharacter() {
            SaveState save = new SaveState();
            assertThrows(IllegalStateException.class, save::restoreCharacter);
        }
    }

    @Nested
    @DisplayName("State Flags")
    class StateFlagTests {

        @Test
        @DisplayName("should set and get boolean flags")
        void handlesFlags() {
            SaveState save = new SaveState();

            save.setFlag("met_norrin", true);
            save.setFlag("trial1_complete", true);
            save.setFlag("found_secret", false);

            assertTrue(save.getFlag("met_norrin"));
            assertTrue(save.hasFlag("trial1_complete"));
            assertFalse(save.getFlag("found_secret"));
            assertFalse(save.getFlag("nonexistent"));
        }

        @Test
        @DisplayName("should set and get counters")
        void handlesCounters() {
            SaveState save = new SaveState();

            save.setCounter("tavern_visits", 3);
            save.incrementCounter("tavern_visits");
            save.incrementCounter("enemies_defeated");

            assertEquals(4, save.getCounter("tavern_visits"));
            assertEquals(1, save.getCounter("enemies_defeated"));
            assertEquals(0, save.getCounter("nonexistent"));
        }

        @Test
        @DisplayName("should set and get strings")
        void handlesStrings() {
            SaveState save = new SaveState();

            save.setString("chosen_faction", "Shardwardens");
            save.setString("nickname", "The Crusher");

            assertEquals("Shardwardens", save.getString("chosen_faction"));
            assertEquals("The Crusher", save.getString("nickname"));
            assertNull(save.getString("nonexistent"));
        }
    }

    @Nested
    @DisplayName("Location Tracking")
    class LocationTests {

        @Test
        @DisplayName("should track current location")
        void tracksCurrentLocation() {
            SaveState save = new SaveState();

            save.setCurrentLocation("drunken_dragon_inn");
            assertEquals("drunken_dragon_inn", save.getCurrentLocationId());
        }

        @Test
        @DisplayName("should track visited locations")
        void tracksVisitedLocations() {
            SaveState save = new SaveState();

            save.setCurrentLocation("drunken_dragon_inn");
            save.setCurrentLocation("town_hall");
            save.setCurrentLocation("clocktower_hill");

            assertTrue(save.hasVisited("drunken_dragon_inn"));
            assertTrue(save.hasVisited("town_hall"));
            assertTrue(save.hasVisited("clocktower_hill"));
            assertFalse(save.hasVisited("whisperwood"));
        }
    }

    @Nested
    @DisplayName("Inventory")
    class InventoryTests {

        @Test
        @DisplayName("should manage inventory items")
        void managesInventory() {
            SaveState save = new SaveState();

            save.addItem("longsword");
            save.addItem("shield");
            save.addItem("health_potion");
            save.removeItem("health_potion");

            List<String> items = save.getInventoryItems();
            assertEquals(2, items.size());
            assertTrue(items.contains("longsword"));
            assertTrue(items.contains("shield"));
        }

        @Test
        @DisplayName("should manage equipped items")
        void managesEquipment() {
            SaveState save = new SaveState();

            save.equipItem("longsword");
            save.equipItem("shield");
            save.equipItem("longsword");  // Duplicate should not add

            assertEquals(2, save.getEquippedItems().size());

            save.unequipItem("shield");
            assertEquals(1, save.getEquippedItems().size());
        }

        @Test
        @DisplayName("should manage gold")
        void managesGold() {
            SaveState save = new SaveState();

            save.addGold(100);
            assertEquals(100, save.getGold());

            assertTrue(save.spendGold(30));
            assertEquals(70, save.getGold());

            assertFalse(save.spendGold(100));  // Can't spend more than have
            assertEquals(70, save.getGold());
        }
    }

    @Nested
    @DisplayName("Play Time")
    class PlayTimeTests {

        @Test
        @DisplayName("should track play time")
        void tracksPlayTime() {
            SaveState save = new SaveState();

            save.addPlayTime(3600);  // 1 hour
            save.addPlayTime(1800);  // 30 minutes

            assertEquals(5400, save.getTotalPlayTimeSeconds());
            assertEquals("1h 30m", save.getFormattedPlayTime());
        }

        @Test
        @DisplayName("should format minutes only when under an hour")
        void formatsShortPlayTime() {
            SaveState save = new SaveState();
            save.addPlayTime(1500);  // 25 minutes

            assertEquals("25m", save.getFormattedPlayTime());
        }
    }

    @Nested
    @DisplayName("File Operations")
    class FileOperationTests {

        @Test
        @DisplayName("should save and load from file")
        void savesAndLoads() throws IOException {
            Character character = createTestCharacter();
            character.takeDamage(5);

            SaveState original = new SaveState(character, "muddlebrook");
            original.setCurrentLocation("town_hall");
            original.setFlag("met_norrin", true);
            original.setCounter("tavern_visits", 3);
            original.setString("nickname", "Crusher");
            original.addItem("longsword");
            original.addGold(50);
            original.addPlayTime(3600);

            Path savePath = tempDir.resolve("test_save.yaml");
            original.save(savePath);

            assertTrue(Files.exists(savePath));

            SaveState loaded = SaveState.load(savePath);

            assertEquals(original.getCampaignId(), loaded.getCampaignId());
            assertEquals(original.getCurrentLocationId(), loaded.getCurrentLocationId());
            assertTrue(loaded.getFlag("met_norrin"));
            assertEquals(3, loaded.getCounter("tavern_visits"));
            assertEquals("Crusher", loaded.getString("nickname"));
            assertTrue(loaded.getInventoryItems().contains("longsword"));
            assertEquals(50, loaded.getGold());
            assertEquals(3600, loaded.getTotalPlayTimeSeconds());

            // Verify character restoration
            Character restored = loaded.restoreCharacter();
            assertEquals("Thorin", restored.getName());
            assertEquals(character.getCurrentHitPoints(), restored.getCurrentHitPoints());
        }

        @Test
        @DisplayName("should increment save count on each save")
        void incrementsSaveCount() throws IOException {
            SaveState save = new SaveState(createTestCharacter(), "test");
            Path savePath = tempDir.resolve("save.yaml");

            save.save(savePath);
            assertEquals(1, save.getSaveCount());

            save.save(savePath);
            assertEquals(2, save.getSaveCount());
        }

        @Test
        @DisplayName("should produce human-readable YAML")
        void producesReadableYaml() throws IOException {
            Character character = createTestCharacter();
            SaveState save = new SaveState(character, "muddlebrook");
            save.setFlag("met_norrin", true);

            Path savePath = tempDir.resolve("readable.yaml");
            save.save(savePath);

            String content = Files.readString(savePath);

            // Should be readable YAML, not one-liner
            assertTrue(content.contains("save_name:"));
            assertTrue(content.contains("campaign_id: muddlebrook"));
            assertTrue(content.contains("met_norrin: true"));
            assertTrue(content.contains("name: Thorin"));
        }

        @Test
        @DisplayName("should create parent directories")
        void createsParentDirectories() throws IOException {
            SaveState save = new SaveState(createTestCharacter(), "test");
            Path deepPath = tempDir.resolve("saves/campaign/test.yaml");

            save.save(deepPath);

            assertTrue(Files.exists(deepPath));
        }
    }

    @Nested
    @DisplayName("Character class/race choice fields round-trip")
    class CharacterChoiceFieldsRoundTripTests {

        @Test
        @DisplayName("Fighter fighting style survives save/load")
        void fightingStyleRoundTrips() throws IOException {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER);
            fighter.setFightingStyle(FightingStyle.DEFENSE);
            assertEquals(FightingStyle.DEFENSE, fighter.getFightingStyle());

            Path savePath = tempDir.resolve("fighter.yaml");
            new SaveState(fighter, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();

            assertEquals(FightingStyle.DEFENSE, restored.getFightingStyle(),
                "Defense fighting style should survive round-trip");
        }

        @Test
        @DisplayName("Rogue expertise skills survive save/load")
        void expertiseSkillsRoundTrip() throws IOException {
            Character rogue = new Character("Lia", Race.HALFLING, CharacterClass.ROGUE);
            rogue.addSkillProficiency(Skill.STEALTH);
            rogue.addSkillProficiency(Skill.ATHLETICS);
            rogue.setExpertiseSkills(Set.of(Skill.STEALTH, Skill.ATHLETICS));

            Path savePath = tempDir.resolve("rogue.yaml");
            new SaveState(rogue, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();

            assertEquals(Set.of(Skill.STEALTH, Skill.ATHLETICS),
                restored.getExpertiseSkills(),
                "Expertise skills should survive round-trip");
        }

        @Test
        @DisplayName("Non-default saving throw proficiency survives save/load")
        void savingThrowProficienciesRoundTrip() throws IOException {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER);
            // Fighter defaults are STR/CON. Add CHARISMA as a non-default save.
            fighter.addSavingThrowProficiency(Ability.CHARISMA);

            Path savePath = tempDir.resolve("saves.yaml");
            new SaveState(fighter, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();

            assertTrue(restored.hasSavingThrowProficiency(Ability.CHARISMA),
                "Non-default CHA save proficiency should survive round-trip");
            assertTrue(restored.hasSavingThrowProficiency(Ability.STRENGTH),
                "Class default STR save should still be present");
        }

        @Test
        @DisplayName("Lay on Hands pool survives save/load")
        void layOnHandsPoolRoundTrips() throws IOException {
            Character paladin = new Character("Aelar", Race.HUMAN, CharacterClass.PALADIN);
            paladin.setLevel(2);

            com.questkeeper.character.features.PaladinFeatures.LayOnHands loh =
                (com.questkeeper.character.features.PaladinFeatures.LayOnHands)
                    paladin.getFeature(
                        com.questkeeper.character.features.PaladinFeatures.LAY_ON_HANDS_ID
                    ).orElseThrow();
            // Spend 4 HP from the pool
            loh.heal(paladin, 4);
            int spentPool = loh.getPoolRemaining();

            Path savePath = tempDir.resolve("loh.yaml");
            new SaveState(paladin, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();
            com.questkeeper.character.features.PaladinFeatures.LayOnHands restoredLoh =
                (com.questkeeper.character.features.PaladinFeatures.LayOnHands)
                    restored.getFeature(
                        com.questkeeper.character.features.PaladinFeatures.LAY_ON_HANDS_ID
                    ).orElseThrow();

            assertEquals(spentPool, restoredLoh.getPoolRemaining(),
                "Lay on Hands pool should survive round-trip, not refill");
        }

        @Test
        @DisplayName("Half-Elf bonus abilities survive save/load")
        void halfElfBonusAbilitiesRoundTrip() throws IOException {
            Character bard = new Character("Halo", Race.HALF_ELF, CharacterClass.BARD);
            bard.setHalfElfBonusAbilities(Ability.INTELLIGENCE, Ability.WISDOM);

            Path savePath = tempDir.resolve("halfelf.yaml");
            new SaveState(bard, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();

            assertEquals(Set.of(Ability.INTELLIGENCE, Ability.WISDOM),
                restored.getHalfElfBonusAbilities(),
                "Half-Elf bonus abilities should survive round-trip");
        }

        @Test
        @DisplayName("Pending ASI count survives save/load")
        void pendingAsiRoundTrip() throws IOException {
            // Lvl 4 fighter with the ASI unspent — must persist as 1.
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            assertEquals(1, fighter.getPendingAbilityScoreImprovements(),
                "precondition: Lvl 4 grants 1 ASI");

            Path savePath = tempDir.resolve("asi.yaml");
            new SaveState(fighter, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();
            assertEquals(1, restored.getPendingAbilityScoreImprovements(),
                "Unspent ASI should survive save/load");
        }

        @Test
        @DisplayName("Spent ASI does not reappear after save/load")
        void spentAsiStaysSpentAfterRoundTrip() throws IOException {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            fighter.applyAbilityScoreImprovement(Ability.STRENGTH);  // spend it
            assertEquals(0, fighter.getPendingAbilityScoreImprovements(),
                "precondition: ASI was spent");

            Path savePath = tempDir.resolve("asi-spent.yaml");
            new SaveState(fighter, "muddlebrook").save(savePath);

            Character restored = SaveState.load(savePath).restoreCharacter();
            assertEquals(0, restored.getPendingAbilityScoreImprovements(),
                "Spent ASI must not regenerate from setLevel during restore");
            assertEquals(16, restored.getBaseAbilityScore(Ability.STRENGTH),
                "Spent +2 STR (14 → 16 base) should persist");
        }
    }

    @Nested
    @DisplayName("Spellcasting round-trip")
    class SpellcastingRoundTripTests {

        @Test
        @DisplayName("Wizard known spells, cantrips, and spent slots survive save/load")
        void wizardSpellbookRoundTrip() {
            com.questkeeper.character.Character wiz = new com.questkeeper.character.Character(
                "Mira", com.questkeeper.character.Character.Race.HALF_ELF,
                com.questkeeper.character.Character.CharacterClass.WIZARD);
            wiz.setLevel(5);
            // Add a non-default spell.
            wiz.getSpellbook().addKnownSpell("fireball");
            wiz.getSpellbook().addCantrip("light");
            // Spend two L1 slots and one L3 slot.
            assertTrue(wiz.getSpellbook().getSpellSlots().expendSlot(1));
            assertTrue(wiz.getSpellbook().getSpellSlots().expendSlot(1));
            assertTrue(wiz.getSpellbook().getSpellSlots().expendSlot(3));

            int[] before = wiz.getSpellbook().getSpellSlots().getCurrentSlots();

            CharacterData data = CharacterData.fromCharacter(wiz);
            // Round-trip through map for a more realistic test.
            CharacterData restoredData = CharacterData.fromMap(data.toMap());
            com.questkeeper.character.Character restored = restoredData.toCharacter();

            int[] after = restored.getSpellbook().getSpellSlots().getCurrentSlots();
            assertArrayEquals(before, after,
                "spell slots must round-trip exactly: before=" + java.util.Arrays.toString(before)
                    + " after=" + java.util.Arrays.toString(after));
            assertTrue(restored.getSpellbook().getKnownSpellIds().contains("fireball"),
                "non-default known spell must persist");
            assertTrue(restored.getSpellbook().getKnownCantripIds().contains("light"),
                "added cantrip must persist");
        }

        @Test
        @DisplayName("non-caster Fighter has no spellbook fields written")
        void nonCasterHasNoSpellFields() {
            com.questkeeper.character.Character fighter = new com.questkeeper.character.Character(
                "Aldric", com.questkeeper.character.Character.Race.HUMAN,
                com.questkeeper.character.Character.CharacterClass.FIGHTER);
            fighter.setLevel(5);
            CharacterData data = CharacterData.fromCharacter(fighter);
            java.util.Map<String, Object> map = data.toMap();
            assertFalse(map.containsKey("known_spells"),
                "non-casters should not write known_spells: " + map.keySet());
            assertFalse(map.containsKey("spell_slots_current"));
        }
    }

    @Nested
    @DisplayName("listSaves discovery")
    class ListSavesTests {

        @Test
        @DisplayName("listSaves(Path) finds .yaml files in subdirectories")
        void findsYamlInSubdirectories() throws IOException {
            Character character = createTestCharacter();

            Path topLevel = tempDir.resolve("top.yaml");
            new SaveState(character, "muddlebrook").save(topLevel);

            Path subDir = tempDir.resolve("demo");
            Files.createDirectories(subDir);
            Path nested = subDir.resolve("01-parser-showcase.yaml");
            new SaveState(character, "muddlebrook").save(nested);

            List<SaveState.SaveInfo> saves = SaveState.listSaves(tempDir);

            assertEquals(2, saves.size(),
                "Expected to find both top-level and nested .yaml saves");
            assertTrue(saves.stream().anyMatch(s -> s.path().equals(nested)),
                "Expected the nested demo save to be discovered");
        }

        @Test
        @DisplayName("listSaves(Path) ignores non-.yaml files in subdirectories")
        void ignoresNonYamlInSubdirectories() throws IOException {
            Path subDir = tempDir.resolve("demo");
            Files.createDirectories(subDir);
            Files.writeString(subDir.resolve("README.md"), "# Demo saves");

            List<SaveState.SaveInfo> saves = SaveState.listSaves(tempDir);

            assertTrue(saves.isEmpty(),
                "README.md in a subdir should not be returned as a save");
        }
    }

    @Nested
    @DisplayName("SaveInfo")
    class SaveInfoTests {

        @Test
        @DisplayName("should extract save info without full load")
        void extractsSaveInfo() throws IOException {
            Character character = createTestCharacter();
            SaveState save = new SaveState(character, "muddlebrook");
            save.addPlayTime(7200);

            Path savePath = tempDir.resolve("info_test.yaml");
            save.save(savePath);

            SaveState.SaveInfo info = SaveState.SaveInfo.fromFile(savePath);

            assertEquals("Thorin - muddlebrook", info.saveName());
            assertEquals("muddlebrook", info.campaignId());
            assertEquals("Thorin", info.characterName());
            assertEquals(1, info.characterLevel());
            assertEquals("2h 0m", info.playTime());
            assertNotNull(info.timestamp());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty save state")
        void handlesEmptySave() throws IOException {
            SaveState save = new SaveState();
            Path savePath = tempDir.resolve("empty.yaml");

            save.save(savePath);
            SaveState loaded = SaveState.load(savePath);

            assertEquals("unknown", loaded.getCampaignId());
            assertEquals("Unnamed Save", loaded.getSaveName());
            assertTrue(loaded.getInventoryItems().isEmpty());
            assertEquals(0, loaded.getGold());
        }

        @Test
        @DisplayName("should handle character update")
        void handlesCharacterUpdate() {
            Character character = createTestCharacter();
            SaveState save = new SaveState(character, "test");

            // Modify character
            character.takeDamage(10);
            character.addExperience(100);

            // Update save
            save.updateCharacter(character);

            // Restore and verify
            Character restored = save.restoreCharacter();
            assertEquals(character.getCurrentHitPoints(), restored.getCurrentHitPoints());
        }

        @Test
        @DisplayName("should return unmodifiable collections")
        void returnsUnmodifiableCollections() {
            SaveState save = new SaveState();
            save.addItem("sword");
            save.setFlag("test", true);

            assertThrows(UnsupportedOperationException.class, () ->
                save.getInventoryItems().add("armor"));
            assertThrows(UnsupportedOperationException.class, () ->
                save.getStateFlags().put("new", true));
            assertThrows(UnsupportedOperationException.class, () ->
                save.getVisitedLocations().add("place"));
        }
    }

    @Nested
    @DisplayName("Auto-save")
    class AutoSaveTests {

        @Test
        @DisplayName("autoSave writes to saves/auto/<char>-<reason>.yaml and the file is loadable")
        void autoSaveWritesAndIsLoadable() throws IOException {
            Character cleric = new Character("AutosaveTester", Race.HUMAN, CharacterClass.CLERIC,
                10, 10, 14, 10, 16, 10);
            cleric.setLevel(2);
            com.questkeeper.campaign.Campaign campaign = com.questkeeper.campaign.Campaign.loadFromYaml(
                com.questkeeper.campaign.Campaign.listAvailable().stream()
                    .filter(c -> c.id().equalsIgnoreCase("muddlebrook"))
                    .findFirst().orElseThrow().path());
            com.questkeeper.state.GameState state = new com.questkeeper.state.GameState(cleric, campaign);

            Path path = SaveState.autoSave(state, "unit-test-" + System.nanoTime());
            try {
                assertNotNull(path, "autoSave should return a non-null path on success");
                assertTrue(Files.exists(path), "Auto-save file should exist at " + path);
                assertTrue(path.toString().contains("auto"),
                    "Auto-saves should live under saves/auto/");

                SaveState restored = SaveState.load(path);
                assertEquals("muddlebrook", restored.getCampaignId());
                Character restoredChar = restored.restoreCharacter();
                assertNotNull(restoredChar);
                assertEquals("AutosaveTester", restoredChar.getName());
                assertEquals(2, restoredChar.getLevel(),
                    "Auto-saved level should round-trip");
            } finally {
                if (path != null) Files.deleteIfExists(path);
            }
        }
    }
}
