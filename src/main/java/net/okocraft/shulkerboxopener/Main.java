package net.okocraft.shulkerboxopener;

import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getType() != InventoryType.CRAFTING || event.getAction() != InventoryAction.PICKUP_HALF) {
            return;
        }

        if (!event.getWhoClicked().hasPermission("shulkerboxopener.use")) {
            return;
        }

        ItemStack shulkerBox = event.getCurrentItem();
        if (shulkerBox == null || shulkerBox.getAmount() != 1) {
            return;
        }

        ItemMeta meta = shulkerBox.getItemMeta();
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta im = (BlockStateMeta) meta;
            if (im.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulker = (ShulkerBox) im.getBlockState();
                event.setCancelled(true);
                event.getView().setCursor(null);
                event.getWhoClicked();
                event.getWhoClicked().openInventory(shulker.getInventory());
            }
        }
    }
}
