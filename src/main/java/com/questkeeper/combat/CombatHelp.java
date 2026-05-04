package com.questkeeper.combat;

import com.questkeeper.character.Character;
import com.questkeeper.character.features.BarbarianFeatures;
import com.questkeeper.character.features.BardFeatures;
import com.questkeeper.character.features.ClericFeatures;
import com.questkeeper.character.features.DruidFeatures;
import com.questkeeper.character.features.FighterFeatures;
import com.questkeeper.character.features.MonkFeatures;
import com.questkeeper.character.features.PaladinFeatures;
import com.questkeeper.character.features.RogueFeatures;
import com.questkeeper.character.features.SorcererFeatures;

/**
 * Builds the in-combat help text dynamically based on the player's class
 * features. Pure read; no state mutation. Extracted from
 * {@link CombatSystem#playerTurn} so the combat dispatcher stays focused
 * on action handling rather than on UI strings.
 */
final class CombatHelp {

    private CombatHelp() {}

    static String forPlayer(Combatant player) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMBAT ACTIONS ===\n");
        sb.append("  attack <target>  - make a weapon attack\n");
        sb.append("  flee             - try to escape (DEX check)\n");
        sb.append("  cast <spell>     - cast a spell (uses a slot)\n");
        sb.append("  help             - this menu\n");

        if (!(player instanceof Character ch)) return sb.toString();

        StringBuilder cls = new StringBuilder();
        if (ch.getFeature(FighterFeatures.SECOND_WIND_ID).isPresent()) {
            cls.append("  secondwind       - Fighter: bonus-action self-heal\n");
        }
        if (ch.getFeature(FighterFeatures.ACTION_SURGE_ID).isPresent()) {
            cls.append("  actionsurge      - Fighter: another action this turn\n");
        }
        if (ch.getFeature(PaladinFeatures.DIVINE_SMITE_ID).isPresent()) {
            cls.append("  smite            - Paladin: prime Divine Smite\n");
        }
        if (ch.getFeature(PaladinFeatures.LAY_ON_HANDS_ID).isPresent()) {
            cls.append("  layonhands       - Paladin: heal from your divine pool\n");
        }
        if (ch.getFeature(PaladinFeatures.SACRED_WEAPON_ID).isPresent()) {
            cls.append("  sacredweapon     - Paladin: Channel Divinity buff\n");
        }
        if (ch.getFeature(PaladinFeatures.TURN_THE_UNHOLY_ID).isPresent()
            || ch.getFeature(ClericFeatures.TURN_UNDEAD_ID).isPresent()) {
            cls.append("  turn             - frighten undead/fiends (Channel Divinity)\n");
        }
        if (ch.getFeature(BarbarianFeatures.RAGE_ID).isPresent()) {
            cls.append("  rage             - Barbarian: enter rage\n");
            cls.append("  reckless         - Barbarian: advantage on attacks (and on you)\n");
        }
        if (ch.getFeature(BarbarianFeatures.FRENZY_ID).isPresent()) {
            cls.append("  frenzy           - Berserker: bonus melee attack while raging\n");
        }
        if (ch.getFeature(MonkFeatures.FLURRY_OF_BLOWS_ID).isPresent()) {
            cls.append("  flurry           - Monk: 2 bonus attacks (1 ki)\n");
            cls.append("  patient          - Monk: defensive (1 ki)\n");
            cls.append("  step             - Monk: dash/disengage (1 ki)\n");
        }
        if (ch.getFeature(MonkFeatures.STUNNING_STRIKE_ID).isPresent()) {
            cls.append("  stun             - Monk L5: prime Stunning Strike\n");
        }
        if (ch.getFeature(RogueFeatures.CUNNING_ACTION_ID).isPresent()) {
            cls.append("  dash / disengage / hide - Rogue: Cunning Action\n");
        }
        if (ch.getFeature(BardFeatures.BARDIC_INSPIRATION_ID).isPresent()) {
            cls.append("  inspire          - Bard: grant a Bardic Inspiration die\n");
        }
        if (ch.getFeature(DruidFeatures.WILD_SHAPE_ID).isPresent()) {
            cls.append("  wildshape        - Druid: transform into a beast\n");
        }
        if (ch.getFeature(SorcererFeatures.FONT_OF_MAGIC_ID).isPresent()) {
            cls.append("  fontofmagic      - Sorcerer: show sorcery points\n");
        }
        if (ch.getFeature(ClericFeatures.CLERIC_CHANNEL_DIVINITY_ID).isPresent()) {
            cls.append("  turn             - Cleric: Channel Divinity vs undead\n");
        }

        if (cls.length() > 0) {
            sb.append("=== CLASS ACTIONS ===\n").append(cls);
        }
        return sb.toString();
    }
}
