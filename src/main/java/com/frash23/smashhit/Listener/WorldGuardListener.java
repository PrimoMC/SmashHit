package com.frash23.smashhit.Listener;

import com.frash23.smashhit.Event.AsyncPreDamageEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardListener implements Listener
{
    WorldGuardPlugin wg = null;

    public WorldGuardListener()
    {
        wg = (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin( "WorldGuard" );
    }

    @EventHandler
    public void onAsyncPreDamageEvent( AsyncPreDamageEvent e )
    {
        Player damager = e.getDamager();
        Damageable entity = e.getEntity();
        World world = damager.getWorld();

        RegionManager wgrm = wg.getRegionManager( world );
        StateFlag[] flags;

        if ( entity instanceof Player )
        {
            flags = new StateFlag[]{ DefaultFlag.PVP };
        }
        else if ( entity instanceof Animals )
        {
            flags = new StateFlag[]{ DefaultFlag.DAMAGE_ANIMALS };
        }
        else
        {
            flags = null;
        }

        if ( !test( wgrm, damager, entity, flags ) )
        {
            e.setCancelled( true );
        }
    }

    private boolean test( RegionManager wgrm, Player damager, Damageable entity, StateFlag... flag )
    {
        if(flag == null)
        {
            return true;
        }
        ApplicableRegionSet set = wgrm.getApplicableRegions( damager.getLocation() );
        if ( set.queryState( wg.wrapPlayer( damager ), flag ) == StateFlag.State.DENY )
        {
            return false;
        }
        set = wgrm.getApplicableRegions( entity.getLocation() );
        if ( entity instanceof Player )
        {
            if ( set.queryState( wg.wrapPlayer( (Player) entity ), flag ) == StateFlag.State.DENY )
            {
                return false;
            }
        }
        else
        {
            if ( set.queryState( null, flag ) == StateFlag.State.DENY )
            {
                return false;
            }
        }
        return true;
    }

}
