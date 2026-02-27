package eu.pb4.destroythemonument.game.playerclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.items.DtmItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

import java.util.List;
import java.util.Map;

public record PlayerClass(
        String name, ItemStack icon,
        Map<Holder<Attribute>, Double> attributes,
        List<ItemStack> armorVisual, List<ItemStack> items,
        List<RestockableItem> restockableItems,
        int blocksToPlanks
) {
    public static final int TOOL_REPAIR_COST = 9943235;

    public static final Codec<PlayerClass> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("class_name").forGetter(PlayerClass::name),
            MoreCodecs.ITEM_STACK.fieldOf("icon").forGetter(PlayerClass::icon),
            Codec.unboundedMap(BuiltInRegistries.ATTRIBUTE.holderByNameCodec(), Codec.DOUBLE).optionalFieldOf("attributes", Map.of()).forGetter(PlayerClass::attributes),
            Codec.list(MoreCodecs.ITEM_STACK).fieldOf("armor").forGetter(PlayerClass::armorVisual),
            Codec.list(MoreCodecs.ITEM_STACK).fieldOf("items").forGetter(PlayerClass::items),
            Codec.list(RestockableItem.CODEC).fieldOf("restockable_items").forGetter(PlayerClass::restockableItems),
            Codec.INT.optionalFieldOf("blocks_to_planks", 2).forGetter(PlayerClass::blocksToPlanks)
    ).apply(instance, PlayerClass::new));

    public void setupPlayer(ServerPlayer player, TeamData team) {
        for (var x : this.attributes.entrySet()) {
            player.getAttributes().getInstance(x.getKey()).setBaseValue(x.getValue());
        }

        for (ItemStack itemStack : this.items) {
            player.getInventory().add(ItemStackBuilder.of(itemStack).setUnbreakable().build());
        }

        for (RestockableItem ri : this.restockableItems) {
            if (ri.startingCount > 0) {
                ItemStack stack = ri.itemStack.copy();
                stack.setCount(ri.startingCount);
                player.getInventory().add(stack);
            }
        }

        player.setItemSlot(EquipmentSlot.HEAD, ItemStackBuilder.of(this.armorVisual.get(0))
                        .setDyeColor(team.getConfig().dyeColor().getValue())
                        .set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).build());
        player.setItemSlot(EquipmentSlot.CHEST, ItemStackBuilder.of(this.armorVisual.get(1))
                .setDyeColor(team.getConfig().dyeColor().getValue())
                .set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).build());
        player.setItemSlot(EquipmentSlot.LEGS, ItemStackBuilder.of(this.armorVisual.get(2))
                .setDyeColor(team.getConfig().dyeColor().getValue())
                .set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).build());
        player.setItemSlot(EquipmentSlot.FEET, ItemStackBuilder.of(this.armorVisual.get(3))
                .setDyeColor(team.getConfig().dyeColor().getValue())
                .set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).build());

        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DtmItems.MAP));
    }

    public void maybeRestockPlayer(ServerPlayer player, PlayerData playerData) {
        for (RestockableItem ri : this.restockableItems) {
            var timer = playerData.restockTimers.getInt(ri);
            if (timer >= ri.restockTime && player.getInventory().countItem(ri.itemStack.getItem()) < ri.maxCount) {
                ItemStack stack = ri.itemStack.copy();
                player.getInventory().add(stack);
                playerData.restockTimers.put(ri, 0);
            } else if (player.getInventory().countItem(ri.itemStack.getItem()) >= ri.maxCount) {
                playerData.restockTimers.put(ri, 0);
            }
        }
    }


    public static class RestockableItem {
        public static final Codec<RestockableItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MoreCodecs.ITEM_STACK.fieldOf("item").forGetter(i -> i.itemStack),
                Codec.INT.fieldOf("restock_time").forGetter(i -> i.restockTime),
                Codec.INT.fieldOf("max_count").forGetter(i -> i.maxCount),
                Codec.INT.optionalFieldOf("start_count", 0).forGetter(i -> i.startingCount),
                Codec.INT.optionalFieldOf("start_offset", 0).forGetter(i -> i.startingOffset)
            ).apply(instance, RestockableItem::new));

        public final ItemStack itemStack;
        public final int restockTime;
        public final int maxCount;
        public final int startingCount;
        public final int startingOffset;

        public RestockableItem(ItemStack itemStack, int restockTime, int maxCount, int startingCount, int startingOffset) {
            this.itemStack = itemStack;
            this.restockTime = restockTime;
            this.maxCount = maxCount;
            this.startingCount = startingCount;
            this.startingOffset = startingOffset;
        }

    }
}
