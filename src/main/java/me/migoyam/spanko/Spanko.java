package me.migoyam.spanko;

import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Spanko extends JavaPlugin implements Listener {
    private final BossBar nightBar = Bukkit.createBossBar(null, BarColor.PURPLE, BarStyle.SEGMENTED_12), dayBar = Bukkit.createBossBar("§eJest już dzień.", BarColor.YELLOW, BarStyle.SOLID);
    private BukkitTask task;
    private final int dzielnik = 100/50;

    private int required(Player player) {
        return (int) Math.ceil((double) player.getWorld().getPlayers().size() / dzielnik);
    }
    private int sleeping(Player player) {
        List<Player> sleeping = player.getWorld().getPlayers().stream().filter(player1 -> player.getPose() == Pose.SLEEPING).collect(Collectors.toList());
		// I stole this code from https://github.com/nkomarn/Harbor/blob/f27335e2420a331100978efe96d6b3257353a578/src/main/java/xyz/nkomarn/harbor/task/Checker.java#L160
        return sleeping.size();
    }
    private void update(Integer sleeping, Integer required) {
        nightBar.setTitle(sleeping + "" + ChatColor.of("#7417bf") +  " z §r" + required + "" + ChatColor.of("#7417bf") + " jest w lóżku.");
        if (sleeping >= required)
            nightBar.setProgress(1);
        else
            nightBar.setProgress((double)sleeping/required);
    }
    private void check(Player player, Integer sleeping, Integer required) {
        update(sleeping, required);
        if (sleeping == 0)
            nightBar.setVisible(false);
        if (required > sleeping && player.getWorld().getTime() != 0 && task != null)
            Bukkit.getScheduler().cancelTask(task.getTaskId());
    }
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        nightBar.setVisible(false);
        dayBar.setVisible(false);
    }
    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            check(player, sleeping(player), required(player));
            nightBar.addPlayer(player);
            dayBar.addPlayer(player);
        } else {
            List<Player> sleepers = event.getFrom().getPlayers().stream().filter(player1 -> player.getPose() == Pose.SLEEPING).collect(Collectors.toList());
            nightBar.removePlayer(player);
            dayBar.removePlayer(player);
            update(sleepers.size(), (int)Math.ceil((double) event.getFrom().getPlayers().size()/dzielnik));
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
        check(player, sleeping(player), required(player));
    }
    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) // I stole this code from https://github.com/nkomarn/Harbor/blob/f27335e2420a331100978efe96d6b3257353a578/src/main/java/xyz/nkomarn/harbor/listener/BedListener.java#L31
            return;

        Player player = event.getPlayer();
        Bukkit.getServer().getScheduler().runTaskLater(this, ()-> {
            update(sleeping(player), required(player));
            nightBar.setVisible(true);
            if (sleeping(player) >= required(player)) {
                task = Bukkit.getScheduler().runTaskLater(this, ()-> {
                    nightBar.setVisible(false);
                    player.getWorld().setTime(0);
                    dayBar.setVisible(true);
                    for (Player p : player.getWorld().getPlayers())
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);

                    Bukkit.getScheduler().runTaskLater(this, ()-> dayBar.setVisible(false),100);
                },100);
            }
        },1);
    }
    @EventHandler
    public void onWake(PlayerBedLeaveEvent event) {
        Bukkit.getScheduler().runTaskLater(this, ()-> {
            Player player = event.getPlayer();
            check(player, sleeping(player), required(player));
        },1);
    }
}