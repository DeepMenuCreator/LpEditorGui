package com.mystplugin.lpgui.listener;

import com.mystplugin.lpgui.LPGuiEditor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatInputListener implements Listener {

    private final LPGuiEditor plugin;

    public ChatInputListener(LPGuiEditor plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        LPGuiEditor.PendingInput pending = plugin.getPendingInputs().get(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        plugin.getPendingInputs().remove(player.getUniqueId());

        String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (raw.equalsIgnoreCase("отмена") || raw.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§7Отменено."));
            return;
        }

        switch (pending.type()) {
            case ADD_USER_PERMISSION -> handleAddUserPermission(player, UUID.fromString(pending.target()), raw);
            case ADD_GROUP_PERMISSION -> handleAddGroupPermission(player, pending.target(), raw);
            case LOAD_OFFLINE_PLAYER -> handleLoadOfflinePlayer(player, raw);
        }
    }

    private void handleAddUserPermission(Player player, UUID uuid, String raw) {
        Node node = parseNode(raw);
        if (node == null) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cПустой ввод, право не добавлено."));
            return;
        }

        plugin.getLuckPerms().getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            user.data().add(node);
            plugin.getLuckPerms().getUserManager().saveUser(user);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§aПраво добавлено: §f" + node.getKey() + " §7(" + node.getValue() + ")");
                player.openInventory(plugin.getGuiManager().userPermissionMenu(user, 0));
            });
        });
    }

    private void handleAddGroupPermission(Player player, String groupName, String raw) {
        Node node = parseNode(raw);
        if (node == null) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cПустой ввод, право не добавлено."));
            return;
        }

        Group group = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
        if (group == null) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cГруппа не найдена."));
            return;
        }

        group.data().add(node);
        plugin.getLuckPerms().getGroupManager().saveGroup(group);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage("§aПраво добавлено: §f" + node.getKey() + " §7(" + node.getValue() + ")");
            player.openInventory(plugin.getGuiManager().groupPermissionMenu(group, 0));
        });
    }

    private void handleLoadOfflinePlayer(Player player, String name) {
        if (name.isBlank()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cНик не может быть пустым."));
            return;
        }

        plugin.getLuckPerms().getUserManager().lookupUniqueId(name).thenAcceptAsync(uuid -> {
            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cИгрок §f" + name + " §cне найден (он ни разу не заходил на сервер)."));
                return;
            }
            plugin.getLuckPerms().getUserManager().loadUser(uuid).thenAcceptAsync(user ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.openInventory(plugin.getGuiManager().userPermissionMenu(user, 0))));
        });
    }

    /**
     * Разбирает строку вида "some.permission.node" (value = true)
     * или "-some.permission.node" (value = false).
     */
    private Node parseNode(String raw) {
        if (raw.isBlank()) return null;
        boolean value = true;
        String key = raw;
        if (key.startsWith("-")) {
            value = false;
            key = key.substring(1);
        } else if (key.startsWith("+")) {
            key = key.substring(1);
        }
        key = key.trim();
        if (key.isEmpty()) return null;

        return PermissionNode.builder(key).value(value).build();
    }
}
