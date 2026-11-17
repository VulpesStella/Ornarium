package io.github.vulpesstella.ornarium.debug;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import io.github.vulpesstella.ornarium.api.IOrnarium;
import io.github.vulpesstella.ornarium.api.OrnariumApi;
import io.github.vulpesstella.ornarium.api.OrnariumTypes;
import io.github.vulpesstella.ornarium.api.SlotType;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = "ornariumdebug", name = "Ornarium Debug Accessories", version = "1",
    dependencies = "required-after:ornarium", acceptedMinecraftVersions = "[1.6.4]")
public final class DebugAccessories {
    private static final ResourceLocation CHARM = new ResourceLocation("ornariumdebug", "charm");
    @SidedProxy(clientSide = "io.github.vulpesstella.ornarium.debug.DebugClientProxy",
        serverSide = "io.github.vulpesstella.ornarium.debug.DebugAccessories$CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        OrnariumApi.registerSlotType(new SlotType(CHARM, "ornariumdebug.slot.charm", "ornariumdebug.group.testing",
            new ResourceLocation("minecraft", "textures/items/paper.png"), 0, "ornariumDebugCharm"));
        OrnariumApi.requestSlots(CHARM, 1);
        OreDictionary.registerOre("ornariumDebugCharm", Item.paper);
        Item amulet = register("amulet", 31990, OrnariumTypes.AMULET, "minecraft:emerald");
        register("ring", 31991, OrnariumTypes.RING, "minecraft:gold_nugget");
        register("belt", 31992, OrnariumTypes.BELT, "minecraft:leather");
        register("charm", 31993, CHARM, "minecraft:blaze_powder");
        proxy.registerRenderers(amulet);
    }

    private static Item register(String name, int id, ResourceLocation type, String texture) {
        // The legacy Item constructor takes an ID offset by the block ID range.
        if (Item.itemsList[id] != null) {
            throw new IllegalStateException("Ornarium debug item ID is occupied: " + id);
        }
        Item item = new DebugAccessory(id - 256, type)
            .setUnlocalizedName("ornariumdebug." + name)
            .setTextureName(texture)
            .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(item, name);
        return item;
    }

    public static class CommonProxy {
        public void registerRenderers(Item amulet) {}
    }

    private static final class DebugAccessory extends Item implements IOrnarium {
        private final ResourceLocation type;

        DebugAccessory(int id, ResourceLocation type) {
            super(id);
            this.type = type;
            setMaxStackSize(1);
        }

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            if (!world.isRemote) {
                // Creative use retains the held item. Keep the original stack untouched so
                // Forge does not treat the use as a hotbar change and resend the inventory.
                OrnariumApi.tryEquip(player, player.capabilities.isCreativeMode ? stack.copy() : stack);
            }
            return stack;
        }

        @Override
        public Collection<ResourceLocation> getOrnariumTypes(ItemStack stack) {
            return type.equals(OrnariumTypes.AMULET)
                ? Arrays.asList(type, CHARM) : Collections.singleton(type);
        }
        @Override public boolean canEquip(ItemStack stack, EntityLivingBase wearer) { return true; }
        @Override public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) { return true; }

        @Override
        public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
            notifyPlayer(wearer, "ornariumdebug.equipped", stack);
        }

        @Override
        public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
            notifyPlayer(wearer, "ornariumdebug.unequipped", stack);
        }

        @Override
        public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
            if (type.equals(OrnariumTypes.RING) && wearer.ticksExisted % 100 == 0) {
                notifyPlayer(wearer, "ornariumdebug.worn", stack);
            }
        }

        private static void notifyPlayer(EntityLivingBase wearer, String key, ItemStack stack) {
            if (wearer instanceof EntityPlayerMP) {
                ((EntityPlayerMP) wearer).sendChatToPlayer(
                    ChatMessageComponent.createFromTranslationWithSubstitutions(key, stack.getDisplayName()));
            }
        }
    }
}
