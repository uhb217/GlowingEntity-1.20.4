package net.uhb217.playertrackermod.gui;

import com.mojang.authlib.GameProfile;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WPlainPanel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.uhb217.playertrackermod.PlayerTrackerMod;
import net.uhb217.playertrackermod.gui.widgeds.Button;
import net.uhb217.playertrackermod.gui.widgeds.SlotButton;
import net.uhb217.playertrackermod.gui.widgeds.Sprite;
import net.uhb217.playertrackermod.utils.IEntityDataSaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class GUI extends LightweightGuiDescription {
    private final Text OFF = Text.literal("Off").formatted(Formatting.RED);
    private final Text ON = Text.literal("On").formatted(Formatting.GREEN);

    public GUI(MinecraftClient client) {
        NbtCompound nbt = ((IEntityDataSaver) client.player).getPersistentData();
        WPlainPanel root = new WPlainPanel().setInsets(new Insets(0, 0, 0, 0));
        this.setRootPanel(root);
        root.setSize(144, 190);

        createPlayerTrackerModule(nbt, root, client);
    }

    public void createPlayerTrackerModule(NbtCompound nbt, WPlainPanel root, MinecraftClient mc) {
        Sprite playerTracker = new Sprite(new Texture(new Identifier(PlayerTrackerMod.MOD_ID, "textures/gui/player_tracker.png"))).setToolTip(Text.literal("Player Tracker").formatted(Formatting.DARK_RED));
        root.add(playerTracker, 5, 31);
        WToggleButton button = new WToggleButton(Text.literal("Off").formatted(Formatting.RED));
        if (nbt.contains("player_tracker") && nbt.getBoolean("player_tracker")) {
            button.setToggle(true);
            button.setLabel(ON);
        }
        button.setOnToggle(aBoolean -> {
            if (aBoolean) {
                button.setLabel(ON);
                mc.setScreen(new TestScreen(new PlayerTrackerSetTargetGUI(mc)).setParent(new TestScreen(this)));
            } else {
                button.setLabel(OFF);
                nbt.putBoolean("player_tracker", false);
                mc.player.sendMessage(Text.literal("§6Player Tracker: §3The compass target set to: Normal"));
            }
        });
        root.add(button, 28, 32);
    }

    public static class PlayerTrackerSetTargetGUI extends LightweightGuiDescription {
        private int page = 0;
        public PlayerTrackerSetTargetGUI(MinecraftClient mc) {
            WPlainPanel root = new WPlainPanel().setInsets(new Insets(0, 0, 0, 0));
            this.setRootPanel(root);
            root.setSize(118, 70);

            ArrayList<SlotButton> buttons = new ArrayList<>();
            int index = 0;
            ItemStack[] playerHeads = getPLayersHeads(mc);
            String[] playerNames = getPLayersNames(mc);
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 3; j++) {
                    buttons.add(new SlotButton());
                    buttons.get(index).setSize(25,25);
                    NbtCompound nbt = ((IEntityDataSaver)mc.player).getPersistentData();
                    if((!(i + j + 1 > getPLayersHeads(mc).length)) && getPLayersNames(mc).length > 1){
                        String playerName = playerNames[index];
                        buttons.get(index).setToolTip(Text.literal(playerName).formatted(Formatting.BLUE)).setIcon(new ItemIcon(playerHeads[index]));
                        buttons.get(index).setOnClick(()-> {
                            nbt.putBoolean("player_tracker", true);
                            nbt.putUuid("compass_target",getPlayerUUidFromName(playerName,mc));
                            mc.setScreen(new TestScreen(new GUI(mc)));
                            mc.player.sendMessage(Text.literal("§6Player Tracker: §3The compass target set to: "+playerName));
                        });
                    }else
                        buttons.set(index, SlotButton.defaultSlotButton(null,mc));
                    root.add(buttons.get(index), 15 + (20 * i), 5 + (20 * j));
                    index++;
                }
            }
            Button left = new Button(new Identifier("textures/gui/sprites/transferable_list/unselect.png")).setSelectedTexture(new Identifier("textures/gui/sprites/transferable_list/unselect_highlighted.png")).setToolTip(Text.literal("Scroll to the previous page").formatted(Formatting.DARK_AQUA));
            Button right = new Button(new Identifier("textures/gui/sprites/transferable_list/select.png")).setSelectedTexture(new Identifier("textures/gui/sprites/transferable_list/select_highlighted.png")).setToolTip(Text.literal("Scroll to the next page").formatted(Formatting.DARK_AQUA));
            Button back = new Button(new Identifier("textures/gui/sprites/spectator/close.png")).setToolTip(Text.literal("Back").formatted(Formatting.RED));
            left.setOnClick(()-> scrollLeft(buttons,playerHeads,playerNames,mc));
            right.setOnClick(()-> scrollRight(buttons,playerHeads,playerNames,mc));
            back.setOnClick(()-> mc.setScreen(new TestScreen(new GUI(mc))));
            root.add(left,3,15,20,40);
            root.add(right,109,15,20,40);
            root.add(back,114,53,10,10);
        }
        public void scrollLeft(List<SlotButton> buttons, ItemStack[] playerHeads, String[] playerNames, MinecraftClient mc){
            if (this.page == 0)
                return;
            this.page--;
            int counter = (buttons.size() * page) + 1;
            for(SlotButton button: buttons){
                if(playerHeads.length >= counter && playerNames.length >= counter && playerNames[counter-1] != null && !playerNames[counter-1].isEmpty()) {
                    String playerName = playerNames[counter-1];
                    NbtCompound nbt = ((IEntityDataSaver)mc.player).getPersistentData();
                    button.setToolTip(Text.literal(playerNames[counter - 1]).formatted(Formatting.BLUE)).setIcon(new ItemIcon(playerHeads[counter - 1]));
                    button.setOnClick(()-> {
                        nbt.putBoolean("player_tracker", true);
                        nbt.putUuid("compass_target",getPlayerUUidFromName(playerName,mc));
                        mc.setScreen(new TestScreen(new GUI(mc)));
                        mc.player.sendMessage(Text.literal("§6Player Tracker: §3The compass target set to: "+playerName));
                    });
                }else button = SlotButton.defaultSlotButton(button,mc);
                counter++;
            }
        }
        private void scrollRight(List<SlotButton> buttons, ItemStack[] playerHeads, String[] playerNames, MinecraftClient mc){
            this.page++;
            int counter = (buttons.size() * page) + 1;
            for(SlotButton button: buttons){
                if(playerHeads.length >= counter && playerNames.length >= counter && playerNames[counter-1] != null && !playerNames[counter-1].isEmpty()) {
                    String playerName = playerNames[counter-1];
                    NbtCompound nbt = ((IEntityDataSaver)mc.player).getPersistentData();
                    button.setToolTip(Text.literal(playerNames[counter - 1]).formatted(Formatting.BLUE)).setIcon(new ItemIcon(playerHeads[counter - 1]));
                    button.setOnClick(()-> {

                        UUID uuid = getPlayerUUidFromName(playerName,mc);
                        if (uuid != null) {
                            nbt.putBoolean("player_tracker", true);
                            nbt.putUuid("compass_target", uuid);
                            mc.setScreen(new TestScreen(new GUI(mc)));
                            mc.player.sendMessage(Text.literal("§6Player Tracker: §3The compass target set to: " + playerName));
                        }else mc.player.sendMessage(Text.literal("Player Tracker: Compass target didn't in the game or in this dimension 😔").formatted(Formatting.RED));
                    });
                }else button = SlotButton.defaultSlotButton(button,mc);
                counter++;
            }
        }

        private UUID getPlayerUUidFromName(String name, MinecraftClient mc) {
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equals(name))
                    return player.getUuid();
            }
            return null;
        }

        public static ItemStack[] getPLayersHeads(MinecraftClient client) {
            List<AbstractClientPlayerEntity> players = client.world.getPlayers();
            ItemStack[] skulls = new ItemStack[players.size()];
            int v = 1;
            if(players.size() > 1)
                for (int i = 1; i < skulls.length; i++) {
                    if(players.get(i).getName() != null && !players.get(i).getName().getString().isEmpty() && !Objects.equals(players.get(i).getName().getString(), "§r")){
                        skulls[i - v] = Items.PLAYER_HEAD.getDefaultStack();
                        skulls[i - v].setNbt(getNbtFromProfile(players.get(i).getGameProfile()));
                    }else v++;
            }
            return skulls;
        }

        public static String[] getPLayersNames(MinecraftClient client) {
            List<AbstractClientPlayerEntity> players = client.world.getPlayers();
            String[] names = new String[players.size()];
            int v = 1;
            if (players.size() > 1)
                for (int i = 1; i < players.size(); i++) {
                    String name = players.get(i).getName().getString();
                    if(name != null && !name.isEmpty() && !name.equals("§r"))
                        names[i-v] = name;
                    else v++;
                }
            return names;
        }

        public static NbtCompound getNbtFromProfile(GameProfile profile) {
            // apply player's skin to head
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile));

            // set name of item to player's name
            NbtCompound displayTag = new NbtCompound();
            displayTag.putString("Name", profile.getName());

            nbtCompound.put("display", displayTag);

            return nbtCompound;
        }
    }
}
