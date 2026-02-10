package com.questkeeper.character.features;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for Rogue class features.
 *
 * Rogue features by level:
 * - Level 1: Expertise, Sneak Attack (1d6), Thieves' Cant
 * - Level 2: Cunning Action
 * - Level 3: Roguish Archetype (Thief), Sneak Attack (2d6)
 *
 * @author Marc McGough
 * @version 1.0
 */
public final class RogueFeatures {

    // Feature IDs
    public static final String SNEAK_ATTACK_ID = "sneak_attack";
    public static final String EXPERTISE_ID = "expertise";
    public static final String CUNNING_ACTION_ID = "cunning_action";
    public static final String THIEVES_CANT_ID = "thieves_cant";
    public static final String FAST_HANDS_ID = "fast_hands";
    public static final String SECOND_STORY_WORK_ID = "second_story_work";

    private RogueFeatures() {
        // Utility class
    }

    /**
     * Creates all Rogue features appropriate for the given level.
     *
     * @param level the character's Rogue level
     * @param expertiseSkills the skills chosen for Expertise (null if not yet chosen)
     * @return list of features available at this level
     */
    public static List<ClassFeature> createFeaturesForLevel(int level, Set<Skill> expertiseSkills) {
        List<ClassFeature> features = new ArrayList<>();

        // Level 1: Expertise, Sneak Attack, Thieves' Cant
        if (level >= 1) {
            features.add(createSneakAttack(level));
            if (expertiseSkills != null && !expertiseSkills.isEmpty()) {
                features.add(createExpertise(expertiseSkills));
            }
            features.add(createThievesCant());
        }

        // Level 2: Cunning Action
        if (level >= 2) {
            features.add(createCunningAction());
        }

        // Level 3: Thief Archetype features
        if (level >= 3) {
            features.add(createFastHands());
            features.add(createSecondStoryWork());
        }

        return features;
    }

    /**
     * Creates the Sneak Attack feature.
     * Deals extra damage when you have advantage or an ally is adjacent to the target.
     */
    public static SneakAttack createSneakAttack(int rogueLevel) {
        return new SneakAttack(rogueLevel);
    }

    /**
     * Creates the Expertise feature for the given skills.
     */
    public static Expertise createExpertise(Set<Skill> skills) {
        return new Expertise(skills);
    }

    /**
     * Creates the Thieves' Cant feature.
     */
    public static ThievesCant createThievesCant() {
        return new ThievesCant();
    }

    /**
     * Creates the Cunning Action feature.
     */
    public static CunningAction createCunningAction() {
        return new CunningAction();
    }

    /**
     * Creates the Fast Hands feature (Thief archetype).
     */
    public static FastHands createFastHands() {
        return new FastHands();
    }

    /**
     * Creates the Second-Story Work feature (Thief archetype).
     */
    public static SecondStoryWork createSecondStoryWork() {
        return new SecondStoryWork();
    }

    // ==========================================
    // Concrete Feature Classes
    // ==========================================

    /**
     * Sneak Attack - Extra damage when you have advantage or ally is adjacent.
     * Damage scales with Rogue level: (level + 1) / 2 d6.
     */
    public static class SneakAttack extends PassiveFeature {

        private int rogueLevel;

        public SneakAttack(int rogueLevel) {
            super(
                SNEAK_ATTACK_ID,
                "Sneak Attack",
                "Once per turn, you can deal extra damage to one creature you hit with an attack " +
                    "if you have advantage on the attack roll. The attack must use a finesse or " +
                    "ranged weapon. You don't need advantage if another enemy of the target is " +
                    "within 5 feet of it and that enemy isn't incapacitated.",
                1  // Level required
            );
            this.rogueLevel = rogueLevel;
        }

        /**
         * Gets the number of d6s for Sneak Attack damage at current level.
         * Level 1-2: 1d6, Level 3-4: 2d6, Level 5-6: 3d6, etc.
         */
        public int getSneakAttackDice() {
            return (rogueLevel + 1) / 2;
        }

        /**
         * Gets the Sneak Attack damage dice notation (e.g., "2d6").
         */
        public String getSneakAttackDamage() {
            return getSneakAttackDice() + "d6";
        }

