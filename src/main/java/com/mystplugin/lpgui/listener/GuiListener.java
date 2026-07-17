package com.mystplugin.lpgui.listener;

import com.mystplugin.lpgui.LPGuiEditor;
import com.mystplugin.lpgui.gui.Holders;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class GuiListener implements Listener {

    private final LPGuiEditor plugin;

    public GuiListener(LPGuiEditor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder == null) return;

        boolean isOurHolder = holder instanceof Holders.MainMenuHolder
                || holder instanceof Holders.PlayerListHolder
                || holder instanceof Holders.GroupListHolder
                || holder instanceof Holders.UserPermissionHolder
                || holder instanceof Holders.GroupPermissionHolder;
        if (!isOurHolder) return;

        // клики по инвентарю самого игрока (нижняя часть экрана) игнорируем
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != holder) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        if (holder instanceof Holders.MainMenuHolder) {
            handleMainMenu(player, event.getSlot());
        } else if (holder instanceof Holders.PlayerListHolder h) {
            handlePlayerList(player, event, h);
        } else if (holder instanceof Holders.GroupListHolder h) {
            handleGroupList(player, event, h);
        } else if (holder instanceof Holders.UserPermissionHolder h) {
            handleUserPermission(player, event, h);
        } else if (holder instanceof Holders.GroupPermissionHolder h) {
            handleGroupPermission(player, event, h);
        }
    }

    private void handleMainMenu(Player player, int slot) {
        if (slot == 11) {
            player.openInventory(plugin.getGuiManager().playerListMenu(0));
        } else if (slot == 15) {
            player.openInventory(plugin.getGuiManager().groupListMenu(0, null));
        }
    }

    private void handlePlayerList(Player player, InventoryClickEvent event, Holders.PlayerListHolder h) {
        int slot = event.getSlot();

        switch (slot) {
            case 45 -> player.openInventory(plugin.getGuiManager().mainMenu());
            case 46 -> player.openInventory(plugin.getGuiManager().playerListMenu(h.page - 1));
            case 52 -> player.openInventory(plugin.getGuiManager().playerListMenu(h.page + 1));
            case 49 -> promptChat(player, LPGuiEditor.PendingInput.Type.LOAD_OFFLINE_PLAYER, "",
                    "§eВведите в чат ник игрока, чьи права нужно открыть:");
            default -> openUserFromHead(player, event.getCurrentItem());
        }
    }

    private void openUserFromHead(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta) || skullMeta.getOwningPlayer() == null) return;

        UUID targetUuid = skullMeta.getOwningPlayer().getUniqueId();
        plugin.getLuckPerms().getUserManager().loadUser(targetUuid).thenAcceptAsync(user ->
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.openInventory(plugin.getGuiManager().userPermissionMenu(user, 0))
                )
        );
    }

    private void handleGroupList(Player player, InventoryClickEvent event, Holders.GroupListHolder h) {
        int slot = event.getSlot();

        if (slot == 45) {
            if (h.addToUserUuid != null) {
                plugin.getLuckPerms().getUserManager().loadUser(h.addToUserUuid).thenAcceptAsync(user ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                player.openInventory(plugin.getGuiManager().userPermissionMenu(user, 0))));
            } else {
                player.openInventory(plugin.getGuiManager().mainMenu());
            }
            return;
        }
        if (slot == 46) {
            player.openInventory(plugin.getGuiManager().groupListMenu(h.page - 1, h.addToUserUuid));
            return;
        }
        if (slot == 52) {
            player.openInventory(plugin.getGuiManager().groupListMenu(h.page + 1, h.addToUserUuid));
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;
        String groupName = stripColor(clicked.getItemMeta().getDisplayName());
        if (groupName.isBlank()) return;

        Group group = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
        if (group == null) return;

        if (h.addToUserUuid != null) {
            plugin.getLuckPerms().getUserManager().loadUser(h.addToUserUuid).thenAcceptAsync(user -> {
                user.data().add(InheritanceNode.builder(group.getName()).build());
                plugin.getLuckPerms().getUserManager().saveUser(user);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§aИгрок " + user.getUsername() + " добавлен в группу " + group.getName() + ".");
                    player.openInventory(plugin.getGuiManager().userPermissionMenu(user, 0));
                });
            });
        } else {
            player.openInventory(plugin.getGuiManager().groupPermissionMenu(group, 0));
        }
    }

    private void handleUserPermission(Player player, InventoryClickEvent event, Holders.UserPermissionHolder h) {
        int slot = event.getSlot();

        switch (slot) {
            case 45 -> {
                player.openInventory(plugin.getGuiManager().playerListMenu(0));
                return;
            }
            case 46 -> {
                reopenUserMenu(player, h.uuid, h.page - 1);
                return;
            }
            case 52 -> {
                reopenUserMenu(player, h.uuid, h.page + 1);
                return;
            }
            default -> { /* обрабатываем ниже */ }
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;
        String displayName = clicked.getItemMeta().getDisplayName();

        if (slot == 48) {
            promptChat(player, LPGuiEditor.PendingInput.Type.ADD_USER_PERMISSION, h.uuid.toString(),
                    "§eВведите право в чат (например: some.permission.node, или -some.permission.node для false):");
            return;
        }
        if (slot == 50) {
            player.openInventory(plugin.getGuiManager().groupListMenu(0, h.uuid));
            return;
        }

        if (slot < 45) {
            handleNodeClick(player, event, h.uuid, null, h.page);
        }
    }

    private void handleGroupPermission(Player player, InventoryClickEvent event, Holders.GroupPermissionHolder h) {
        int slot = event.getSlot();

        if (slot == 45) {
            player.openInventory(plugin.getGuiManager().groupListMenu(0, null));
            return;
        }
        if (slot == 46 || slot == 52) {
            Group group = plugin.getLuckPerms().getGroupManager().getGroup(h.groupName);
            if (group != null) {
                int newPage = slot == 46 ? h.page - 1 : h.page + 1;
                player.openInventory(plugin.getGuiManager().groupPermissionMenu(group, newPage));
            }
            return;
        }
        if (slot == 48) {
            promptChat(player, LPGuiEditor.PendingInput.Type.ADD_GROUP_PERMISSION, h.groupName,
                    "§eВведите право в чат (например: some.permission.node, или -some.permission.node для false):");
            return;
        }

        if (slot < 45) {
            handleNodeClick(player, event, null, h.groupName, h.page);
        }
    }

    private void handleNodeClick(Player player, InventoryClickEvent event, UUID userUuid, String groupName, int page) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;
        String nodeKey = stripColor(clicked.getItemMeta().getDisplayName());
        if (nodeKey.isBlank()) return;

        if (!event.getClick().isShiftClick()) {
            player.sendMessage("§7Право: §f" + nodeKey);
            return;
        }

        if (userUuid != null) {
            plugin.getLuckPerms().getUserManager().loadUser(userUuid).thenAcceptAsync(user -> {
                removeMatchingNode(user.data().toCollection(), nodeKey, node -> {
                    user.data().remove(node);
                    plugin.getLuckPerms().getUserManager().saveUser(user);
                });
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cПраво удалено: §f" + nodeKey);
                    player.openInventory(plugin.getGuiManager().userPermissionMenu(user, page));
                });
            });
        } else if (groupName != null) {
            Group group = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
            if (group == null) return;
            removeMatchingNode(group.data().toCollection(), nodeKey, node -> {
                group.data().remove(node);
                plugin.getLuckPerms().getGroupManager().saveGroup(group);
            });
            player.sendMessage("§cПраво удалено: §f" + nodeKey);
            player.openInventory(plugin.getGuiManager().groupPermissionMenu(group, page));
        }
    }

    private void removeMatchingNode(Collection<Node> nodes, String key, Consumer<Node> onFound) {
        for (Node node : nodes) {
            if (node.getKey().equals(key)) {
                onFound.accept(node);
                return;
            }
        }
    }

    private void reopenUserMenu(Player player, UUID uuid, int page) {
        User cached = plugin.getLuckPerms().getUserManager().getUser(uuid);
        if (cached != null) {
            player.openInventory(plugin.getGuiManager().userPermissionMenu(cached, page));
        } else {
            plugin.getLuckPerms().getUserManager().loadUser(uuid).thenAcceptAsync(user ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.openInventory(plugin.getGuiManager().userPermissionMenu(user, page))));
        }
    }

    private void promptChat(Player player, LPGuiEditor.PendingInput.Type type, String target, String message) {
        player.closeInventory();
        plugin.getPendingInputs().put(player.getUniqueId(), new LPGuiEditor.PendingInput(type, target));
        player.sendMessage(message);
        player.sendMessage("§7(напишите §fотмена§7, чтобы отменить)");
    }

    private String stripColor(String s) {
        return org.bukkit.ChatColor.stripColor(s);
    }
}
