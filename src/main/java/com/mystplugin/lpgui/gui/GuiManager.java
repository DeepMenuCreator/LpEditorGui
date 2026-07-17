package com.mystplugin.lpgui.gui;

import com.mystplugin.lpgui.LPGuiEditor;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Раскладка нижнего ряда (слоты 45-53) везде одинаковая, чтобы не путаться:
 * 45 - назад, 46 - пред. страница, 48/50 - действия, 52 - след. страница
 */
public class GuiManager {

    private static final int PAGE_SIZE = 45;

    private final LPGuiEditor plugin;

    public GuiManager(LPGuiEditor plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------------
    // Главное меню
    // ---------------------------------------------------------------------

    public Inventory mainMenu() {
        Holders.MainMenuHolder holder = new Holders.MainMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, "§8LuckPerms §7» §fГлавное меню");
        holder.setInventory(inv);

        fillBorder(inv);
        inv.setItem(11, item(Material.PLAYER_HEAD, "§aПрава игроков",
                "§7Просмотр и редактирование", "§7прав отдельных игроков"));
        inv.setItem(15, item(Material.WRITTEN_BOOK, "§bПрава групп",
                "§7Просмотр и редактирование", "§7прав групп LuckPerms"));

        return inv;
    }

    // ---------------------------------------------------------------------
    // Список игроков
    // ---------------------------------------------------------------------

    public Inventory playerListMenu(int page) {
        Holders.PlayerListHolder holder = new Holders.PlayerListHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8LuckPerms §7» §fИгроки (стр. " + (page + 1) + ")");
        holder.setInventory(inv);

        List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, online.size());

        int slot = 0;
        for (int i = from; i < to; i++) {
            Player p = online.get(i);
            inv.setItem(slot++, playerHead(p, "§a" + p.getName(), "§7Нажмите, чтобы открыть", "§7его права"));
        }

        inv.setItem(45, item(Material.ARROW, "§7« Назад"));
        addPageNav(inv, page, online.size());
        inv.setItem(49, item(Material.NAME_TAG, "§eВвести имя вручную",
                "§7Загрузить оффлайн-игрока", "§7по нику через чат"));

        return inv;
    }

    // ---------------------------------------------------------------------
    // Список групп
    // ---------------------------------------------------------------------

    public Inventory groupListMenu(int page, java.util.UUID addToUserUuid) {
        Holders.GroupListHolder holder = new Holders.GroupListHolder(page, addToUserUuid);
        String title = addToUserUuid != null
                ? "§8LuckPerms §7» §fВыберите группу"
                : "§8LuckPerms §7» §fГруппы (стр. " + (page + 1) + ")";
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        List<Group> groups = plugin.getLuckPerms().getGroupManager().getLoadedGroups()
                .stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, groups.size());

        int slot = 0;
        for (int i = from; i < to; i++) {
            Group g = groups.get(i);
            inv.setItem(slot++, item(Material.WRITTEN_BOOK, "§b" + g.getName(), "§7Нажмите, чтобы выбрать"));
        }

        inv.setItem(45, item(Material.ARROW, "§7« Назад"));
        addPageNav(inv, page, groups.size());

        return inv;
    }

    // ---------------------------------------------------------------------
    // Права игрока
    // ---------------------------------------------------------------------

    public Inventory userPermissionMenu(User user, int page) {
        Holders.UserPermissionHolder holder =
                new Holders.UserPermissionHolder(user.getUniqueId(), user.getUsername(), page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                "§8LuckPerms §7» §f" + safeName(user.getUsername()) + " (стр. " + (page + 1) + ")");
        holder.setInventory(inv);

        List<Node> nodes = new ArrayList<>(user.getNodes());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, nodes.size());

        int slot = 0;
        for (int i = from; i < to; i++) {
            inv.setItem(slot++, nodeItem(nodes.get(i)));
        }

        inv.setItem(45, item(Material.ARROW, "§7« Назад"));
        addPageNav(inv, page, nodes.size());
        inv.setItem(48, item(Material.LIME_DYE, "§aДобавить право",
                "§7Ввести узел права через чат", "§7Пример: §fsome.permission.node",
                "§7Чтобы выдать false: §f-some.permission.node"));
        inv.setItem(50, item(Material.ENDER_PEARL, "§dДобавить в группу",
                "§7Выбрать группу для", "§7добавления игрока в неё"));

        return inv;
    }

    // ---------------------------------------------------------------------
    // Права группы
    // ---------------------------------------------------------------------

    public Inventory groupPermissionMenu(Group group, int page) {
        Holders.GroupPermissionHolder holder = new Holders.GroupPermissionHolder(group.getName(), page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                "§8LuckPerms §7» §fГруппа " + group.getName() + " (стр. " + (page + 1) + ")");
        holder.setInventory(inv);

        List<Node> nodes = new ArrayList<>(group.getNodes());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, nodes.size());

        int slot = 0;
        for (int i = from; i < to; i++) {
            inv.setItem(slot++, nodeItem(nodes.get(i)));
        }

        inv.setItem(45, item(Material.ARROW, "§7« Назад"));
        addPageNav(inv, page, nodes.size());
        inv.setItem(48, item(Material.LIME_DYE, "§aДобавить право",
                "§7Ввести узел права через чат", "§7Пример: §fsome.permission.node",
                "§7Чтобы выдать false: §f-some.permission.node"));

        return inv;
    }

    // ---------------------------------------------------------------------
    // Вспомогательные методы
    // ---------------------------------------------------------------------

    private void addPageNav(Inventory inv, int page, int totalSize) {
        if (page > 0) {
            inv.setItem(46, item(Material.ARROW, "§e← Предыдущая страница"));
        }
        if ((page + 1) * PAGE_SIZE < totalSize) {
            inv.setItem(52, item(Material.ARROW, "§e→ Следующая страница"));
        }
    }

    private ItemStack nodeItem(Node node) {
        boolean isGroupNode = node.getType() == NodeType.INHERITANCE;
        Material mat = isGroupNode ? Material.ENDER_PEARL
                : (node.getValue() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);

        String color = node.getValue() ? "§a" : "§c";
        return item(mat, color + node.getKey(),
                "§7Значение: " + (node.getValue() ? "§atrue" : "§cfalse"),
                "§8Shift+ЛКМ — удалить это право",
                "§8ЛКМ — показать полностью в чат");
    }

    private ItemStack playerHead(OfflinePlayer player, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private String safeName(String name) {
        return name == null ? "Неизвестно" : name;
    }
}
