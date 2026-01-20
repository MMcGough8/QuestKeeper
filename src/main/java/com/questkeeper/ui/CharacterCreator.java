package com.questkeeper.ui;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;
import com.questkeeper.character.Character.CharacterClass;
import com.questkeeper.character.Character.Race;
import com.questkeeper.character.Character.Skill;
import com.questkeeper.inventory.Inventory;
import com.questkeeper.inventory.Inventory.EquipmentSlot;
import com.questkeeper.inventory.StandardEquipment;

import java.util.*;

import static com.questkeeper.ui.Display.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Enhanced D&D Character Creator with multiple ability score methods.
 * 
 * Supports:
 * - Point Buy (27 points, official 5e method)
 * - Standard Array (15, 14, 13, 12, 10, 8)
 * - Roll for Stats (4d6 drop lowest)
 * - Random Everything (let fate decide)
 * 
 * @author Marc McGough
 * @version 1.0
 */
public class CharacterCreator {

    private static final Scanner scanner = new Scanner(System.in);
    private static final int POINT_BUY_TOTAL = 27;
    private static final int[] POINT_BUY_COSTS = {0, 1, 2, 3, 4, 5, 7, 9}; // Cost to reach score 8 through 15

    public static Character createCharacter() {
        clearScreen();

        printBox("QUESTKEEPER", 70, YELLOW);
        printBox("A D&D 5th Edition Adventure", 70, WHITE);
        println();
        println(bold(colorize("Welcome, adventurer. Your story begins now...", CYAN)));
        println();
        pressEnterToContinue();

        // Step 1: Name
        clearScreen();
        printBox("STEP 1: NAME YOUR HERO", 70, MAGENTA);
        String name = promptForString("What is your character's name?", "Aelar");
        if (name.isEmpty()) {
            name = "Aelar";
        }
        println(colorize("Welcome to the world, " + bold(name) + "!", GREEN));
        pressEnterToContinue();

        // Step 2: Race
        clearScreen();
        printBox("STEP 2: CHOOSE YOUR RACE", 70, MAGENTA);
        println(bold("Your ancestry shapes your abilities and place in the world.\n"));
        for (int i = 0; i < Race.values().length; i++) {
            Race r = Race.values()[i];
            String bonus = getRaceBonusString(r);
            println(String.format("%s) %s — %s, %d ft speed",
                    colorize(String.valueOf(i + 1), YELLOW),
                    bold(r.getDisplayName()),
                    bonus,
                    r.getSpeed()));
        }
        println();
        Race race = promptForEnum(Race.values(), "Select your race (number): ");
        printBox("You are a proud " + race.getDisplayName() + "!", 70, GREEN);
        pressEnterToContinue();

        // Step 3: Class
        clearScreen();
        printBox("STEP 3: CHOOSE YOUR CLASS", 70, MAGENTA);
        println(bold("Your class defines your path and power.\n"));
        for (int i = 0; i < CharacterClass.values().length; i++) {
            CharacterClass cc = CharacterClass.values()[i];
            println(String.format("%s) %s — Hit Die: d%d",
                    colorize(String.valueOf(i + 1), YELLOW),
                    bold(cc.getDisplayName()),
                    cc.getHitDie()));
        }
        println();
        CharacterClass characterClass = promptForEnum(CharacterClass.values(), "Select your class (number): ");
        printBox("You are now a Level 1 " + characterClass.getDisplayName() + "!", 70, GREEN);
        pressEnterToContinue();

        // Step 4: Ability Score Method Selection
        clearScreen();
        printBox("STEP 4: DETERMINE ABILITY SCORES", 70, MAGENTA);
        println(bold("Choose how to generate your six ability scores:\n"));
        println("1) " + bold("Point Buy") + "         — Spend 27 points (official 5e method)");
        println("2) " + bold("Standard Array") + "   — Assign 15, 14, 13, 12, 10, 8");
        println("3) " + bold("Roll for Stats") + "   — 4d6 drop lowest ×6 (classic randomness)");
        println("4) " + bold("Random Everything") + " — Let fate decide race, class, and stats too!");
        println();

        int method = promptForInt("Choose method (1-4): ", 1, 4);

        // Handle random everything - overrides previous choices
        if (method == 4) {
            Object[] randomResult = randomFullCharacter(name);
            name = (String) randomResult[0];
            race = (Race) randomResult[1];
            characterClass = (CharacterClass) randomResult[2];
            @SuppressWarnings("unchecked")
            Map<Ability, Integer> randomScores = (Map<Ability, Integer>) randomResult[3];
            
            // Create character with random choices
            Character character = new Character(name, race, characterClass);
            for (Map.Entry<Ability, Integer> entry : randomScores.entrySet()) {
                character.setAbilityScore(entry.getKey(), entry.getValue());
            }
            
            // Random skills
            randomSkillProficiencies(character);

            // Random equipment (option A)
            addEquipmentOptionA(character, characterClass);
            character.getInventory().addGold(10);
            println(colorize("\n✓ You receive standard equipment (Option A)!", GREEN));

            printBox("Character creation complete!", 70, GREEN);
            pressEnterToContinue();
            showFinalCharacterSheet(character);
            return character;
        }

        // For other methods, collect base scores first
        Map<Ability, Integer> baseScores = new EnumMap<>(Ability.class);
        
        switch (method) {
            case 1 -> pointBuySystem(race, baseScores);
            case 2 -> standardArrayAssignment(race, baseScores);
            case 3 -> rollForStats(race, baseScores);
        }

        Character character = new Character(name, race, characterClass);
        
        for (Map.Entry<Ability, Integer> entry : baseScores.entrySet()) {
            character.setAbilityScore(entry.getKey(), entry.getValue());
        }

        printBox("Ability scores locked in!", 70, GREEN);
        pressEnterToContinue();

        // Step 5: Skill Proficiencies (Step 5 is racial bonuses - automatic)
        clearScreen();
        printBox("STEP 5: CHOOSE SKILL PROFICIENCIES", 70, MAGENTA);
        selectSkillProficiencies(character);
        pressEnterToContinue();

        // Step 6: Starting Equipment
        clearScreen();
        printBox("STEP 6: STARTING EQUIPMENT", 70, MAGENTA);
        selectStartingEquipment(character);
        pressEnterToContinue();

        // Final Character Sheet
        showFinalCharacterSheet(character);

        return character;
    }

