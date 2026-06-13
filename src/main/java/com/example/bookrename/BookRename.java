package com.example.bookrename;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookRename extends JavaPlugin implements Listener {

    private final Map<UUID, ItemStack> pendingItems = new HashMap<>();
    private final Map<UUID, Integer> pendingSlots = new HashMap<>();

    private static final Component MSG_HOLD_ITEM =
            Component.text("Hold an item to rename!", NamedTextColor.RED);
    private static final Component MSG_WRITE_BOOK =
            Component.text("Write your new item name in the book and sign it!", NamedTextColor.GREEN);
    private static final Component MSG_HINT =
            Component.text("(Only the first page will be used as the name)", NamedTextColor.GRAY);
    private static final Component MSG_EMPTY =
            Component.text("Name can't be empty! Try again.", NamedTextColor.RED);
    private static final Component MSG_RETURNED =
            Component.text("BookRename: Your item was returned.", NamedTextColor.YELLOW);

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BookRename enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(MSG_HOLD_ITEM);
            return true;
        }

        pendingItems.put(player.getUniqueId(), hand.clone());
        pendingSlots.put(player.getUniqueId(), player.getInventory().getHeldItemSlot());

        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        player.getInventory().setItemInMainHand(book);

        player.sendMessage(MSG_WRITE_BOOK);
        player.sendMessage(MSG_HINT);
        return true;
    }

    @EventHandler
    public void onBookSign(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!pendingItems.containsKey(uuid)) return;

        BookMeta bookMeta = event.getNewBookMeta();
        String rawName = bookMeta.getPageCount() > 0 ? bookMeta.getPage(1).trim() : "";

        if (rawName.isEmpty()) {
            player.sendMessage(MSG_EMPTY);
            return;
        }

        Component nameComponent = MiniMessage.miniMessage().deserialize(rawName);

        ItemStack originalItem = pendingItems.get(uuid);
        ItemMeta meta = originalItem.getItemMeta();
        meta.displayName(nameComponent);
        originalItem.setItemMeta(meta);

        int slot = pendingSlots.getOrDefault(uuid, player.getInventory().getHeldItemSlot());
        player.getInventory().setItem(slot, originalItem);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                if (item.getItemMeta() instanceof BookMeta bm) {
                    if (bm.hasAuthor() && bm.getPageCount() > 0
                            && bm.getPage(1).trim().equals(rawName)) {
                        item.setAmount(0);
                        break;
                    }
                }
            }
        }

        pendingItems.remove(uuid);
        pendingSlots.remove(uuid);

        player.sendMessage(Component.text("Item renamed to: ", NamedTextColor.GREEN)
                .append(nameComponent));
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, ItemStack> entry : pendingItems.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.getInventory().addItem(entry.getValue());
                player.sendMessage(MSG_RETURNED);
            }
        }
        pendingItems.clear();
        pendingSlots.clear();
    }
}
