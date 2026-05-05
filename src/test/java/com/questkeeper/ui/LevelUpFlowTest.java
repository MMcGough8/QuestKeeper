package com.questkeeper.ui;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("LevelUpFlow")
class LevelUpFlowTest {

    private static Scanner scannerOf(String script) {
        return new Scanner(script);
    }

    @Nested
    @DisplayName("Pending ASI prompt")
    class PendingAsiTests {

        @Test
        @DisplayName("Choosing 1 + STR applies +2 to STR and clears the pending ASI")
        void plusTwoFlow() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            int strBefore = fighter.getBaseAbilityScore(Ability.STRENGTH);

            String input = String.join("\n",
                "1",       // +2 to one
                "str",     // STR
                ""
            );
            LevelUpFlow.applyPendingAbilityScoreImprovements(fighter, scannerOf(input));

            assertEquals(strBefore + 2, fighter.getBaseAbilityScore(Ability.STRENGTH));
            assertEquals(0, fighter.getPendingAbilityScoreImprovements());
        }

        @Test
        @DisplayName("Choosing 2 + STR + DEX applies +1 each and clears the pending ASI")
        void plusOneOneFlow() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            fighter.setLevel(4);
            int strBefore = fighter.getBaseAbilityScore(Ability.STRENGTH);
            int dexBefore = fighter.getBaseAbilityScore(Ability.DEXTERITY);

            String input = String.join("\n",
                "2",
                "str",
                "dex",
                ""
            );
            LevelUpFlow.applyPendingAbilityScoreImprovements(fighter, scannerOf(input));

            assertEquals(strBefore + 1, fighter.getBaseAbilityScore(Ability.STRENGTH));
            assertEquals(dexBefore + 1, fighter.getBaseAbilityScore(Ability.DEXTERITY));
            assertEquals(0, fighter.getPendingAbilityScoreImprovements());
        }

        @Test
        @DisplayName("All pending ASIs get spent before the flow returns")
        void drainsAllPending() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            // Fighter 1 -> 8 crosses ASI levels 4, 6 (Fighter bonus), 8 = 3.
            fighter.setLevel(8);
            assertEquals(3, fighter.getPendingAbilityScoreImprovements(),
                "precondition: 3 ASIs after L8 (Fighter gets a bonus at L6)");

            String input = String.join("\n",
                "1", "str",
                "1", "dex",
                "1", "con",
                ""
            );
            LevelUpFlow.applyPendingAbilityScoreImprovements(fighter, scannerOf(input));

            assertEquals(0, fighter.getPendingAbilityScoreImprovements(),
                "All ASIs should be drained");
        }

        @Test
        @DisplayName("Returns immediately when there are no pending ASIs")
        void noopWhenZero() {
            Character fighter = new Character("Aelar", Race.HUMAN, CharacterClass.FIGHTER,
                14, 14, 14, 10, 10, 10);
            assertEquals(0, fighter.getPendingAbilityScoreImprovements(),
                "precondition: nothing pending");

            // Empty scanner — flow must not block trying to read.
            LevelUpFlow.applyPendingAbilityScoreImprovements(fighter, scannerOf(""));
        }
    }
}
