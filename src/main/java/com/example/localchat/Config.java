package com.example.localchat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> TEAM_TAG;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_TAG;
    public static final ModConfigSpec.BooleanValue SPY_FOR_OPS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GLOBAL_PREFIX = b.comment("Префикс для глобального чата")
                .define("globalPrefix", "!");
        TEAM_TAG = b.define("teamTag", "[Команда] ");
        GLOBAL_TAG = b.define("globalTag", "[Г] ");
        SPY_FOR_OPS = b.comment("Операторы (уровень 2+) видят командный чат всех команд")
                .define("spyForOps", false);
        SPEC = b.build();
    }
}
