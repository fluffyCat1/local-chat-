package com.example.localchat;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class MuteCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /mute <игрок> [минуты (0 = навсегда)] [причина]
        d.register(Commands.literal("mute")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                        .executes(c -> mute(c, 0, ""))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                .executes(c -> mute(c, IntegerArgumentType.getInteger(c, "minutes"), ""))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(c -> mute(c,
                                                IntegerArgumentType.getInteger(c, "minutes"),
                                                StringArgumentType.getString(c, "reason")))))));

        // /unmute <игрок>
        d.register(Commands.literal("unmute")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                        .executes(MuteCommands::unmute)));

        // /mutelist
        d.register(Commands.literal("mutelist")
                .requires(s -> s.hasPermission(2))
                .executes(MuteCommands::list));
    }

    private static int mute(CommandContext<CommandSourceStack> c, int minutes, String reason) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        long until = minutes <= 0 ? 0 : System.currentTimeMillis() + minutes * 60_000L;
        int count = 0;

        for (GameProfile gp : GameProfileArgument.getGameProfiles(c, "targets")) {
            MuteManager.mute(gp.getId(), gp.getName(), until, reason);
            count++;

            ServerPlayer p = server.getPlayerList().getPlayer(gp.getId());
            if (p != null) {
                String msg = "Вы замучены (" + MuteManager.timeLeft(until) + ")."
                        + (reason.isEmpty() ? "" : " Причина: " + reason);
                p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
            }
            String finalMsg = gp.getName() + " замучен (" + MuteManager.timeLeft(until) + ")"
                    + (reason.isEmpty() ? "" : ", причина: " + reason);
            c.getSource().sendSuccess(() -> Component.literal(finalMsg).withStyle(ChatFormatting.YELLOW), true);
        }
        return count;
    }

    private static int unmute(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        MinecraftServer server = c.getSource().getServer();
        int count = 0;

        for (GameProfile gp : GameProfileArgument.getGameProfiles(c, "targets")) {
            if (MuteManager.unmute(gp.getId())) {
                count++;
                ServerPlayer p = server.getPlayerList().getPlayer(gp.getId());
                if (p != null) {
                    p.sendSystemMessage(Component.literal("Вы размучены.").withStyle(ChatFormatting.GREEN));
                }
                String name = gp.getName();
                c.getSource().sendSuccess(() -> Component.literal(name + " размучен").withStyle(ChatFormatting.GREEN), true);
            } else {
                String name = gp.getName();
                c.getSource().sendFailure(Component.literal(name + " не был замучен"));
            }
        }
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Map<UUID, MuteManager.Entry> all = MuteManager.all();
        if (all.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("Замученных игроков нет"), false);
            return 0;
        }
        for (MuteManager.Entry e : all.values()) {
            String line = e.name + " - " + MuteManager.timeLeft(e.until)
                    + (e.reason == null || e.reason.isEmpty() ? "" : " (" + e.reason + ")");
            c.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.YELLOW), false);
        }
        return all.size();
    }
}
