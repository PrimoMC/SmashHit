package com.frash23.smashhit.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.frash23.smashhit.DamageResolver;
import com.frash23.smashhit.Event.AsyncPreDamageEvent;
import com.frash23.smashhit.Packet.WrapperPlayServerSetCooldown;
import com.frash23.smashhit.Particle.ParticleEffect;
import com.frash23.smashhit.SmashHit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getPluginManager;

public class SmashHitListener extends PacketAdapter
{
    private SmashHit plugin;
    private final long IMMUNITY_MILLI;
    private ProtocolManager pmgr;
    private DamageResolver damageResolver;

    private Map<UUID, Integer> cps = new HashMap<>();
    private Map<UUID, Long> lastHit = new HashMap<>();
    private Queue<EntityDamageByEntityEvent> hitQueue = new ConcurrentLinkedQueue<>();

    private static byte MAX_CPS;
    private static float MAX_DISTANCE;

    public SmashHitListener( SmashHit pl, long damageImmunity, boolean useCrits, boolean oldCrits, int maxCps, double maxDistance )
    {
        super( pl, ListenerPriority.HIGH, Collections.singletonList( PacketType.Play.Client.USE_ENTITY ) );

        plugin = pl;
        this.IMMUNITY_MILLI = damageImmunity;
        pmgr = ProtocolLibrary.getProtocolManager();

        damageResolver = DamageResolver.getDamageResolver( useCrits, oldCrits );

        MAX_CPS = (byte) maxCps;
        MAX_DISTANCE = (float) maxDistance * (float) maxDistance;
    }

    private BukkitTask hitQueueProcessor = new BukkitRunnable()
    {
        @Override
        public void run()
        {
            while ( hitQueue.size() > 0 )
            {
                EntityDamageByEntityEvent e = hitQueue.remove();
                ( (Damageable) e.getEntity() ).damage( e.getFinalDamage(), e.getDamager() );
            }
        }
    }.runTaskTimer( SmashHit.getInstance(), 1, 1 );

    private BukkitTask cpsResetter = new BukkitRunnable()
    {
        @Override
        public void run()
        {
            cps.clear();
        }
    }.runTaskTimer( SmashHit.getInstance(), 20, 20 );

