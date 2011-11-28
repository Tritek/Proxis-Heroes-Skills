package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillIceAura extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillIceAura(Heroes plugin) {
        super(plugin, "IceAura");
        setDescription("Passive saves you from death if not on cooldown");
        setUsage("/skill iceaura");
        setArgumentRange(0, 0);
        setIdentifiers("skill iceaura");
        setTypes(SkillType.ICE, SkillType.SILENCABLE, SkillType.HARMFUL, SkillType.BUFF);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("health-percent-on-rebirth", .5);
        node.setProperty("on-text", "%hero% chills the air in an %skill%!");
        node.setProperty("off-text", "%hero% halts the chill of the %skill%!");
        node.setProperty("tick-damage", 1);
        node.setProperty(Setting.MANA.node(), 1);
        node.setProperty(Setting.PERIOD.node(), 5000);
        node.setProperty(Setting.RADIUS.node(), 10);
        return node;
    }
    
    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, "on-text", "%hero% chills the air in an %skill%!").replace("%hero%", "$1").replace("%skill", "$2");
        expireText = getSetting(null, "off-text", "%hero% halts the chill of the %skill%!").replace("%hero%", "$1").replace("%skill", "$2");
    }
    
    @Override
    public SkillResult use(Hero hero, String args[]) {
        if (hero.hasEffect("IceAura")) {
            hero.removeEffect(hero.getEffect("IceAura"));
        } else {
            long period = getSetting(hero, Setting.PERIOD.node(), 5000, false);
            int tickDamage = getSetting(hero, "tick-damage", 1, false);
            int range = getSetting(hero, Setting.RADIUS.node(), 10, false);
            int mana = getSetting(hero, Setting.MANA.node(), 1, false);
            hero.addEffect(new IcyAuraEffect(this, period, tickDamage, range, mana));
        }
        return SkillResult.NORMAL;
    }
    
    public class IcyAuraEffect extends PeriodicEffect {

        private int tickDamage;
        private int range;
        private int mana;

        public IcyAuraEffect(SkillIceAura skill, long period, int tickDamage, int range, int manaLoss) {
            super(skill, "IceAura", period);
            this.tickDamage = tickDamage;
            this.range = range;
            this.mana = manaLoss;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.ICE);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);

            Player player = hero.getPlayer();

            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity) {
                    LivingEntity lEntity = (LivingEntity) entity;

                    // Check if the target is damagable
                    if (!damageCheck(player, lEntity)) {
                        continue;
                    }
                    addSpellTarget(lEntity, hero);
                    lEntity.damage(tickDamage, player);
                }
            }
            if (mana > 0) {
                if (hero.getMana() - mana < 0) {
                    hero.setMana(0);
                } else {
                    hero.setMana(hero.getMana() - mana);
                }
                if (hero.isVerbose()) {
                    Messaging.send(hero.getPlayer(), Messaging.createManaBar(100));
                }
            }
        }
    }
}