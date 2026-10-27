package com.example.localchat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Set;

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

        if (denyIfMuted(sender)) return;

        // Глобальный чат: сообщение начинается с префикса (по умолчанию !)
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

        // Командный (локальный) чат: видят все игроки твоей команды, где бы они ни были
        Team team = sender.getTeam();
        if (team == null) {
            sender.sendSystemMessage(Component.literal(
                    "Вы не состоите в команде, вас никто не услышит. Для глобального чата начните сообщение с "
                            + prefix)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        Component msg = build(Config.TEAM_TAG.get(), ChatFormatting.GREEN, sender, raw);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean sameTeam = team == p.getTeam();
            boolean spy = Config.SPY_FOR_OPS.get() && p.hasPermissions(2);
            if (sameTeam || spy) {
                p.sendSystemMessage(msg);
            }
        }
        server.sendSystemMessage(msg);
    }

    private static final Set<String> BLOCKED_WHEN_MUTED =
            Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm");

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MuteManager.load(event.getServer());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MuteCommands.register(event.getDispatcher());
    }

    // Замученный игрок не может писать через /msg, /me, /say, /teammsg
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        String root = input.split(" ", 2)[0].toLowerCase();
        int colon = root.indexOf(':');
        if (colon >= 0) root = root.substring(colon + 1);

        if (BLOCKED_WHEN_MUTED.contains(root) && denyIfMuted(player)) {
            event.setCanceled(true);
        }
    }

    private static boolean denyIfMuted(ServerPlayer player) {
        MuteManager.Entry mute = MuteManager.get(player.getUUID());
        if (mute == null) return false;
        String msg = "Вы замучены (осталось: " + MuteManager.timeLeft(mute.until) + ")."
                + (mute.reason == null || mute.reason.isEmpty() ? "" : " Причина: " + mute.reason);
        player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        return true;
    }

    private static Component build(String tag, ChatFormatting color, ServerPlayer sender, String text) {
        MutableComponent c = Component.literal(tag).withStyle(color);
        c.append(sender.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        c.append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        c.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        return c;
    }
}