    private static String getRaceBonusString(Race race) {
        StringBuilder bonus = new StringBuilder();
        
        // Human special case - +1 to all
        if (race == Race.HUMAN) {
            return "+1 to all abilities";
        }
        
        // Check each ability for bonuses
        for (Ability a : Ability.values()) {
            int b = race.getAbilityBonus(a);
            if (b > 0) {
                if (bonus.length() > 0) {
                    bonus.append(", ");
                }
                bonus.append("+").append(b).append(" ").append(a.getAbbreviation());
            }
        }
        
        return bonus.toString();
    }

    private static void pointBuySystem(Race race, Map<Ability, Integer> scores) {
        // Initialize all scores to 8
        for (Ability a : Ability.values()) {
            scores.put(a, 8);
        }
        
        int pointsLeft = POINT_BUY_TOTAL;

        while (pointsLeft > 0) {
            clearScreen();
            printBox("POINT BUY — " + pointsLeft + " points remaining", 70, YELLOW);
            println("Costs: 8→9 (1pt), 9→10 (1pt), ... 13→14 (2pt), 14→15 (2pt)\n");
            println(bold("Current Scores (before racial bonuses):\n"));

            for (Ability a : Ability.values()) {
                int score = scores.get(a);
                int costToNext = score < 15 ? getPointCost(score + 1) : 0;
                int racialBonus = getRacialBonus(race, a);
                String racial = racialBonus > 0 ? colorize(" +" + racialBonus, GREEN) : "";
                String costStr = costToNext > 0 && costToNext <= pointsLeft 
                        ? colorize(" (next: " + costToNext + "pt)", MAGENTA) 
                        : "";
                        
                println(String.format("  %s: %2d%s%s",
                        padRight(a.getFullName(), 12),
                        score,
                        racial,
                        costStr));
            }
            println();
            println(colorize("Enter 'done' to finish early, or select an ability to increase.", WHITE));
            println();

            String input = promptForString("Increase which ability?", "").toLowerCase();
            
            if (input.equals("done")) {
                break;
            }

            Ability ability = parseAbility(input);
            if (ability == null) {
                println(colorize("Invalid ability. Try again.", RED));
                pressEnterToContinue();
                continue;
            }

            int current = scores.get(ability);

            if (current >= 15) {
                println(colorize("Cannot exceed 15 before racial bonuses!", RED));
                pressEnterToContinue();
                continue;
            }

            int cost = getPointCost(current + 1);
            if (cost > pointsLeft) {
                println(colorize("Not enough points! Need " + cost + ", have " + pointsLeft, RED));
                pressEnterToContinue();
                continue;
            }

            scores.put(ability, current + 1);
            pointsLeft -= cost;
            println(colorize("✓ " + ability.getFullName() + " → " + (current + 1) + " (-" + cost + " pts)", GREEN));
            pressEnterToContinue();
        }
    }