        /**
         * Updates the Rogue level (called when leveling up).
         */
        public void setRogueLevel(int level) {
            this.rogueLevel = level;
        }

        public int getRogueLevel() {
            return rogueLevel;
        }
    }

    /**
     * Expertise - Double proficiency bonus on chosen skills.
     */
    public static class Expertise extends PassiveFeature {

        private final Set<Skill> expertiseSkills;

        public Expertise(Set<Skill> skills) {
            super(
                EXPERTISE_ID,
                "Expertise",
                "Your proficiency bonus is doubled for any ability check you make that uses " +
                    "either of the chosen proficiencies.",
                1  // Level required
            );
            this.expertiseSkills = EnumSet.copyOf(skills);
        }

        /**
         * Gets the skills that have expertise.
         */
        public Set<Skill> getExpertiseSkills() {
            return Collections.unmodifiableSet(expertiseSkills);
        }

        /**
         * Checks if a skill has expertise.
         */
        public boolean hasExpertise(Skill skill) {
            return expertiseSkills.contains(skill);
        }
    }

    /**
     * Thieves' Cant - Secret language and symbols.
     */
    public static class ThievesCant extends PassiveFeature {

        public ThievesCant() {
            super(
                THIEVES_CANT_ID,
                "Thieves' Cant",
                "You have learned thieves' cant, a secret mix of dialect, jargon, and code " +
                    "that allows you to hide messages in seemingly normal conversation. " +
                    "You also understand a set of secret signs and symbols.",
                1  // Level required
            );
        }
    }

    /**
     * Cunning Action - Use bonus action to Dash, Disengage, or Hide.
     */
    public static class CunningAction extends ActivatedFeature {

        public CunningAction() {
            super(
                CUNNING_ACTION_ID,
                "Cunning Action",
                "You can take a bonus action on each of your turns in combat to take the " +
                    "Dash, Disengage, or Hide action.",
                2,  // Level required
                1,  // Max uses (resets each turn, effectively unlimited)
                ResetType.UNLIMITED,
                true,   // Available in combat
                false   // Not useful outside combat
            );
        }

        @Override
        protected String activate(Character user) {
            // This is handled specially in combat - the action choice matters
            return "Cunning Action ready. Choose: dash, disengage, or hide.";
        }

        /**
         * Performs the Dash cunning action.
         */
        public String dash(Character user) {
            return String.format("%s uses Cunning Action to Dash! Movement speed doubled this turn.",
                user.getName());
        }

        /**
         * Performs the Disengage cunning action.
         */
        public String disengage(Character user) {
            return String.format("%s uses Cunning Action to Disengage! " +
                "Movement doesn't provoke opportunity attacks this turn.", user.getName());
        }

        /**
         * Performs the Hide cunning action.
         */
        public String hide(Character user) {
            int stealthMod = user.getSkillModifier(Skill.STEALTH);
            return String.format("%s uses Cunning Action to Hide! Make a Stealth check (+%d).",
                user.getName(), stealthMod);
        }
    }

    /**
     * Fast Hands (Thief) - Use object or Sleight of Hand as bonus action.
     */
    public static class FastHands extends PassiveFeature {

        public FastHands() {
            super(
                FAST_HANDS_ID,
                "Fast Hands",
                "You can use the bonus action granted by your Cunning Action to make a " +
                    "Dexterity (Sleight of Hand) check, use your thieves' tools to disarm a " +
                    "trap or open a lock, or take the Use an Object action.",
                3  // Level required
            );
        }
    }

    /**
     * Second-Story Work (Thief) - Climb speed and better jumping.
     */
    public static class SecondStoryWork extends PassiveFeature {

        public SecondStoryWork() {
            super(
                SECOND_STORY_WORK_ID,
                "Second-Story Work",
                "You gain the ability to climb faster than normal; climbing no longer costs " +
                    "you extra movement. In addition, when you make a running jump, the " +
                    "distance you cover increases by a number of feet equal to your " +
                    "Dexterity modifier.",
                3  // Level required
            );
        }

        /**
         * Gets the bonus jump distance based on DEX modifier.
         */
        public int getBonusJumpDistance(Character character) {
            return character.getAbilityModifier(Character.Ability.DEXTERITY);
        }
    }
}
