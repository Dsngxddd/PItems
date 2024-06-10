package net.cengiz1.pitems;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PItems extends JavaPlugin implements Listener {

    private Economy economy;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault bulunamadı! Plugin devre dışı bırakılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig(); 
        reloadConfig();
        getLogger().info("Plugin etkinleştirildi!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("pitems")) {
                if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
                    String itemName = args[1];
                    int level = Integer.parseInt(args[2]);

                    ItemStack item = createItem(itemName, level);
                    if (item != null) {
                        player.getInventory().addItem(item);
                        player.sendMessage(ChatColor.GREEN + "Öğe başarıyla verildi: " + itemName + " - Seviye " + level);
                    } else {
                        player.sendMessage(ChatColor.RED + "Belirtilen öğe bulunamadı veya geçersiz seviye!");
                    }
                    return true;
                }
            }
        } else {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir!");
            return false;
        }
        return false;
    }

    private ItemStack createItem(String itemName, int level) {
        ConfigurationSection itemSection = getConfig().getConfigurationSection("items." + itemName.toLowerCase().replace(" ", ""));
        if (itemSection != null) {
            int maxLevel = itemSection.getInt("maxLevel", 1);
            if (level > 0 && level <= maxLevel) {
                ItemStack itemStack = new ItemStack(Material.valueOf(Objects.requireNonNull(itemSection.getString("material"))));
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    String displayName = itemSection.getString("name");
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    List<String> lore = Arrays.asList("Level " + level);
                    meta.setLore(lore);
                    itemStack.setItemMeta(meta);
                    return itemStack;
                }
            }
        }
        return null;
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    private void upgradeItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() != Material.AIR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String itemName = ChatColor.stripColor(meta.getDisplayName()); 
                ConfigurationSection itemSection = getConfig().getConfigurationSection("items." + itemName.toLowerCase().replace(" ", ""));
                if (itemSection != null) {
                    int currentLevel = getCurrentLevelFromLore(meta.getLore()); // Mevcut seviyeyi al
                    int maxLevel = itemSection.getInt("maxLevel", 1);
                    if (currentLevel < maxLevel) {
                        int nextLevel = currentLevel + 1;
                        double cost = itemSection.getDouble("levels." + nextLevel + ".cost");
                        if (economy.has(player, cost)) {
                            double damage = itemSection.getDouble("levels." + nextLevel + ".damage");
                            economy.withdrawPlayer(player, cost);
                            List<String> lore = meta.getLore();
                            if (lore != null) {
                                lore.set(0, "Level " + nextLevel); // Lore'un ilk satırını güncelle
                            }
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                            player.getInventory().setItemInMainHand(item); // Ana eldeki itemi güncelle
                            player.updateInventory();
                            player.sendMessage(ChatColor.GREEN + "Öğe başarıyla seviye " + nextLevel + " yapıldı.");
                            player.sendMessage(ChatColor.GREEN + "Öğe fiyatı " + cost + " aldınız.");
                            player.sendMessage(ChatColor.GREEN + "Öğe damage'i " + damage + " yapıldı.");
                        } else {
                            player.sendMessage(ChatColor.RED + "Yükseltme için yeterli paranız yok!");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Bu öğenin maksimum seviyesine ulaştınız!");
                    }
                }
            }
        }
    }

    // Öğenin mevcut seviyesini lore'dan al
    private int getCurrentLevelFromLore(List<String> lore) {
        if (lore != null && !lore.isEmpty()) {
            for (String line : lore) {
                if (line.startsWith("Level ")) {
                    try {
                        return Integer.parseInt(line.split(" ")[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }
}