    /**
     * Standard array assignment - assign 15, 14, 13, 12, 10, 8 to abilities.
     */
    private static void standardArrayAssignment(Race race, Map<Ability, Integer> scores) {
        clearScreen();
        printBox("STANDARD ARRAY", 70, YELLOW);
        println("Assign the values: " + bold("15, 14, 13, 12, 10, 8") + "\n");
        pressEnterToContinue();

        int[] array = {15, 14, 13, 12, 10, 8};
        Set<Ability> assigned = EnumSet.noneOf(Ability.class);

        for (int value : array) {
            clearScreen();
            printBox("Assign " + bold(String.valueOf(value)) + " to which ability?", 70, CYAN);
            println("Current assignments:\n");

            for (Ability a : Ability.values()) {
                int curr = scores.getOrDefault(a, 0);
                int racialBonus = getRacialBonus(race, a);
                String racial = racialBonus > 0 ? colorize(" +" + racialBonus, GREEN) : "";
                String status = assigned.contains(a) ? colorize(" ✓", GREEN) : "";
                
                println(String.format("  %d) %s: %s%s%s",
                        a.ordinal() + 1,
                        padRight(a.getFullName(), 12),
                        curr > 0 ? String.valueOf(curr) : "--",
                        racial,
                        status));
            }
            println();

            Ability ability;
            do {
                ability = promptForEnum(Ability.values(), "Choose ability (number): ");
                if (assigned.contains(ability)) {
                    println(colorize("Already assigned! Choose another.", RED));
                }
            } while (assigned.contains(ability));

            scores.put(ability, value);
            assigned.add(ability);
            println(colorize("✓ Assigned " + value + " to " + ability.getFullName(), GREEN));
            pressEnterToContinue();
        }
    }

    /**
     * Roll for stats - 4d6 drop lowest, six times.
     */
    private static void rollForStats(Race race, Map<Ability, Integer> scores) {
        clearScreen();
        printBox("ROLLING FOR STATS — 4d6 drop lowest", 70, YELLOW);
        println("Rolling six times...\n");
        pressEnterToContinue();

        List<Integer> rolls = new ArrayList<>();
        Random rand = new Random();

        for (int i = 1; i <= 6; i++) {
            int[] dice = new int[4];
            for (int j = 0; j < 4; j++) {
                dice[j] = rand.nextInt(6) + 1;
            }
            Arrays.sort(dice);
            int dropped = dice[0];
            int total = dice[1] + dice[2] + dice[3];

            rolls.add(total);
            println(String.format("Roll %d: [%d, %d, %d, %d] → drop %d → %s",
                    i, dice[0], dice[1], dice[2], dice[3], dropped,
                    colorize(String.valueOf(total), GREEN)));
            sleep(800);
        }

        rolls.sort(Collections.reverseOrder());
        println("\nYour final rolled stats: " + bold(rolls.toString()));
        pressEnterToContinue();

        Set<Ability> assigned = EnumSet.noneOf(Ability.class);

        for (int value : rolls) {
            clearScreen();
            printBox("Assign " + bold(String.valueOf(value)) + " to which ability?", 70, CYAN);
            println("Current assignments:\n");

            for (Ability a : Ability.values()) {
                int curr = scores.getOrDefault(a, 0);
                int racialBonus = getRacialBonus(race, a);
                String racial = racialBonus > 0 ? colorize(" +" + racialBonus, GREEN) : "";
                String status = assigned.contains(a) ? colorize(" ✓", GREEN) : "";
                
                println(String.format("  %d) %s: %s%s%s",
                        a.ordinal() + 1,
                        padRight(a.getFullName(), 12),
                        curr > 0 ? String.valueOf(curr) : "--",
                        racial,
                        status));
            }
            println();

            Ability ability;
            do {
                ability = promptForEnum(Ability.values(), "Choose ability (number): ");
                if (assigned.contains(ability)) {
                    println(colorize("Already assigned! Choose another.", RED));
                }
            } while (assigned.contains(ability));

            scores.put(ability, value);
            assigned.add(ability);
            println(colorize("✓ Assigned " + value + " to " + ability.getFullName(), GREEN));
            pressEnterToContinue();
        }
    }

