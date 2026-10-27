package com.example.localchat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.ServerChatEvent;

@Mod(LocalChat.MODID)
@EventBusSubscriber(modid = LocalChat.MODID)
public class LocalChat {
    public static final String MODID = "localchat";

    public LocalChat(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        MinecraftServer server = sender.getServer();
        if (server == null) return;

        String raw = event.getRawText();
        String prefix = Config.GLOBAL_PREFIX.get();

        event.setCanceled(true);

        // Глобальный чат
        if (!prefix.isEmpty() && raw.startsWith(prefix)) {
            String text = raw.substring(prefix.length()).trim();
            if (text.isEmpty()) return;

            Component msg = build(Config.GLOBAL_TAG.get(), ChatFormatting.GOLD, sender, text);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(msg);
            }
            server.sendSystemMessage(msg);
            return;
        }

        // Локальный чат
        Component msg = build(Config.LOCAL_TAG.get(), ChatFormatting.YELLOW, sender, raw);
        double r = Config.RADIUS.get();
        double r2 = r * r;
        int heard = 0;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() == sender.level() && p.distanceToSqr(sender) <= r2) {
                p.sendSystemMessage(msg);
                if (p != sender) heard++;
            }
        }
        server.sendSystemMessage(msg);

        if (heard == 0 && Config.NOBODY_HEARS_WARNING.get()) {
            sender.sendSystemMessage(Component.literal(
                    "Вас никто не слышит. Для глобального чата начните сообщение с "
                            + Config.GLOBAL_PREFIX.get())
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    private static Component build(String tag, ChatFormatting color, ServerPlayer sender, String text) {
        MutableComponent c = Component.literal(tag).withStyle(color);
        c.append(sender.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        c.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        c.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        return c;
    }
}
