package net.okocraft.shulkerboxopener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    /**
     * Key: shulker box inventory, value: NOT raw slot of shulker box item.
     */
    private final Map<Inventory, Integer> shulkerBoxSlots = new ConcurrentHashMap<>();

    private final Map<UUID, InventoryView> previousInventoryViews = new ConcurrentHashMap<>();

    private final Set<UUID> apiChestOpening = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getView().getTopInventory();
        Player player = (Player) event.getPlayer();

        // play chest close animation.
        if (apiChestOpening.remove(player.getUniqueId())) {
            InventoryHolder holder = inv.getHolder();
            if (holder instanceof Chest chest) {
                chest.close();
            } else if (holder instanceof DoubleChest doubleChest) {
                if (doubleChest.getRightSide() instanceof Chest chest) {
                    chest.close();
                }
                if (doubleChest.getLeftSide() instanceof Chest chest) {
                    chest.close();
                }
            }
        }

        // not opening shulker box
        if (shulkerBoxSlots.remove(inv) == null) {
            return;
        }

        player.updateInventory();

        // if player opened inventory which can be re-opened.
        InventoryView previousInventoryView = previousInventoryViews.remove(player.getUniqueId());
        if (previousInventoryView != null) {
            player.getScheduler().run(this, t -> {
                // play open chest animation when player open previous cached chest.
                InventoryHolder holder = previousInventoryView.getTopInventory().getHolder();
                if (holder instanceof Chest chest) {
                    chest.open();
                    apiChestOpening.add(player.getUniqueId());
                } else if (holder instanceof DoubleChest doubleChest) {
                    if (doubleChest.getRightSide() instanceof Chest chest) {
                        chest.open();
                    }
                    if (doubleChest.getLeftSide() instanceof Chest chest) {
                        chest.open();
                    }
                    apiChestOpening.add(player.getUniqueId());
                }

                player.openInventory(previousInventoryView);
            }, null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getType() == InventoryType.CRAFTING) {
            handleShulkerBoxOpen(event);
        } else if (event.getView().getType() == InventoryType.SHULKER_BOX) {
            handleShulkerBoxClick(event);
        } else {
            if (handleShulkerBoxOpen(event)) {
                previousInventoryViews.put(event.getWhoClicked().getUniqueId(), event.getView());
            }
        }
    }

    private boolean handleShulkerBoxOpen(InventoryClickEvent event) {
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            return false;
        }
        if (event.getAction() != InventoryAction.PICKUP_HALF) {
            return false;
        }
        if (!event.getWhoClicked().hasPermission("shulkerboxopener.use")) {
            return false;
        }

        ItemStack shulkerBoxItem = event.getCurrentItem();
        if (shulkerBoxItem == null || shulkerBoxItem.getAmount() != 1) {
            return false;
        }

        if (shulkerBoxItem.getItemMeta() instanceof BlockStateMeta im
                && im.getBlockState() instanceof ShulkerBox shulkerBoxState) {
            event.setCancelled(true);
            event.getView().setCursor(null);
            Inventory inv = shulkerBoxState.getInventory();
            shulkerBoxSlots.put(inv, event.getSlot());
            event.getWhoClicked().openInventory(inv);
            return true;
        }

        return false;
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
        if (shulkerBoxItem == null || !(shulkerBoxItem.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox)) {
            return;
        }

        setShulkerItemInventory(clicker, shulkerBoxItem, inv);
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
        if (shulkerBoxItem == null || !(shulkerBoxItem.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox)) {
            return;
        }

        setShulkerItemInventory(clicker, shulkerBoxItem, inv);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private void onPlayerMove(PlayerMoveEvent event) {
        if (shulkerBoxSlots.isEmpty()) {
            return;
        }

        // Do not move when opening shulkerbox in inventory to prevent folia region merge do bad things.

        boolean opening = false;
        for (Inventory inv : shulkerBoxSlots.keySet()) {
            if (inv.getViewers().contains(event.getPlayer())) {
                opening = true;
                break;
            }
        }
        // prevent removing entry in loop.
        if (opening) {
            event.getPlayer().closeInventory();
        }
    }

    private void setShulkerItemInventory(HumanEntity inventoryHolder, ItemStack shulkerBoxItem, Inventory newInventory) {
        inventoryHolder.getScheduler().run(this, t -> {
            if (shulkerBoxItem.getItemMeta() instanceof BlockStateMeta meta
                    && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
                shulkerBox.getInventory().setContents(newInventory.getContents());
                meta.setBlockState(shulkerBox);
                shulkerBoxItem.setItemMeta(meta);
            }
        }, null);
    }
}