    /**
     * Randomizes everything - race, class, and stats.
     * Returns an array: [name, race, characterClass, scores map]
     */
    private static Object[] randomFullCharacter(String name) {
        clearScreen();
        printBox("FATE TAKES THE REINS...", 70, RED);
        println("Randomizing everything except your name...\n");
        sleep(1500);

        Random rand = new Random();

        Race[] races = Race.values();
        Race randomRace = races[rand.nextInt(races.length)];
        println("Race: " + bold(randomRace.getDisplayName()));
        sleep(1000);

        CharacterClass[] classes = CharacterClass.values();
        CharacterClass randomClass = classes[rand.nextInt(classes.length)];
        println("Class: " + bold(randomClass.getDisplayName()));
        sleep(1000);

        // Roll stats
        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int[] d = new int[4];
            for (int j = 0; j < 4; j++) {
                d[j] = rand.nextInt(6) + 1;
            }
            Arrays.sort(d);
            rolls.add(d[1] + d[2] + d[3]);
        }
        rolls.sort(Collections.reverseOrder());

        // Assign highest to lowest across abilities
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        Ability[] abilities = Ability.values();
        for (int i = 0; i < 6; i++) {
            scores.put(abilities[i], rolls.get(i));
        }

        println("\nStats: " + rolls);
        println("Assigned highest to lowest: STR, DEX, CON, INT, WIS, CHA");
        sleep(1500);
        println(colorize("\nYour destiny has been forged!", GREEN));
        pressEnterToContinue();

