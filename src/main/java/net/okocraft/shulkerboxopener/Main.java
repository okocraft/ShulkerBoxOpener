package net.okocraft.shulkerboxopener;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    /**
     * Key: shulker box inventory, value: NOT raw slot of shulker box item.
     */
    private final Map<Inventory, Integer> shulkerBoxSlots = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        shulkerBoxSlots.remove(event.getView().getTopInventory());
        ((Player) event.getPlayer()).updateInventory();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getType() == InventoryType.CRAFTING) {
            handleShulkerBoxOpen(event);
        } if (event.getView().getType() == InventoryType.SHULKER_BOX) {
            handleShulkerBoxClick(event);
        }
    }

    private void handleShulkerBoxOpen(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.PICKUP_HALF) {
            return;
        }
        if (!event.getWhoClicked().hasPermission("shulkerboxopener.use")) {
            return;
        }

        ItemStack shulkerBoxItem = event.getCurrentItem();
        if (shulkerBoxItem == null || shulkerBoxItem.getAmount() != 1) {
            return;
        }

        if (shulkerBoxItem.getItemMeta() instanceof BlockStateMeta im
                && im.getBlockState() instanceof ShulkerBox shulkerBoxState) {
            event.setCancelled(true);
            event.getView().setCursor(null);
            Inventory inv = shulkerBoxState.getInventory();
            shulkerBoxSlots.put(inv, event.getSlot());
            event.getWhoClicked().openInventory(inv);
        }
    }

    private void handleShulkerBoxClick(InventoryClickEvent event) {
        Inventory inv = event.getView().getTopInventory();
        if (!shulkerBoxSlots.containsKey(inv)) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        if (!clicker.hasPermission("shulkerboxopener.use")) {
            event.setCancelled(true);
            return;
        }

        int shulkerSlot = shulkerBoxSlots.get(inv);
        if(event.getRawSlot() >= inv.getSize() && event.getSlot() == shulkerSlot) {
            event.setCancelled(true);
            return;
        }

        ItemStack shulkerBoxItem = event.getView().getBottomInventory().getItem(shulkerSlot);
        if (shulkerBoxItem == null || shulkerBoxItem.getType() != Material.SHULKER_BOX) {
            return;
        }

        setShulkerItemInventory(shulkerBoxItem, inv);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getView().getTopInventory();
        if (!shulkerBoxSlots.containsKey(inv)) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        if (!clicker.hasPermission("shulkerboxopener.use")) {
            event.setCancelled(true);
            return;
        }

        ItemStack shulkerBoxItem = event.getView().getBottomInventory().getItem(shulkerBoxSlots.get(inv));
        if (shulkerBoxItem == null || shulkerBoxItem.getType() != Material.SHULKER_BOX) {
            return;
        }

        setShulkerItemInventory(shulkerBoxItem, inv);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onBlockPlace(BlockPlaceEvent event) {
        // Can this really happen?
        InventoryView invView = event.getPlayer().getOpenInventory();
        if (invView.getType() != InventoryType.SHULKER_BOX) {
            return;
        }

        Integer rawSlot = shulkerBoxSlots.get(invView.getTopInventory());
        if (rawSlot == null) {
            return;
        }

        ItemStack shulkerBoxItem = invView.getBottomInventory().getItem(rawSlot);
        if (event.getItemInHand().equals(shulkerBoxItem)) {
            event.getPlayer().closeInventory();
            event.setCancelled(true);
        }
    }

    private void setShulkerItemInventory(ItemStack shulkerBoxItem, Inventory newInventory) {
        getServer().getScheduler().runTask(this, () -> {
            if (shulkerBoxItem.getItemMeta() instanceof BlockStateMeta meta
                    && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
                shulkerBox.getInventory().setContents(newInventory.getContents());
                meta.setBlockState(shulkerBox);
                shulkerBoxItem.setItemMeta(meta);
            }
        });
    }
}
