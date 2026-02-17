package com.questkeeper.magic;

import com.questkeeper.character.Character;
import com.questkeeper.character.Character.Ability;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Abstract base class for spells providing common functionality.
 *
 * @author Marc McGough
 * @version 1.0
 */
public abstract class AbstractSpell implements Spell {

    private final String id;
    private final String name;
    private final String description;
    private final int level;
    private final SpellSchool school;
    private final CastingTime castingTime;
    private final int range;
    private final String rangeDescription;
    private final Set<SpellComponent> components;
    private final String materialComponent;
    private final SpellDuration duration;
    private final boolean concentration;
    private final boolean ritual;
    private final boolean canTargetEnemy;
    private final boolean canTargetAlly;
    private final boolean requiresAttackRoll;
    private final Ability saveAbility;  // null if no save

    protected AbstractSpell(Builder<?> builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.level = builder.level;
        this.school = builder.school;
        this.castingTime = builder.castingTime;
        this.range = builder.range;
        this.rangeDescription = builder.rangeDescription;
        this.components = Collections.unmodifiableSet(builder.components);
        this.materialComponent = builder.materialComponent;
        this.duration = builder.duration;
        this.concentration = builder.concentration;
        this.ritual = builder.ritual;
        this.canTargetEnemy = builder.canTargetEnemy;
        this.canTargetAlly = builder.canTargetAlly;
        this.requiresAttackRoll = builder.requiresAttackRoll;
        this.saveAbility = builder.saveAbility;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public int getLevel() { return level; }

    @Override
    public SpellSchool getSchool() { return school; }

    @Override
    public CastingTime getCastingTime() { return castingTime; }

    @Override
    public int getRange() { return range; }

    @Override
    public String getRangeDescription() { return rangeDescription; }

    @Override
    public Set<SpellComponent> getComponents() { return components; }

    @Override
    public String getMaterialComponent() { return materialComponent; }

    @Override
    public SpellDuration getDuration() { return duration; }

    @Override
    public boolean requiresConcentration() { return concentration; }

    @Override
    public boolean isRitual() { return ritual; }

    @Override
    public boolean canTargetEnemy() { return canTargetEnemy; }

    @Override
    public boolean canTargetAlly() { return canTargetAlly; }

    @Override
    public boolean requiresAttackRoll() { return requiresAttackRoll; }

    @Override
    public boolean allowsSavingThrow() { return saveAbility != null; }

    @Override
    public Ability getSaveAbility() { return saveAbility; }

    /**
     * Builder for creating spells.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        private String id;
        private String name;
        private String description;
        private int level;
        private SpellSchool school;
        private CastingTime castingTime = CastingTime.ACTION;
        private int range = 0;
        private String rangeDescription = "Self";
        private Set<SpellComponent> components = EnumSet.noneOf(SpellComponent.class);
        private String materialComponent;
        private SpellDuration duration = SpellDuration.INSTANTANEOUS;
        private boolean concentration = false;
        private boolean ritual = false;
        private boolean canTargetEnemy = false;
        private boolean canTargetAlly = false;
        private boolean requiresAttackRoll = false;
        private Ability saveAbility = null;

        public T id(String id) {
            this.id = id;
            return (T) this;
        }

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        public T level(int level) {
            this.level = level;
            return (T) this;
        }

        public T school(SpellSchool school) {
            this.school = school;
            return (T) this;
        }

        public T castingTime(CastingTime castingTime) {
            this.castingTime = castingTime;
            return (T) this;
        }

        public T range(int range, String description) {
            this.range = range;
            this.rangeDescription = description;
            return (T) this;
        }

        public T rangeSelf() {
            this.range = 0;
            this.rangeDescription = "Self";
            return (T) this;
        }

        public T rangeTouch() {
            this.range = 5;
            this.rangeDescription = "Touch";
            return (T) this;
        }

        public T rangeFeet(int feet) {
            this.range = feet;
            this.rangeDescription = feet + " feet";
            return (T) this;
        }

        public T components(SpellComponent... components) {
            this.components = EnumSet.noneOf(SpellComponent.class);
            for (SpellComponent c : components) {
                this.components.add(c);
            }
            return (T) this;
        }

        public T verbal() {
            this.components.add(SpellComponent.VERBAL);
            return (T) this;
        }

        public T somatic() {
            this.components.add(SpellComponent.SOMATIC);
            return (T) this;
        }

        public T material(String material) {
            this.components.add(SpellComponent.MATERIAL);
            this.materialComponent = material;
            return (T) this;
        }

        public T duration(SpellDuration duration) {
            this.duration = duration;
            return (T) this;
        }

        public T concentration() {
            this.concentration = true;
            return (T) this;
        }

        public T ritual() {
            this.ritual = true;
            return (T) this;
        }

        public T targetEnemy() {
            this.canTargetEnemy = true;
            return (T) this;
        }

        public T targetAlly() {
            this.canTargetAlly = true;
            return (T) this;
        }

        public T targetAny() {
            this.canTargetEnemy = true;
            this.canTargetAlly = true;
            return (T) this;
        }

        public T attackRoll() {
            this.requiresAttackRoll = true;
            return (T) this;
        }

        public T savingThrow(Ability ability) {
            this.saveAbility = ability;
            return (T) this;
        }

        public abstract Spell build();
    }
}
