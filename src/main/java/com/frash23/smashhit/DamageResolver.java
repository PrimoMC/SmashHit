package com.frash23.smashhit;

import com.frash23.smashhit.Util.Reflection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class DamageResolver
{

    public static DamageResolver getDamageResolver( boolean USE_CRITS, boolean OLD_CRITS )
    {
        return new DamageResolver( USE_CRITS, OLD_CRITS );
    }

    private boolean USE_CRITS, OLD_CRITS;

    public double getDamage( Player damager, Damageable entity )
    {

        try
        {
            Object handle = Reflection.getHandle( damager );
            Field field = Reflection.getField( Reflection.getNMSClass( "GenericAttributes" ), "ATTACK_DAMAGE" );
            Object attributeInstance = Reflection.getMethod( handle.getClass(), "getAttributeInstance", new Class[0] ).invoke( handle, new Object[]{ field.get( null ) } );
            double damage = (double) Reflection.getMethod( attributeInstance.getClass(), "getValue", new Class[0] ).invoke( handle, new Object[0] );
            if ( USE_CRITS && !( (Entity) damager ).isOnGround() && damager.getVelocity().getY() < 0 && OLD_CRITS || !damager.isSprinting() )
            {
                damage *= 1.5;
            }

            return damage;
        }
        catch ( IllegalAccessException | InvocationTargetException e )
        {
            e.printStackTrace();
        }
        return 0;
    }

    DamageResolver( boolean useCrits, boolean oldCrits )
    {
        USE_CRITS = useCrits;
        OLD_CRITS = oldCrits;
    }

}
