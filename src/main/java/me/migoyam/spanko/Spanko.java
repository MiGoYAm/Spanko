package me.migoyam.spanko;

import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Spanko extends JavaPlugin implements Listener {
    private final BossBar nightBar = getServer().createBossBar(null, BarColor.PURPLE, BarStyle.SEGMENTED_12);
    private final BossBar dayBar = getServer().createBossBar("§eJest już dzień.", BarColor.YELLOW, BarStyle.SOLID);
    private BukkitTask task;
    private final int percentage = 50;
    private final int dzielnik = 100/percentage;

    private int required(Player player) {
        return (int) Math.ceil((double) player.getWorld().getPlayerCount()/dzielnik);
    }
    private int sleeping(Player player) {
        return (int) player.getWorld().getPlayers().stream().filter(player1 -> player1.getPose() == Pose.SLEEPING).count(); // I stole this code from https://github.com/nkomarn/Harbor/blob/f27335e2420a331100978efe96d6b3257353a578/src/main/java/xyz/nkomarn/harbor/task/Checker.java#L160
    }
    private void update(Integer sleeping, Integer required) {
        nightBar.setTitle(sleeping + "" + ChatColor.of("#7417bf") +  " z §r" + required + "" + ChatColor.of("#7417bf") + " jest w lóżku.");
        nightBar.setProgress((sleeping >= required) ? 1 : (double)sleeping/required);
    }
    private void check(Player player, Integer sleeping, Integer required) {
        update(sleeping, required);
        if (sleeping == 0)
            nightBar.setVisible(false);
        if (required > sleeping && player.getWorld().getTime() != 0 && task != null)
            task.cancel();
    }
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        nightBar.setVisible(false);
        dayBar.setVisible(false);

        for(World world : getServer().getWorlds().stream().filter(world -> world.getEnvironment().equals(World.Environment.NORMAL)).collect(Collectors.toList()))
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, percentage);
    }
    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            check(player, sleeping(player), required(player));
            nightBar.addPlayer(player);
            dayBar.addPlayer(player);
        } else {
            nightBar.removePlayer(player);
            dayBar.removePlayer(player);
            check(player, (int) event.getFrom().getPlayers().stream().filter(player1 -> player1.getPose() == Pose.SLEEPING).count(),
                    (int)Math.ceil((double) event.getFrom().getPlayerCount()/dzielnik));
        }
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            update(sleeping(player), required(player));
            nightBar.addPlayer(player);
            dayBar.addPlayer(player);
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, ()-> check(player, sleeping(player), required(player)),1);
    }
    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) // I stole this code from https://github.com/nkomarn/Harbor/blob/f27335e2420a331100978efe96d6b3257353a578/src/main/java/xyz/nkomarn/harbor/listener/BedListener.java#L31
            return;

        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, ()-> {
            update(sleeping(player), required(player));
            nightBar.setVisible(true);
            if (sleeping(player) >= required(player)) {
                task = getServer().getScheduler().runTaskLater(this, ()-> {
                    nightBar.setVisible(false);
                    dayBar.setVisible(true);
                    for (Player p : player.getWorld().getPlayers())
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);

                    getServer().getScheduler().runTaskLater(this, ()-> dayBar.setVisible(false),100);
                },100);
            }
        },1);
    }
    @EventHandler
    public void onWake(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, ()-> check(player, sleeping(player), required(player)),1);
    }
}