        return new Object[] { name, randomRace, randomClass, scores };
    }

    /**
     * Displays the final character sheet.
     */
    private static void showFinalCharacterSheet(Character character) {
        clearScreen();
        printBox("YOUR HERO IS READY!", 80, GREEN);
        println();
        println(bold(centerText(character.getName(), 80)));
        println(centerText("Level " + character.getLevel() + " " + 
                character.getRace().getDisplayName() + " " + 
                character.getCharacterClass().getDisplayName(), 80));
        println();
        printDivider('─', 80, WHITE);

        println(bold("\nABILITY SCORES"));
        println(character.getAbilityScoresString());
        println();

        println(bold("COMBAT"));
        println(String.format("  Hit Points: %s%d/%d%s", 
                colorize("", GREEN), 
                character.getCurrentHitPoints(), 
                character.getMaxHitPoints(), 
                colorize("", WHITE)));
        println(String.format("  Armor Class: %d", character.getArmorClass()));
        println(String.format("  Initiative: %+d", character.getAbilityModifier(Ability.DEXTERITY)));
        println(String.format("  Speed: %d ft", character.getRace().getSpeed()));
        println(String.format("  Proficiency Bonus: +%d", character.getProficiencyBonus()));
        println();

        println(bold("RACIAL TRAITS"));
        println("  " + character.getRace().getDisplayName() + " traits applied");
        println();

        printDivider('─', 80, WHITE);
        printBox("Press Enter to begin your adventure as " + character.getName() + "...", 80, YELLOW);
        scanner.nextLine();
        clearScreen();  // Clear before returning to game
    }

    private static List<Skill> getClassSkillOptions(CharacterClass cc) {
        return switch (cc) {
            case BARBARIAN -> List.of(
                Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION,
                Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL
            );
            case BARD -> List.of(Skill.values()); // Bards can choose any skill
            case CLERIC -> List.of(
                Skill.HISTORY, Skill.INSIGHT, Skill.MEDICINE,
                Skill.PERSUASION, Skill.RELIGION
            );
            case DRUID -> List.of(
                Skill.ARCANA, Skill.ANIMAL_HANDLING, Skill.INSIGHT,
                Skill.MEDICINE, Skill.NATURE, Skill.PERCEPTION,
                Skill.RELIGION, Skill.SURVIVAL
            );
            case FIGHTER -> List.of(
                Skill.ACROBATICS, Skill.ANIMAL_HANDLING, Skill.ATHLETICS,
                Skill.HISTORY, Skill.INSIGHT, Skill.INTIMIDATION,
                Skill.PERCEPTION, Skill.SURVIVAL
            );
            case MONK -> List.of(
                Skill.ACROBATICS, Skill.ATHLETICS, Skill.HISTORY,
                Skill.INSIGHT, Skill.RELIGION, Skill.STEALTH
            );
            case PALADIN -> List.of(
                Skill.ATHLETICS, Skill.INSIGHT, Skill.INTIMIDATION,
                Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION
            );
            case RANGER -> List.of(
                Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INSIGHT,
                Skill.INVESTIGATION, Skill.NATURE, Skill.PERCEPTION,
                Skill.STEALTH, Skill.SURVIVAL
            );
            case ROGUE -> List.of(
                Skill.ACROBATICS, Skill.ATHLETICS, Skill.DECEPTION,
                Skill.INSIGHT, Skill.INTIMIDATION, Skill.INVESTIGATION,
                Skill.PERCEPTION, Skill.PERFORMANCE, Skill.PERSUASION,
                Skill.SLEIGHT_OF_HAND, Skill.STEALTH
            );
            case SORCERER -> List.of(
                Skill.ARCANA, Skill.DECEPTION, Skill.INSIGHT,
                Skill.INTIMIDATION, Skill.PERSUASION, Skill.RELIGION
            );
            case WARLOCK -> List.of(
                Skill.ARCANA, Skill.DECEPTION, Skill.HISTORY,
                Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.NATURE,
                Skill.RELIGION
            );
            case WIZARD -> List.of(
                Skill.ARCANA, Skill.HISTORY, Skill.INSIGHT,
                Skill.INVESTIGATION, Skill.MEDICINE, Skill.RELIGION
            );
        };
    }

    private static int getClassSkillCount(CharacterClass cc) {
        return switch (cc) {
            case BARD, RANGER, ROGUE -> 3; // Rogue gets 4, but 3 for simplicity
            default -> 2;
        };
    }

    private static void selectSkillProficiencies(Character character) {
        CharacterClass cc = character.getCharacterClass();
        List<Skill> availableSkills = new ArrayList<>(getClassSkillOptions(cc));
        int numChoices = getClassSkillCount(cc);

        println(bold("As a " + cc.getDisplayName() + ", choose " + numChoices + " skill proficiencies:\n"));

        Set<Skill> chosen = EnumSet.noneOf(Skill.class);

        for (int i = 0; i < numChoices; i++) {
            clearScreen();
            printBox("SKILL PROFICIENCIES (" + (i + 1) + "/" + numChoices + ")", 70, CYAN);
            println(bold("Choose a skill to be proficient in:\n"));

            // Display available skills
            List<Skill> remaining = availableSkills.stream()
                    .filter(s -> !chosen.contains(s))
                    .toList();

            for (int j = 0; j < remaining.size(); j++) {
                Skill skill = remaining.get(j);
                Ability linked = skill.getAbility();
                int mod = character.getAbilityModifier(linked);
                int withProf = mod + character.getProficiencyBonus();
                
                println(String.format("%s) %s (%s) — currently %+d, with proficiency %+d",
                        colorize(String.valueOf(j + 1), YELLOW),
                        bold(skill.getDisplayName()),
                        linked.getAbbreviation(),
                        mod,
                        withProf));
            }
            println();

            int choice = promptForInt("Select skill (1-" + remaining.size() + "): ", 1, remaining.size());
            Skill selected = remaining.get(choice - 1);
            chosen.add(selected);
            character.addSkillProficiency(selected);

            println(colorize("✓ You are now proficient in " + selected.getDisplayName() + "!", GREEN));
            pressEnterToContinue();
        }

        println(colorize("\nSkill proficiencies selected!", GREEN));
    }

    private static void randomSkillProficiencies(Character character) {
        CharacterClass cc = character.getCharacterClass();
        List<Skill> available = new ArrayList<>(getClassSkillOptions(cc));
        int numChoices = getClassSkillCount(cc);
        
        Collections.shuffle(available);
        
        println("\nRandom skill proficiencies:");
        for (int i = 0; i < numChoices && i < available.size(); i++) {
            Skill skill = available.get(i);
            character.addSkillProficiency(skill);
            println("  • " + skill.getDisplayName());
        }
    }

    private static void selectStartingEquipment(Character character) {
        CharacterClass cc = character.getCharacterClass();

        println(bold("As a " + cc.getDisplayName() + ", you receive starting equipment.\n"));
        println("Choose your equipment package:\n");

        // Get equipment options for the class
        List<String> optionA = getEquipmentOptionA(cc);
        List<String> optionB = getEquipmentOptionB(cc);
        int goldOption = getStartingGold(cc);

        println(colorize("Option A:", YELLOW));
        for (String item : optionA) {
            println("  • " + item);
        }
        println();

        println(colorize("Option B:", YELLOW));
        for (String item : optionB) {
            println("  • " + item);
        }
        println();

        println(colorize("Option C:", YELLOW));
        println("  • " + goldOption + " gold pieces (buy your own equipment)");
        println();

        int choice = promptForInt("Choose option (1=A, 2=B, 3=Gold): ", 1, 3);

        Inventory inventory = character.getInventory();

        switch (choice) {
            case 1 -> {
                println(colorize("\n✓ You receive Option A equipment!", GREEN));
                addEquipmentOptionA(character, cc);
                println("Equipment added to your inventory:");
                for (String item : optionA) {
                    println("  • " + item);
                }
            }
            case 2 -> {
                println(colorize("\n✓ You receive Option B equipment!", GREEN));
                addEquipmentOptionB(character, cc);
                println("Equipment added to your inventory:");
                for (String item : optionB) {
                    println("  • " + item);
                }
            }
            case 3 -> {
                println(colorize("\n✓ You receive " + goldOption + " gold pieces!", GREEN));
                println("Visit a shop to purchase your equipment.");
                // Gold is stored in copper: 1 gp = 100 cp
                inventory.addGold(goldOption * 100);
            }
        }

        // Give everyone a small amount of pocket change (10 cp = 1 sp)
        inventory.addGold(10);
    }

    private static void addEquipmentOptionA(Character character, CharacterClass cc) {
        Inventory inv = character.getInventory();
        StandardEquipment equip = StandardEquipment.getInstance();

        switch (cc) {
            case BARBARIAN -> {
                inv.addItem(equip.getWeapon("greataxe"));
                inv.addItem(equip.getWeapon("handaxe"));
                inv.addItem(equip.getWeapon("handaxe"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.equipToSlot(equip.getWeapon("greataxe"), EquipmentSlot.MAIN_HAND);
            }
            case BARD -> {
                inv.addItem(equip.getWeapon("rapier"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("rapier"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case CLERIC -> {
                inv.addItem(equip.getWeapon("mace"));
                inv.addItem(equip.getArmor("scale_mail"));
                inv.addItem(equip.getArmor("shield"));
                inv.equipToSlot(equip.getWeapon("mace"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("scale_mail"), EquipmentSlot.ARMOR);
                inv.equipToSlot(equip.getArmor("shield"), EquipmentSlot.OFF_HAND);
            }
            case DRUID -> {
                inv.addItem(equip.getArmor("shield"));
                inv.addItem(equip.getWeapon("scimitar"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.equipToSlot(equip.getWeapon("scimitar"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
                inv.equipToSlot(equip.getArmor("shield"), EquipmentSlot.OFF_HAND);
            }
            case FIGHTER -> {
                inv.addItem(equip.getWeapon("longsword"));
                inv.addItem(equip.getArmor("chain_mail"));
                inv.addItem(equip.getArmor("shield"));
                inv.equipToSlot(equip.getWeapon("longsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("chain_mail"), EquipmentSlot.ARMOR);
                inv.equipToSlot(equip.getArmor("shield"), EquipmentSlot.OFF_HAND);
            }
            case MONK -> {
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("shortsword"), EquipmentSlot.MAIN_HAND);
            }
            case PALADIN -> {
                inv.addItem(equip.getWeapon("longsword"));
                inv.addItem(equip.getArmor("chain_mail"));
                inv.addItem(equip.getArmor("shield"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.equipToSlot(equip.getWeapon("longsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("chain_mail"), EquipmentSlot.ARMOR);
                inv.equipToSlot(equip.getArmor("shield"), EquipmentSlot.OFF_HAND);
            }
            case RANGER -> {
                inv.addItem(equip.getWeapon("longbow"));
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.equipToSlot(equip.getWeapon("longbow"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case ROGUE -> {
                inv.addItem(equip.getWeapon("rapier"));
                inv.addItem(equip.getWeapon("shortbow"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("rapier"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case SORCERER, WARLOCK, WIZARD -> {
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("quarterstaff"));
                inv.equipToSlot(equip.getWeapon("quarterstaff"), EquipmentSlot.MAIN_HAND);
            }
        }
    }

    private static void addEquipmentOptionB(Character character, CharacterClass cc) {
        Inventory inv = character.getInventory();
        StandardEquipment equip = StandardEquipment.getInstance();

        switch (cc) {
            case BARBARIAN -> {
                inv.addItem(equip.getWeapon("handaxe"));
                inv.addItem(equip.getWeapon("handaxe"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.equipToSlot(equip.getWeapon("handaxe"), EquipmentSlot.MAIN_HAND);
            }
            case BARD -> {
                inv.addItem(equip.getWeapon("longsword"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("longsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case CLERIC -> {
                inv.addItem(equip.getWeapon("warhammer"));
                inv.addItem(equip.getArmor("chain_mail"));
                inv.addItem(equip.getArmor("shield"));
                inv.equipToSlot(equip.getWeapon("warhammer"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("chain_mail"), EquipmentSlot.ARMOR);
                inv.equipToSlot(equip.getArmor("shield"), EquipmentSlot.OFF_HAND);
            }
            case DRUID -> {
                inv.addItem(equip.getWeapon("quarterstaff"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.equipToSlot(equip.getWeapon("quarterstaff"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case FIGHTER -> {
                inv.addItem(equip.getWeapon("longbow"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.addItem(equip.getWeapon("handaxe"));
                inv.addItem(equip.getWeapon("handaxe"));
                inv.equipToSlot(equip.getWeapon("longbow"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case MONK -> {
                inv.addItem(equip.getWeapon("quarterstaff"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("quarterstaff"), EquipmentSlot.MAIN_HAND);
            }
            case PALADIN -> {
                inv.addItem(equip.getWeapon("greatsword"));
                inv.addItem(equip.getArmor("chain_mail"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.addItem(equip.getWeapon("javelin"));
                inv.equipToSlot(equip.getWeapon("greatsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("chain_mail"), EquipmentSlot.ARMOR);
            }
            case RANGER -> {
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getArmor("scale_mail"));
                inv.equipToSlot(equip.getWeapon("shortsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("scale_mail"), EquipmentSlot.ARMOR);
            }
            case ROGUE -> {
                inv.addItem(equip.getWeapon("shortsword"));
                inv.addItem(equip.getWeapon("shortbow"));
                inv.addItem(equip.getArmor("leather_armor"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("shortsword"), EquipmentSlot.MAIN_HAND);
                inv.equipToSlot(equip.getArmor("leather_armor"), EquipmentSlot.ARMOR);
            }
            case SORCERER, WARLOCK, WIZARD -> {
                inv.addItem(equip.getWeapon("light_crossbow"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.addItem(equip.getWeapon("dagger"));
                inv.equipToSlot(equip.getWeapon("light_crossbow"), EquipmentSlot.MAIN_HAND);
            }
        }
    }

    private static List<String> getEquipmentOptionA(CharacterClass cc) {
        return switch (cc) {
            case BARBARIAN -> List.of("Greataxe", "2 Handaxes", "Explorer's Pack", "4 Javelins");
            case BARD -> List.of("Rapier", "Diplomat's Pack", "Lute", "Leather Armor", "Dagger");
            case CLERIC -> List.of("Mace", "Scale Mail", "Light Crossbow + 20 bolts", "Priest's Pack", "Shield", "Holy Symbol");
            case DRUID -> List.of("Wooden Shield", "Scimitar", "Leather Armor", "Explorer's Pack", "Druidic Focus");
            case FIGHTER -> List.of("Chain Mail", "Longsword", "Shield", "Light Crossbow + 20 bolts", "Dungeoneer's Pack");
            case MONK -> List.of("Shortsword", "Dungeoneer's Pack", "10 Darts");
            case PALADIN -> List.of("Longsword", "Shield", "5 Javelins", "Priest's Pack", "Chain Mail", "Holy Symbol");
            case RANGER -> List.of("Scale Mail", "2 Shortswords", "Dungeoneer's Pack", "Longbow + 20 arrows");
            case ROGUE -> List.of("Rapier", "Shortbow + 20 arrows", "Burglar's Pack", "Leather Armor", "2 Daggers", "Thieves' Tools");
            case SORCERER -> List.of("Light Crossbow + 20 bolts", "Component Pouch", "Dungeoneer's Pack", "2 Daggers");
            case WARLOCK -> List.of("Light Crossbow + 20 bolts", "Component Pouch", "Scholar's Pack", "Leather Armor", "Simple Weapon", "2 Daggers");
            case WIZARD -> List.of("Quarterstaff", "Component Pouch", "Scholar's Pack", "Spellbook");
        };
    }

    private static List<String> getEquipmentOptionB(CharacterClass cc) {
        return switch (cc) {
            case BARBARIAN -> List.of("2 Handaxes", "Any Simple Weapon", "Explorer's Pack", "4 Javelins");
            case BARD -> List.of("Longsword", "Entertainer's Pack", "Lute", "Leather Armor", "Dagger");
            case CLERIC -> List.of("Warhammer", "Chain Mail", "Light Crossbow + 20 bolts", "Explorer's Pack", "Shield", "Holy Symbol");
            case DRUID -> List.of("Wooden Shield", "Simple Melee Weapon", "Leather Armor", "Explorer's Pack", "Druidic Focus");
            case FIGHTER -> List.of("Leather Armor", "Longbow + 20 arrows", "2 Handaxes", "Dungeoneer's Pack");
            case MONK -> List.of("Simple Weapon", "Explorer's Pack", "10 Darts");
            case PALADIN -> List.of("2 Longswords", "5 Javelins", "Explorer's Pack", "Chain Mail", "Holy Symbol");
            case RANGER -> List.of("Leather Armor", "2 Shortswords", "Explorer's Pack", "Longbow + 20 arrows");
            case ROGUE -> List.of("Shortsword", "Shortbow + 20 arrows", "Dungeoneer's Pack", "Leather Armor", "2 Daggers", "Thieves' Tools");
            case SORCERER -> List.of("Any Simple Weapon", "Arcane Focus", "Explorer's Pack", "2 Daggers");
            case WARLOCK -> List.of("Any Simple Weapon", "Arcane Focus", "Dungeoneer's Pack", "Leather Armor", "Simple Weapon", "2 Daggers");
            case WIZARD -> List.of("Dagger", "Arcane Focus", "Explorer's Pack", "Spellbook");
        };
    }

    private static int getStartingGold(CharacterClass cc) {
        // Average starting gold by class (simplified)
        return switch (cc) {
            case BARBARIAN -> 40;  // 2d4 × 10
            case BARD -> 100;      // 5d4 × 10
            case CLERIC -> 100;    // 5d4 × 10
            case DRUID -> 40;      // 2d4 × 10
            case FIGHTER -> 100;   // 5d4 × 10
            case MONK -> 10;       // 5d4 (no × 10)
            case PALADIN -> 100;   // 5d4 × 10
            case RANGER -> 100;    // 5d4 × 10
            case ROGUE -> 80;      // 4d4 × 10
            case SORCERER -> 60;   // 3d4 × 10
            case WARLOCK -> 80;    // 4d4 × 10
            case WIZARD -> 80;     // 4d4 × 10
        };
    }

    private static int getRacialBonus(Race race, Ability ability) {
        // Human gets +1 to all
        if (race == Race.HUMAN) {
            return 1;
        }
        return race.getAbilityBonus(ability);
    }

    private static int getPointCost(int targetScore) {
        if (targetScore <= 8) return 0;
        if (targetScore > 15) return 999; // Can't go above 15
        
        // Cost to go from (score-1) to score
        return switch (targetScore) {
            case 9, 10, 11, 12, 13 -> 1;
            case 14, 15 -> 2;
            default -> 0;
        };
    }

    private static Ability parseAbility(String input) {
        if (input == null || input.isEmpty()) return null;
        
        input = input.toLowerCase().trim();
        
        for (Ability a : Ability.values()) {
            if (a.getFullName().toLowerCase().startsWith(input) ||
                a.getAbbreviation().toLowerCase().equals(input)) {
                return a;
            }
        }
        
        // Try number
        try {
            int num = Integer.parseInt(input);
            if (num >= 1 && num <= 6) {
                return Ability.values()[num - 1];
            }
        } catch (NumberFormatException ignored) {}
        
        return null;
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) return text;
        return text + " ".repeat(width - text.length());
    }

    private static String promptForString(String prompt, String defaultValue) {
        print(colorize(prompt + " ", CYAN));
        if (!defaultValue.isEmpty()) {
            print(colorize("(default: " + defaultValue + ") ", MAGENTA));
        }
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private static <T extends Enum<T>> T promptForEnum(T[] values, String prompt) {
        while (true) {
            print(colorize(prompt, CYAN));
            String input = scanner.nextLine().trim();
            try {
                int n = Integer.parseInt(input);
                if (n >= 1 && n <= values.length) {
                    return values[n - 1];
                }
            } catch (NumberFormatException ignored) {}
            println(colorize("Invalid input. Enter a number from 1 to " + values.length + ".", RED));
        }
    }

    private static int promptForInt(String prompt, int min, int max) {
        while (true) {
            print(colorize(prompt, CYAN));
            String input = scanner.nextLine().trim();
            try {
                int n = Integer.parseInt(input);
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {}
            println(colorize("Please enter a number between " + min + " and " + max + ".", RED));
        }
    }

    private static void pressEnterToContinue() {
        println();
        print(colorize("Press Enter to continue...", MAGENTA));
        scanner.nextLine();
    }

    private static String centerText(String text, int width) {
        int pad = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Character player = CharacterCreator.createCharacter();
        println(bold("\nThe adventure begins for " + player.getName() + "!"));
    }
}