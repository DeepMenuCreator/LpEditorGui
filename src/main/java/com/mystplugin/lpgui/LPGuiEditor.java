package com.mystplugin.lpgui;

import com.mystplugin.lpgui.gui.GuiManager;
import com.mystplugin.lpgui.listener.ChatInputListener;
import com.mystplugin.lpgui.listener.GuiListener;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LPGuiEditor extends JavaPlugin {

    private static LPGuiEditor instance;

    private LuckPerms luckPerms;
    private GuiManager guiManager;

    /** Игроки, ожидающие ввода текста в чат (например, названия права) */
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("LuckPerms не найден на сервере! Плагин LPGuiEditor отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.luckPerms = LuckPermsProvider.get();
        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        var cmd = getCommand("lpgui");
        if (cmd != null) {
            cmd.setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Эту команду можно использовать только в игре.");
                    return true;
                }
                if (!player.hasPermission("lpgui.use")) {
                    player.sendMessage("§cУ вас нет прав на использование этой команды.");
                    return true;
                }
                player.openInventory(guiManager.mainMenu());
                return true;
            });
        }

        getLogger().info("LPGuiEditor включен.");
    }

    @Override
    public void onDisable() {
        pendingInputs.clear();
        getLogger().info("LPGuiEditor отключен.");
    }

    public static LPGuiEditor getInstance() {
        return instance;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public Map<UUID, PendingInput> getPendingInputs() {
        return pendingInputs;
    }

    /**
     * Описывает, какого рода текстовый ввод в чате ожидается от игрока,
     * и к какой цели (UUID игрока или имя группы) он относится.
     */
    public record PendingInput(Type type, String target) {
        public enum Type {
            ADD_USER_PERMISSION,
            ADD_GROUP_PERMISSION,
            LOAD_OFFLINE_PLAYER
        }
    }
}
