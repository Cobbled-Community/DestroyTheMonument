package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.TooltipFlag;
import xyz.nucleoid.packettweaker.PacketContext;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.Map;

public class MultiBlockItem extends BlockItem implements PolymerItem {
    public MultiBlockItem(Properties settings) {
        super(Blocks.BIRCH_PLANKS, settings);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getPlayer() != null) {
            GameSpace gameSpace = GameSpaceManager.get().byPlayer(context.getPlayer());
            if (gameSpace != null) {
                BaseGameLogic logic = gameSpace.getAttachment(DTM.GAME_LOGIC);

                if (logic != null) {
                    Block block = logic.participants.get(PlayerRef.of(context.getPlayer())).selectedBlock;
                    BlockState state = block.getStateForPlacement(context);
                    return state != null && this.canPlace(context, state) ? state : null;
                }
            }
        }

        BlockState blockState = this.getBlock().getStateForPlacement(context);
        return blockState != null && this.canPlace(context, blockState) ? blockState : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {

    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BIRCH_PLANKS;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag type, PacketContext context) {
        var player = context.getPlayer();
        if (player == null) {
            return PolymerItem.super.getPolymerItemStack(itemStack, type, context);
        }

        Item item = Items.BIRCH_PLANKS;
        GameSpace gameSpace = GameSpaceManager.get().byPlayer(player);
        if (gameSpace != null) {
            BaseGameLogic logic = gameSpace.getAttachment(DTM.GAME_LOGIC);

            if (logic != null) {
                PlayerData data = logic.participants.get(PlayerRef.of(player));

                if (data != null) {
                    item = data.selectedBlock.asItem();
                }
            }

            if (item instanceof PolymerItem virtualItem && item != this) {
                ItemStack stack = item.getDefaultInstance();
                stack.setCount(itemStack.getCount());
                ItemStack out = virtualItem.getPolymerItemStack(stack, type, context);
                //out.getOrCreateNbt().putString(PolymerItemUtils.POLYMER_ITEM_ID, Registries.ITEM.getId(itemStack.getItem()).toString());
                return out;
            }

            return new ItemStack(item, itemStack.getCount());
        }
        return PolymerItem.super.getPolymerItemStack(itemStack, type, context);
    }
}
