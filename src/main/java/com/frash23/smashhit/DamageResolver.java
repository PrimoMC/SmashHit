package com.frash23.smashhit;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DamageResolver
{

    public static DamageResolver getDamageResolver( boolean USE_CRITS, boolean OLD_CRITS )
    {
        return new DamageResolver( USE_CRITS, OLD_CRITS );
    }

    private boolean USE_CRITS, OLD_CRITS;

    public double getDamage( Player damager, Damageable entity )
    {
        double damage = damager.getAttribute( Attribute.GENERIC_ATTACK_DAMAGE ).getValue();
        if ( USE_CRITS && !( (Entity) damager ).isOnGround() && damager.getVelocity().getY() < 0 && OLD_CRITS || !damager.isSprinting() )
        {
            damage *= 1.5;
        }
        return damage;
    }

    DamageResolver( boolean useCrits, boolean oldCrits )
    {
        USE_CRITS = useCrits;
        OLD_CRITS = oldCrits;
    }

}
