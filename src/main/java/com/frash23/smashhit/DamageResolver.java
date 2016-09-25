package com.frash23.smashhit;

import com.frash23.smashhit.Particle.ParticleEffect;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DamageResolver
{

    public static DamageResolver getDamageResolver( boolean USE_CRITS, boolean OLD_CRITS )
    {
        return new DamageResolver( USE_CRITS, OLD_CRITS );
    }

    private boolean USE_CRITS, OLD_CRITS;

    public double getDamage( Player damager, LivingEntity entity )
    {
        double damage = damager.getAttribute( Attribute.GENERIC_ATTACK_DAMAGE ).getValue();
        if(isCrit( damager ))
        {
            damage *= 1.5;
        }
        return damage;
    }

    public boolean isCrit(Player damager)
    {

        if ( USE_CRITS )
        {
            if ( ( !damager.isOnGround() && damager.getVelocity().getY() < 0 ) )
            {
                if ( !( !OLD_CRITS && damager.isSprinting() ) )
                {
                   return true;
                }
            }
        }
        return false;
    }

    private DamageResolver( boolean useCrits, boolean oldCrits )
    {
        USE_CRITS = useCrits;
        OLD_CRITS = oldCrits;
    }

}
