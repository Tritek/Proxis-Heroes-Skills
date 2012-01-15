package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;
import org.bukkit.configuration.ConfigurationSection;

public class SkillEmpathy extends TargettedSkill {

    public SkillEmpathy(Heroes plugin) {
        super(plugin, "Empathy");
        setDescription("Deals $1% of the targets missing health in damage.");
        setUsage("/skill empathy [target]");
        setArgumentRange(0, 1);
        setIdentifiers(new String[] { "skill empathy" });
        
        setTypes(SkillType.DARK, SkillType.DAMAGING, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMod = (SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getLevel())) * 100;
        damageMod = damageMod > 0 ? damageMod : 0;
        String description = getDescription().replace("$1", damageMod + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getLevel()) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false) * hero.getLevel());
        if (mana > 0) {
            description += " M:" + mana;
        }
        
        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, mana, true) * hero.getLevel());
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        
        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getLevel());
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-damage", 0);
        node.set("damage-modifier", 1);
        node.set("damage-modifier-increase", 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target == player) {
            return SkillResult.INVALID_TARGET;
        }
        int maxDamage = (int) SkillConfigManager.getUseSetting(hero, this, "max-damage", 10, false);
        double damageMod = (SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getLevel()));
        damageMod = damageMod > 0 ? damageMod : 0;
        int damage = (int) (hero.getMaxHealth() - hero.getHealth());
        if (maxDamage != 0 && damage > maxDamage) {
            damage = maxDamage;
        }
        if (target instanceof Player && damageCheck((Player) target, player)) {
            return SkillResult.CANCELLED;
        }
        damage = (int) Math.round(damage * damageMod);
        target.damage(damage, player);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}