    @SuppressWarnings( "deprecation" )
    @Override
    public void onPacketReceiving( PacketEvent e )
    {

        PacketContainer packet = e.getPacket();
        Player attacker = e.getPlayer();
        Entity entity = packet.getEntityModifier( e ).read( 0 );
        LivingEntity target = entity instanceof LivingEntity ? (LivingEntity) entity : null;
        World world = attacker.getWorld();

        //don't handle packets other than entity interaction.
        if ( e.getPacketType() != PacketType.Play.Client.USE_ENTITY )
        {
            return;
        }

        //don't handle packets other than entity attacks.
        if ( packet.getEntityUseActions().read( 0 ) != EntityUseAction.ATTACK )
        {
            return;
        }

        //don't hit entites who don't exist or are already dead.
        if ( target == null || target.isDead() )
        {
            return;
        }

        //don't hit entities who are in a different world.
        if ( world != target.getWorld() )
        {
            return;
        }

        //don't hit entities who are too far away from the attacker
        if ( attacker.getLocation().distanceSquared( target.getLocation() ) >= MAX_DISTANCE )
        {
            return;
        }

        if ( target instanceof Player )
        {
            //don't hit players when pvp is disabled.
            if ( !world.getPVP() )
            {
                return;
            }

            //don't hit players who are in creative mode.
            if ( ( (Player) target ).getGameMode() == GameMode.CREATIVE )
            {
                return;
            }

            //don't hit players who are in the same team without friendly fire.
            if ( attacker.getScoreboard() != null )
            {
                Team team = attacker.getScoreboard().getEntryTeam( attacker.getName() );

                if ( team != null )
                {
                    if ( !team.allowFriendlyFire() && team.hasEntry( target.getName() ) )
                    {
                        return;
                    }
                }
            }
        }

        e.setCancelled( true );
        //don't hit players when they're within the damage immunity time.
        if ( lastHit.containsKey( target.getUniqueId() ) && System.currentTimeMillis() - lastHit.get( target.getUniqueId() ) < IMMUNITY_MILLI )
        {
            return;
        }
            /* Construct the fake packet for making the attacker's
             * victim appear hit */
        PacketContainer damageAnimation = new PacketContainer( PacketType.Play.Server.ENTITY_STATUS );
        damageAnimation.getIntegers().write( 0, target.getEntityId() );
        damageAnimation.getBytes().write( 0, (byte) 2 );

        try
        {
            double damage = damageResolver.getDamage( attacker );

            AsyncPreDamageEvent damageEvent = new AsyncPreDamageEvent( attacker, target, damage );
            getPluginManager().callEvent( damageEvent );

            if ( !damageEvent.isCancelled() )
            {
                if ( damageResolver.isCrit( attacker ) )
                {
                    ParticleEffect.CRIT.display( 0, 0, 0, .5f, 10, target.getEyeLocation(), 16 );
                    attacker.playSound( attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f );
                }
                pmgr.sendServerPacket( attacker, damageAnimation );
                for ( Player player : attacker.getNearbyEntities( 16, 16, 16 ).stream().filter( p -> p instanceof Player ).map( p -> (Player) p ).collect( Collectors.toList() ) )
                {
                    pmgr.sendServerPacket( player, damageAnimation );
                }

                    /* Check if attacker's CPS is within the specified maximum */
                int attackerCps = cps.containsKey( attacker.getUniqueId() ) ? cps.get( attacker.getUniqueId() ) : 0;
                cps.put( attacker.getUniqueId(), attackerCps + 1 );

                    /* By handling CPS this way, the recorded CPS will still increment even if the limit is reached.
                     * This should weed out some hackers nicely */
                if ( attackerCps <= MAX_CPS )
                {
                    if ( target instanceof Player )
                    {
                        handleDisable( attacker, (Player) target );
                    }
                    lastHit.put( target.getUniqueId(), System.currentTimeMillis() );
                    hitQueue.add( new EntityDamageByEntityEvent( attacker, target, DamageCause.ENTITY_ATTACK, damageEvent.getDamage() ) );
                }
            }

        }
        catch ( InvocationTargetException err )
        {
            throw new RuntimeException( "Error while sending damage packet: ", err );
        }

    }

    public void stop()
    {
        cpsResetter.cancel();
        hitQueueProcessor.cancel();
        damageResolver = null;
    }

    private Random rand = new Random();

    public void handleDisable( Player attacker, Player victim )
    {
        ItemStack item = attacker.getInventory().getItemInMainHand();

        if ( item != null && item.getType().toString().contains( "AXE" ) && !item.getType().toString().contains( "PICKAXE" ) && victim.isBlocking() )
        {
            float chance = 0.25F + ( (float) item.getEnchantmentLevel( Enchantment.DIG_SPEED ) * 0.05F );

            if ( attacker.isSprinting() )
            {
                chance += 0.75F;
            }

            if ( rand.nextFloat() < chance )
            {
                try
                {
                    PacketContainer shieldAnim = new PacketContainer( PacketType.Play.Server.ENTITY_STATUS );
                    shieldAnim.getIntegers().write( 0, victim.getEntityId() );
                    shieldAnim.getBytes().write( 0, (byte) 30 );

                    ProtocolLibrary.getProtocolManager().sendServerPacket( attacker, shieldAnim );

                    for ( Player player : attacker.getNearbyEntities( 16, 16, 16 ).stream().filter( p -> p instanceof Player ).map( p -> (Player) p ).collect( Collectors.toList() ) )
                    {
                        ProtocolLibrary.getProtocolManager().sendServerPacket( player, shieldAnim );
                    }

                    WrapperPlayServerSetCooldown cooldown = new WrapperPlayServerSetCooldown();
                    cooldown.setItem( Material.SHIELD );
                    cooldown.setTicks( 20 * 5 );
                    cooldown.sendPacket( victim );

                }
                catch ( InvocationTargetException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
