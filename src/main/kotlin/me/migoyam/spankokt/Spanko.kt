package me.migoyam.spankokt

import net.md_5.bungee.api.ChatColor
import org.bukkit.GameRule
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.ceil

public final class Spanko: JavaPlugin(), Listener{
    private val nightBar = server.createBossBar(null, BarColor.PURPLE, BarStyle.SEGMENTED_12)
    private val dayBar = server.createBossBar("§eJest już dzień.", BarColor.YELLOW, BarStyle.SOLID)
    private val percentage = 50
    private val dzielnik = (100/percentage).toDouble()
    private var task: BukkitTask? = null

    private fun required(player: Player): Int = ceil(player.world.playerCount/dzielnik).toInt()
    private fun sleeping(player: Player): Int = player.world.players.filter{player1 -> player1.pose == Pose.SLEEPING}.count()

    private fun update(sleeping: Int, required: Int){
        nightBar.setTitle("$sleeping" +  ChatColor.of("#7417bf") + " z §r$required " + ChatColor.of("#7417bf") + "graczy jest w lóżku.")
        nightBar.progress = if (sleeping >= required) 1.0 else (sleeping/required).toDouble()
    }
    private fun check(player: Player, sleeping: Int, required: Int){
        update(sleeping, required)
        if(sleeping == 0)
            nightBar.isVisible = false
        if(required > sleeping && player.world.time != 0L)
            task!!.cancel()
    }
    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        nightBar.isVisible = false
        dayBar.isVisible = false

        for(world in server.worlds.filter{world1: World ->  world1.environment == World.Environment.NORMAL})
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, percentage)
    }
    @EventHandler
    public fun onChangeWorld(event: PlayerChangedWorldEvent){
        val player = event.player
        if(player.world.environment == World.Environment.NORMAL){
            check(player, sleeping(player), required(player))
            nightBar.addPlayer(player)
            dayBar.addPlayer(player)
        }else{
            check(player, event.from.players.filter{player1 -> player1.pose == Pose.SLEEPING}.count(), ceil(event.from.playerCount/dzielnik).toInt())
            nightBar.removePlayer(player)
            dayBar.removePlayer(player)
        }
    }
    @EventHandler
    public fun onJoin(event: PlayerJoinEvent){
        val player = event.player
        if(player.world.environment == World.Environment.NORMAL){
            nightBar.addPlayer(player)
            dayBar.addPlayer(player)
            update(sleeping(player), required(player))
        }
    }
    @EventHandler
    public fun onQuit(event: PlayerQuitEvent){
        val player = event.player
        server.scheduler.runTaskLater(this, Runnable {check(player, sleeping(player), required(player))}, 1)
    }
    @EventHandler
    public fun onSleep(event: PlayerBedEnterEvent){
        if(event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK)
            return

        val player = event.player
        server.scheduler.runTaskLater(this, Runnable {
            update(sleeping(player), required(player))
            nightBar.isVisible = true
            if (sleeping(player) >= required(player)) {
                task = server.scheduler.runTaskLater(this, Runnable {
                    nightBar.isVisible = false
                    dayBar.isVisible = true
                    for(p in player.world.players)
                        p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f)

                    server.scheduler.runTaskLater(this, Runnable { dayBar.isVisible = false }, 100)
                }, 100)
            }
        }, 1)
    }
    @EventHandler
    public fun onWake(event: PlayerBedLeaveEvent){
        val player = event.player
        server.scheduler.runTaskLater(this, Runnable {check(player, sleeping(player), required(player))}, 1)
    }
}