package com.mystplugin.lpgui.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Набор InventoryHolder-классов, которые позволяют GuiListener понять,
 * какое именно меню открыто у игрока и с какими данными оно связано.
 */
public class Holders {

    public static class MainMenuHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }

    public static class PlayerListHolder implements InventoryHolder {
        private Inventory inventory;
        public final int page;
        public PlayerListHolder(int page) { this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }

    /**
     * Список групп. Если addToUserUuid не null — значит меню открыто, чтобы
     * добавить конкретного игрока в выбранную группу (а не просто редактировать группу).
     */
    public static class GroupListHolder implements InventoryHolder {
        private Inventory inventory;
        public final int page;
        public final UUID addToUserUuid;
        public GroupListHolder(int page, UUID addToUserUuid) {
            this.page = page;
            this.addToUserUuid = addToUserUuid;
        }
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }

    public static class UserPermissionHolder implements InventoryHolder {
        private Inventory inventory;
        public final UUID uuid;
        public final String name;
        public final int page;
        public UserPermissionHolder(UUID uuid, String name, int page) {
            this.uuid = uuid;
            this.name = name;
            this.page = page;
        }
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }

    public static class GroupPermissionHolder implements InventoryHolder {
        private Inventory inventory;
        public final String groupName;
        public final int page;
        public GroupPermissionHolder(String groupName, int page) {
            this.groupName = groupName;
            this.page = page;
        }
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
    }
}
