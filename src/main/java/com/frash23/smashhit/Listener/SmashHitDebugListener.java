package com.frash23.smashhit.Listener;

import com.frash23.smashhit.SmashHit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SmashHitDebugListener implements Listener
{
    private SmashHit plugin;

    public SmashHitDebugListener( SmashHit pl )
    {
        plugin = pl;
    }

    @EventHandler( priority = EventPriority.LOWEST )
    public void onEntityDamageByEntityEvent( EntityDamageByEntityEvent e )
    {
        if ( e.getDamager() instanceof Player )
        {
            plugin.getLogger().info( " --- SmashHit Debug --- " );
            plugin.getLogger().info( "Attacker: " + e.getDamager().getName() + ", victim: " + e.getEntity().getName() );
            plugin.getLogger().info( "Final damage: " + e.getFinalDamage() );
        }
    }
}
