package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DtmTridentItem extends TridentItem implements PolymerItem {

    public DtmTridentItem(Item.Properties settings) {
        super(settings.attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, (double)8.0F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, (double)-2.9F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.minecraft.trident");
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.TRIDENT;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return PolymerItem.super.getPolymerItemModel(Items.TRIDENT.getDefaultInstance(), context);
    }

    private static final Map<Player, UUID> lastThrownTridentUUID = new HashMap<>();

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        ItemStack stackInHand = user.getItemInHand(user.getUsedItemHand());
        if (user instanceof Player playerEntity) {
            int i = this.getUseDuration(stack, user) - remainingUseTicks;
            if (i < 10) {
                return false;
            } else {
                float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, playerEntity);
                if (f > 0.0F && !playerEntity.isInWaterOrRain()) {
                    return false;
                } else if (stack.nextDamageWillBreak()) {
                    return false;
                } else {
                    Holder<SoundEvent> registryEntry = (Holder) EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
                    playerEntity.awardStat(Stats.ITEM_USED.get(this));
                    if (world instanceof ServerLevel) {
                        ServerLevel serverWorld = (ServerLevel) world;
                        stack.hurtWithoutBreaking(1, playerEntity);

                        if (f == 0.0F) {
                            removeLastThrownTrident(playerEntity, serverWorld);
                            ThrownTrident tridentEntity = (ThrownTrident) Projectile.spawnProjectileFromRotation(ThrownTrident::new, serverWorld, stack, playerEntity, 0.0F, 2.5F, 1.0F);
                            if (playerEntity.hasInfiniteMaterials()) {
                                tridentEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                            } else {
                                ((Player) user).getCooldowns().addCooldown(stackInHand, 60);
                            }

                            lastThrownTridentUUID.put(playerEntity, tridentEntity.getUUID());

                            world.playSound((Player) null, tridentEntity, (SoundEvent) registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            return true;
                        }
                    }

                    if (f > 0.0F) {
                        float g = playerEntity.getYRot();
                        float h = playerEntity.getXRot();
                        float j = -Mth.sin(g * ((float)Math.PI / 180F)) * Mth.cos(h * ((float)Math.PI / 180F));
                        float k = -Mth.sin(h * ((float)Math.PI / 180F));
                        float l = Mth.cos(g * ((float)Math.PI / 180F)) * Mth.cos(h * ((float)Math.PI / 180F));
                        float m = Mth.sqrt(j * j + k * k + l * l);
                        j *= f / m;
                        k *= f / m;
                        l *= f / m;
                        playerEntity.push((double)j, (double)k, (double)l);
                        playerEntity.startAutoSpinAttack(20, 8.0F, stack);
                        if (playerEntity.onGround()) {
                            float n = 1.1999999F;
                            playerEntity.move(MoverType.SELF, new Vec3((double)0.0F, (double)1.1999999F, (double)0.0F));
                        }

                        world.playSound((Player)null, playerEntity, (SoundEvent)registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }


    private void removeLastThrownTrident(Player playerEntity, ServerLevel serverWorld) {
        UUID lastTridentUuid = lastThrownTridentUUID.get(playerEntity);

        if (lastTridentUuid != null) {
            Entity entity = serverWorld.getEntity(lastTridentUuid);
            if (entity instanceof ThrownTrident tridentEntity && tridentEntity.getOwner() == playerEntity) {
                tridentEntity.discard();
            }
        }
    }
}
