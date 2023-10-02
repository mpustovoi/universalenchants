package fuzs.universalenchants;

import fuzs.puzzleslib.api.config.v3.ConfigHolder;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.core.v1.context.PackRepositorySourcesContext;
import fuzs.puzzleslib.api.event.v1.core.EventPhase;
import fuzs.puzzleslib.api.event.v1.entity.living.LivingExperienceDropCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.LivingHurtCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.LootingLevelCallback;
import fuzs.puzzleslib.api.event.v1.entity.living.UseItemEvents;
import fuzs.puzzleslib.api.event.v1.entity.player.ArrowLooseCallback;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerInteractEvents;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerXpEvents;
import fuzs.puzzleslib.api.event.v1.level.BlockEvents;
import fuzs.puzzleslib.api.event.v1.server.RegisterCommandsCallback;
import fuzs.puzzleslib.api.event.v1.server.TagsUpdatedCallback;
import fuzs.puzzleslib.api.resources.v1.DynamicPackResources;
import fuzs.puzzleslib.api.resources.v1.PackResourcesHelper;
import fuzs.universalenchants.config.ClientConfig;
import fuzs.universalenchants.config.CommonConfig;
import fuzs.universalenchants.config.ServerConfig;
import fuzs.universalenchants.data.DynamicItemTagProvider;
import fuzs.universalenchants.handler.BetterEnchantsHandler;
import fuzs.universalenchants.handler.ItemCompatHandler;
import fuzs.universalenchants.init.ModRegistry;
import fuzs.universalenchants.server.commands.ModEnchantCommand;
import fuzs.universalenchants.world.item.enchantment.data.AdditionalEnchantmentDataProvider;
import fuzs.universalenchants.world.item.enchantment.serialize.EnchantmentHoldersManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UniversalEnchants implements ModConstructor {
    public static final String MOD_ID = "universalenchants";
    public static final String MOD_NAME = "Universal Enchants";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static final ConfigHolder CONFIG = ConfigHolder.builder(MOD_ID).client(ClientConfig.class).common(CommonConfig.class).server(ServerConfig.class);

    @Override
    public void onConstructMod() {
        ModRegistry.touch();
        AdditionalEnchantmentDataProvider.initialize();
        registerHandlers();
    }

    private static void registerHandlers() {
        RegisterCommandsCallback.EVENT.register((dispatcher, context, environment) -> {
            if (CONFIG.get(CommonConfig.class).enchantCommand.replaceVanillaCommand()) {
                ModEnchantCommand.register(dispatcher, context);
            }
        });
        TagsUpdatedCallback.EVENT.register((RegistryAccess registries, boolean client) -> {
            if (!client) EnchantmentHoldersManager.loadAll();
        });
        ArrowLooseCallback.EVENT.register(ItemCompatHandler::onArrowLoose);
        UseItemEvents.TICK.register(ItemCompatHandler::onUseItemTick);
        LootingLevelCallback.EVENT.register(ItemCompatHandler::onLootingLevel);
        PlayerInteractEvents.USE_ITEM_V2.register(BetterEnchantsHandler::onUseItem);
        LivingHurtCallback.EVENT.register(BetterEnchantsHandler::onLivingHurt);
        BlockEvents.FARMLAND_TRAMPLE.register(BetterEnchantsHandler::onFarmlandTrample);
        // run after other mods had a chance to change looting level
        LivingExperienceDropCallback.EVENT.register(EventPhase.AFTER, BetterEnchantsHandler::onLivingExperienceDrop);
        BlockEvents.DROP_EXPERIENCE.register(EventPhase.AFTER, BetterEnchantsHandler::onDropExperience);
        PlayerXpEvents.PICKUP_XP.register(BetterEnchantsHandler::onPickupXp);
    }

    @Override
    public void onAddDataPackFinders(PackRepositorySourcesContext context) {
        context.addRepositorySource(PackResourcesHelper.buildServerPack(id("enchantment_target_tags"), DynamicPackResources.create(DynamicItemTagProvider::new), true));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
