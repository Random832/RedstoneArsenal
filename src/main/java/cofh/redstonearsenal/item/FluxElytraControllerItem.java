package cofh.redstonearsenal.item;

import cofh.core.util.ProxyUtils;
import cofh.lib.item.ICoFHItem;
import cofh.lib.item.IMultiModeItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;

public class FluxElytraControllerItem extends Item implements ICoFHItem, IMultiModeItem {

    public FluxElytraControllerItem(Item.Properties builder) {

        super(builder);
        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("empowered"), (stack, world, entity) -> isEmpowered(stack) ? 1.0F : 0.0F);
    }

    @Override
    @OnlyIn (Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

        if (isEmpowered(stack)) {
            tooltip.add(getTextComponent("info.redstone_arsenal.mode.1").withStyle(TextFormatting.RED));
        } else {
            tooltip.add(getTextComponent("info.redstone_arsenal.mode.0").withStyle(TextFormatting.GRAY));
        }
        addIncrementModeChangeTooltip(stack, worldIn, tooltip, flagIn);
    }

    public boolean isEmpowered(ItemStack stack) {

        return getMode(stack) > 0;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {

        ItemStack stack = player.getItemInHand(hand);
        ItemStack chest = player.getItemBySlot(EquipmentSlotType.CHEST);
        if (chest.getItem() instanceof FluxElytraItem) {
            FluxElytraItem elytra = (FluxElytraItem) chest.getItem();
            elytra.setMode(chest, getMode(stack));

            if (elytra.boost(chest, player)) {
                return ActionResult.sidedSuccess(stack, world.isClientSide());
            }
        }
        return ActionResult.fail(stack);
    }

    @Override
    public void onModeChange(PlayerEntity player, ItemStack stack) {

        ItemStack chest = player.getItemBySlot(EquipmentSlotType.CHEST);
        if (chest.getItem() instanceof FluxElytraItem) {
            ((FluxElytraItem) chest.getItem()).setMode(chest, getMode(stack));
            if (isEmpowered(stack)) {
                player.level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.4F, 1.0F);
            } else {
                player.level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 0.6F);
            }
        } else {
            setMode(stack, isEmpowered(stack) ? 0 : 1);
        }
    }

}