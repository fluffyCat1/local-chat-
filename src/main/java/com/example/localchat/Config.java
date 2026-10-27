package com.example.localchat;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RADIUS;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> LOCAL_TAG;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_TAG;
    public static final ModConfigSpec.BooleanValue NOBODY_HEARS_WARNING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RADIUS = b.comment("Радиус локального чата в блоках")
                .defineInRange("radius", 100, 1, 100000);
        GLOBAL_PREFIX = b.comment("Префикс для глобального чата")
                .define("globalPrefix", "!");
        LOCAL_TAG = b.define("localTag", "[Л] ");
        GLOBAL_TAG = b.define("globalTag", "[Г] ");
        NOBODY_HEARS_WARNING = b.comment("Предупреждать, если рядом никого нет")
                .define("nobodyHearsWarning", true);
        SPEC = b.build();
    }
}
