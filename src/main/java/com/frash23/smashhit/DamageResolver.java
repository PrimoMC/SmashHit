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
        if ( USE_CRITS )
        {
            if ( ( !damager.isOnGround() && damager.getVelocity().getY() < 0 ) )
            {
                if ( !( !OLD_CRITS && damager.isSprinting() ) )
                {
                    damage *= 1.5;
                    ParticleEffect.CRIT.display( 0, 0, 0, .5f, 10, entity.getEyeLocation(), 16 );
                    damager.playSound( damager.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f );
                }
            }
        }
        return damage;
    }

    DamageResolver( boolean useCrits, boolean oldCrits )
    {
        USE_CRITS = useCrits;
        OLD_CRITS = oldCrits;
    }

}
