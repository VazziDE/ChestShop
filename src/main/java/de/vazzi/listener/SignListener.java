package de.vazzi.listener;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.math.BlockFace;
import de.vazzi.ChestShop;
import me.onebone.economyapi.EconomyAPI;

import java.util.Map;

public class SignListener implements Listener {

    public final String SIGN_CREATE = "shop";
    public final String SIGN_PREFIX = "§f[§bChestShop§f]";


    @EventHandler
    public void playerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ChestShop.getInstance().setPlayerCooldown(player.getName());

    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        ChestShop.getInstance().removeCooldown(player.getName());

    }

    @EventHandler
    public void createShopSign(SignChangeEvent event) {
        if (!event.isCancelled()) {
            Block sign = event.getBlock();
            if (sign.getId() == Block.WALL_SIGN) {
                if (event.getLine(0).equalsIgnoreCase(SIGN_CREATE)) {
                    if (isNumeric(event.getLine(1)) && isNumeric(event.getLine(2)) && isNumeric(event.getLine(3))) {
                        int itemid = Integer.parseInt(event.getLine(1));
                        int itemcount = Integer.parseInt(event.getLine(2));
                        int itemprice = Integer.parseInt(event.getLine(3));

                        Item shopitem = Item.get(itemid);

                        if (shopitem.isNull()) {
                            event.getPlayer().sendMessage(SIGN_PREFIX + " §cItem with ID §f" + itemid + " §cnot found");
                            return;
                        }
                        if (itemcount < 1) {
                            event.getPlayer().sendMessage(SIGN_PREFIX + " §cThe Item Count can not be lower then 1");
                            return;
                        }
                        if (itemprice < 0) {
                            event.getPlayer().sendMessage(SIGN_PREFIX + " §cThe Item Price can not be lower then 0");
                            return;
                        }

                        event.setLine(0, SIGN_PREFIX);
                        event.setLine(1, "§a" + itemcount + "x §e" + shopitem.getName());
                        event.setLine(2, "§e" + itemprice + "$");
                        event.setLine(3, "§f" + event.getPlayer().getName());

                    }

                }
            }
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (event.getAction().equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)) {
            if (block.getId() == Block.WALL_SIGN) {
                BlockEntity signEntity = block.getLevel().getBlockEntity(block);
                if (signEntity instanceof BlockEntitySign) {
                    String[] text = ((BlockEntitySign) signEntity).getText();
                    if (text[0].equals(SIGN_PREFIX)) {
                        if (!ChestShop.getInstance().inCooldown(player.getName())) {
                            if (getChest((BlockEntitySign) signEntity) != null) {
                                BlockEntityChest entityChest = getChest((BlockEntitySign) signEntity);
                                String[] secondLine = text[1].split("x");
                                String count = secondLine[0].replace("§a", "").replace("x", "");
                                String itemname = secondLine[1].replace("§e", "").replaceFirst(" ", "");
                                String price = text[2].replace("§e", "").replace("$", "");
                                String owner = text[3].replace("§f", "");
                                if (isValidate(player, count, itemname, price, owner)) {
                                    if (EconomyAPI.getInstance().myMoney(player) >= Integer.parseInt(price)) {
                                        if (buyFromShop(player, entityChest, itemname, Integer.parseInt(count), price)) {
                                            notifyOwner(owner, player.getName(), itemname, count, price);
                                        }
                                        ChestShop.getInstance().setPlayerCooldown(player.getName());
                                    } else {
                                        event.getPlayer().sendMessage(SIGN_PREFIX + " §cYou do not have enough money.");
                                    }
                                }
                            }


                        } else {
                            event.getPlayer().sendMessage(SIGN_PREFIX + " §cSomething went wrong.");
                        }
                    }
                }
            }
        }
    }


    public BlockEntityChest getChest(BlockEntitySign sign) {
        Block chestBlock = sign.getBlock().getSide(BlockFace.fromIndex(sign.getBlock().getDamage()).getOpposite());
        if (chestBlock.getId() == Block.CHEST) {
            BlockEntity chestEntity = chestBlock.getLevel().getBlockEntity(chestBlock);
            if (chestEntity instanceof BlockEntityChest) {
                return (BlockEntityChest) chestEntity;
            }
        }

        return null;
    }

    public boolean isValidate(Player player, String count, String itemname, String price, String owner) {
        if (!count.equals("") && !itemname.equals("") && !price.equals("") && !owner.equals("")) {
            if (!owner.equals(player.getName())) {
                if (Integer.parseInt(count) > 0) {
                    if (!Item.fromString(itemname).isNull()) {
                        return Integer.parseInt(price) >= 0;
                    }
                }
            } else {
                player.sendMessage(SIGN_PREFIX + " §cYou can not buy at your own chestshop");
            }
        }
        return false;
    }

    public boolean buyFromShop(Player player, BlockEntityChest entityChest, String itemname,
                               int count, String price) {
        int shopItemcount = 0;
        for (Item item : entityChest.getInventory().getContents().values()) {
            if (item.getName().equals(Item.fromString(itemname).getName()) && item.getCount() > 0) {
                shopItemcount += item.getCount();
            }
        }
        if (shopItemcount >= count) {

            Item chestItem = Item.fromString(itemname);
            chestItem.setCount(count);
            int already = 0;
            for (Map.Entry<Integer, Item> item : entityChest.getInventory().getContents().entrySet()) {
                if (item.getValue().getName().equals(Item.fromString(itemname).getName())) {
                    Item slotitem = entityChest.getInventory().getItem(item.getKey());
                    if (slotitem.getCount() + already >= count) {
                        int rest = slotitem.getCount() - (count - already);
                        slotitem.setCount(rest);
                        entityChest.getInventory().setItem(item.getKey(), slotitem);
                        break;
                    } else {
                        already += slotitem.getCount();
                        entityChest.getInventory().setItem(item.getKey(), Item.get(Item.AIR));
                    }

                }
            }
            player.getInventory().addItem(chestItem);
            EconomyAPI.getInstance().reduceMoney(player, Double.parseDouble(price));
            player.sendMessage(SIGN_PREFIX + " §aYou succesfully buyed " + count + "x " + itemname + " for " + price + "$.");
            return true;
        } else {
            player.sendMessage(SIGN_PREFIX + " §cThe ChestShop has not enough items.");
            return false;
        }

    }

    public void notifyOwner(String owner, String player, String itemname, String count, String
            price) {
        EconomyAPI.getInstance().addMoney(owner, Double.parseDouble(price));
        if (ChestShop.getInstance().getServer().getPlayer(owner) != null) {
            Player ownerPlayer = ChestShop.getInstance().getServer().getPlayer(owner);
            ownerPlayer.sendMessage(SIGN_PREFIX + " §7" + player + " has bought at your ChestShop "
                    + count + "x " + itemname + " for " + price + "$.");
        }
    }

    public boolean isNumeric(String text) {
        for (char c : text.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

}
