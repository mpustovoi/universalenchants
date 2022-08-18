package fuzs.universalenchants;

import fuzs.puzzleslib.core.CoreServices;
import fuzs.universalenchants.data.EnchantmentDataManager;
import fuzs.universalenchants.handler.BetterEnchantsHandler;
import fuzs.universalenchants.handler.ItemCompatHandler;
import fuzs.universalenchants.init.ForgeModRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod(UniversalEnchants.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class UniversalEnchantsForge {

    @SubscribeEvent
    public static void onConstructMod(final FMLConstructModEvent evt) {
        CoreServices.FACTORIES.modConstructor(UniversalEnchants.MOD_ID).accept(new UniversalEnchants());
        ForgeModRegistry.touch();
        registerHandlers();
    }

    private static void registerHandlers() {
        MinecraftForge.EVENT_BUS.addListener((final TagsUpdatedEvent evt) -> {
            if (evt.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
                EnchantmentDataManager.loadAll();
            }
        });
        ItemCompatHandler itemCompatHandler = new ItemCompatHandler();
        MinecraftForge.EVENT_BUS.addListener((final ArrowLooseEvent evt) -> {
            itemCompatHandler.onArrowLoose(evt.getEntity(), evt.getBow(), evt.getLevel(), evt.getCharge(), evt.hasAmmo());
        });
        MinecraftForge.EVENT_BUS.addListener((final LivingEntityUseItemEvent.Tick evt) -> {
            itemCompatHandler.onItemUseTick(evt.getEntity(), evt.getItem(), evt.getDuration()).ifPresent(evt::setDuration);
        });
        MinecraftForge.EVENT_BUS.addListener((final LootingLevelEvent evt) -> {
            itemCompatHandler.onLootingLevel(evt.getEntity(), evt.getDamageSource(), evt.getLootingLevel()).ifPresent(evt::setLootingLevel);
        });
        BetterEnchantsHandler betterEnchantsHandler = new BetterEnchantsHandler();
        MinecraftForge.EVENT_BUS.addListener((final ArrowNockEvent evt) -> {
            InteractionResultHolder<ItemStack> result = betterEnchantsHandler.onArrowNock(evt.getEntity(), evt.getBow(), evt.getLevel(), evt.getHand());
            if (result.getResult() != InteractionResult.PASS) {
                evt.setAction(result);
                // cancelling the event is only used when firing the arrow is supposed to fail
            }
        });
        MinecraftForge.EVENT_BUS.addListener((final PlayerInteractEvent.RightClickItem evt) -> {
            InteractionResultHolder<ItemStack> result = betterEnchantsHandler.onRightClickItem(evt.getEntity(), evt.getLevel(), evt.getHand());
            if (result.getResult() != InteractionResult.PASS) {
                evt.setCancellationResult(result.getResult());
                evt.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((final LivingHurtEvent evt) -> {
            betterEnchantsHandler.onLivingHurt(evt.getEntity(), evt.getSource(), evt.getAmount());
        });
        MinecraftForge.EVENT_BUS.addListener((final BlockEvent.FarmlandTrampleEvent evt) -> {
            betterEnchantsHandler.onFarmlandTrample((Level) evt.getLevel(), evt.getPos(), evt.getState(), evt.getFallDistance(), evt.getEntity()).ifPresent(unit -> evt.setCanceled(true));
        });
        // run after other mods had a chance to change looting level
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, (final LivingExperienceDropEvent evt) -> {
            betterEnchantsHandler.onLivingExperienceDrop(evt.getEntity(), evt.getAttackingPlayer(), evt.getOriginalExperience(), evt.getDroppedExperience()).ifPresent(evt::setDroppedExperience);
        });
    }
}
