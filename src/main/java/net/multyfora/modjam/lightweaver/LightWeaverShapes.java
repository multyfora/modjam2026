package net.multyfora.modjam.lightweaver;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Arrays;
import java.util.List;

public final class LightWeaverShapes {

    public static final int GRID_SIZE = 9;
    private static final int CELLS = GRID_SIZE * GRID_SIZE;
    private static final int WORDS = (CELLS + 63) / 64;

    public record WeaverShape(String id, String displayName, boolean[] pattern,
                               ResourceKey<Enchantment> enchantment, int tier) {

        public boolean matches(boolean[] cells) {
            return Arrays.equals(cells, pattern);
        }

        public int cellCount() {
            int count = 0;
            for (boolean b : pattern) if (b) count++;
            return count;
        }
    }

    private static WeaverShape shape(String id, ResourceKey<Enchantment> enchantment, int tier, String... rows) {
        boolean[] cells = new boolean[CELLS];
        for (int r = 0; r < rows.length && r < GRID_SIZE; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length() && c < GRID_SIZE; c++) {
                if (row.charAt(c) == 'X') {
                    cells[r * GRID_SIZE + c] = true;
                }
            }
        }
        return new WeaverShape(id, prettyName(id), cells, enchantment, tier);
    }

    private static String prettyName(String id) {
        StringBuilder sb = new StringBuilder();
        for (String word : id.split("_")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** Alphabetical — also the order shown on the cheat sheet. */
    public static final List<WeaverShape> SHAPES = List.of(
            shape("aqua_affinity", Enchantments.AQUA_AFFINITY, 1,
                    ".........",
                    "....X....",
                    "....X....",
                    "...XXX...",
                    "..XXXXX..",
                    "..XXXXX..",
                    "...XXX...",
                    "....X....",
                    "........."),
            shape("bane_of_arthropods", Enchantments.BANE_OF_ARTHROPODS, 2,
                    "X.......X",
                    ".X.....X.",
                    "..X...X..",
                    "...X.X...",
                    "..XXXXX..",
                    "...X.X...",
                    "..X...X..",
                    ".X.....X.",
                    "X.......X"),
            shape("binding_curse", Enchantments.BINDING_CURSE, 6,
                    "...X.X...",
                    "..X...X..",
                    "..X...X..",
                    "...XXX...",
                    "..XXXXX..",
                    "..X...X..",
                    "..X...X..",
                    "..XXXXX..",
                    "........."),
            shape("blast_protection", Enchantments.BLAST_PROTECTION, 3,
                    ".........",
                    "....X....",
                    "..X.X....",
                    "..X...X..",
                    ".X.....X.",
                    "..X...X..",
                    "..X.X....",
                    "....X....",
                    "........."),
            shape("breach", Enchantments.BREACH, 4,
                    "XXXXXXXXX",
                    "X.X.....X",
                    "X..X....X",
                    "X...X...X",
                    "X....X..X",
                    "X.....X.X",
                    "X......XX",
                    "X.......X",
                    "XXXXXXXXX"),
            shape("channeling", Enchantments.CHANNELING, 3,
                    "....X....",
                    "....X....",
                    "...X.....",
                    "...X.....",
                    "..X......",
                    "..X......",
                    ".XX......",
                    ".XX......",
                    "........."),
            shape("density", Enchantments.DENSITY, 4,
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX",
                    "XXXXXXXXX"),
            shape("depth_strider", Enchantments.DEPTH_STRIDER, 3,
                    "...X.X...",
                    "..X...X..",
                    ".........",
                    "..X...X..",
                    ".X.....X.",
                    ".........",
                    ".X.....X.",
                    "X.......X",
                    "........."),
            shape("efficiency", Enchantments.EFFICIENCY, 1,
                    "....X....",
                    "...X.X...",
                    "..X...X..",
                    ".X.....X.",
                    ".........",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X...."),
            shape("feather_falling", Enchantments.FEATHER_FALLING, 1,
                    ".........",
                    ".........",
                    "....X....",
                    "....X....",
                    "...XXX...",
                    "....X....",
                    "....X....",
                    ".........",
                    "........."),
            shape("fire_aspect", Enchantments.FIRE_ASPECT, 2,
                    "....X....",
                    "...XXX...",
                    "..XXXXX..",
                    "..XX.XX..",
                    "..XXXXX..",
                    "...XXX...",
                    "....X....",
                    "....X....",
                    "....X...."),
            shape("fire_protection", Enchantments.FIRE_PROTECTION, 3,
                    ".........",
                    "X.....X..",
                    ".X...X...",
                    "..X.X....",
                    "...X.....",
                    "..X.X....",
                    ".X...X...",
                    "X.....X..",
                    "........."),
            shape("flame", Enchantments.FLAME, 1,
                    ".....X...",
                    "....XXX..",
                    "...XXXXX.",
                    "..XXXXXX.",
                    "...XXXXX.",
                    "....XXX..",
                    ".....X...",
                    ".........",
                    "........."),
            shape("fortune", Enchantments.FORTUNE, 2,
                    ".........",
                    "....X....",
                    "...XXX...",
                    "..XXXXX..",
                    ".XXXXXXX.",
                    "..XXXXX..",
                    "...XXX...",
                    "....X....",
                    "........."),
            shape("frost_walker", Enchantments.FROST_WALKER, 3,
                    ".........",
                    "X...X...X",
                    ".X..X..X.",
                    "..X.X.X..",
                    ".XXXXXXX.",
                    "..X.X.X..",
                    ".X..X..X.",
                    "X...X...X",
                    "........."),
            shape("impaling", Enchantments.IMPALING, 2,
                    "..X...X..",
                    "..X...X..",
                    "...XXX...",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "...X.X...",
                    "........."),
            shape("infinity", Enchantments.INFINITY, 1,
                    ".........",
                    "XX.....XX",
                    "X.X...X.X",
                    "..X.X.X..",
                    "...XXX...",
                    "..X.X.X..",
                    "X.X...X.X",
                    "XX.....XX",
                    "........."),
            shape("knockback", Enchantments.KNOCKBACK, 1,
                    ".........",
                    ".........",
                    "........X",
                    ".......XX",
                    "XXXXXXXXX",
                    ".......XX",
                    "........X",
                    ".........",
                    "........."),
            shape("looting", Enchantments.LOOTING, 1,
                    "..XXXXX..",
                    "..X.X.X..",
                    "..XXXXX..",
                    "..XXXXX..",
                    "..XXXXX..",
                    "..XXXXX..",
                    ".........",
                    ".........",
                    "........."),
            shape("loyalty", Enchantments.LOYALTY, 2,
                    ".........",
                    ".X.....X.",
                    ".X.....X.",
                    "..X...X..",
                    "..X...X..",
                    "...X.X...",
                    "....X....",
                    "....X....",
                    "....X...."),
            shape("luck_of_the_sea", Enchantments.LUCK_OF_THE_SEA, 1,
                    ".........",
                    "...XXX...",
                    "..X...X..",
                    "..XXXXX..",
                    "..X...X..",
                    "..XXXXX..",
                    "...X..X..",
                    "...X..X..",
                    "........."),
            shape("lunge", Enchantments.LUNGE, 4,
                    "..X......",
                    "...X.....",
                    "....X....",
                    ".....X...",
                    "......X..",
                    ".......X.",
                    ".......XX",
                    ".........",
                    "........."),
            shape("lure", Enchantments.LURE, 1,
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "...X.....",
                    "..X......",
                    ".X......."),
            shape("mending", Enchantments.MENDING, 5,
                    ".XX...XX.",
                    "XXXX.XXXX",
                    "XXXX.XXXX",
                    ".XXXXXXX.",
                    "..XXXXX..",
                    "...XXX...",
                    "....X....",
                    ".........",
                    "........."),
            shape("multishot", Enchantments.MULTISHOT, 1,
                    "....X....",
                    "...X.X...",
                    "...X.X...",
                    "..X...X..",
                    "..X...X..",
                    ".X.....X.",
                    ".X.....X.",
                    "X.......X",
                    "........."),
            shape("piercing", Enchantments.PIERCING, 1,
                    "X........",
                    ".XX......",
                    "..XX.....",
                    "...X.....",
                    ".........",
                    ".X.......",
                    "..X......",
                    "...X.....",
                    "........."),
            shape("power", Enchantments.POWER, 1,
                    "X........",
                    "XX.......",
                    "X.X......",
                    "X..X.....",
                    "X...X....",
                    "X....X...",
                    "X.....X..",
                    "X......X.",
                    "........."),
            shape("projectile_protection", Enchantments.PROJECTILE_PROTECTION, 3,
                    ".........",
                    ".........",
                    "..XXXXX..",
                    "..X...X..",
                    "..X...X..",
                    "..X...X..",
                    "..XXXXX..",
                    ".........",
                    "........."),
            shape("protection", Enchantments.PROTECTION, 2,
                    ".........",
                    ".........",
                    "....X....",
                    "....X....",
                    "..XXXXX..",
                    "....X....",
                    "....X....",
                    ".........",
                    "........."),
            shape("punch", Enchantments.PUNCH, 1,
                    ".........",
                    ".........",
                    "X........",
                    "XX.......",
                    "XXXXXXXXX",
                    "XX.......",
                    "X........",
                    ".........",
                    "........."),
            shape("quick_charge", Enchantments.QUICK_CHARGE, 1,
                    "...XXX...",
                    "....XX...",
                    ".....X...",
                    ".........",
                    "...XXX...",
                    "....XX...",
                    ".....X...",
                    ".........",
                    "........."),
            shape("respiration", Enchantments.RESPIRATION, 1,
                    ".........",
                    "..XX.XX..",
                    "..XX.XX..",
                    "..XX.XX..",
                    "..XX.XX..",
                    "..XX.XX..",
                    "...X.X...",
                    ".........",
                    "........."),
            shape("riptide", Enchantments.RIPTIDE, 3,
                    ".........",
                    "...XXX...",
                    "..X...X..",
                    "..X....X.",
                    "...X..X..",
                    "....XX...",
                    ".....X...",
                    ".........",
                    "........."),
            shape("sharpness", Enchantments.SHARPNESS, 2,
                    "....X....",
                    "....X....",
                    "....X....",
                    "..XXXXX..",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "........."),
            shape("silk_touch", Enchantments.SILK_TOUCH, 2,
                    ".........",
                    "..XXXXX..",
                    "..X...X..",
                    ".X..X..X.",
                    "X...X...X",
                    ".X..X..X.",
                    "..X...X..",
                    "..XXXXX..",
                    "........."),
            shape("smite", Enchantments.SMITE, 2,
                    "..XXXXX..",
                    "..XXXXX..",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "....X....",
                    "...XXX...",
                    "........."),
            shape("soul_speed", Enchantments.SOUL_SPEED, 5,
                    "....X....",
                    "...XXX...",
                    "..XXXXX..",
                    "..XX.XX..",
                    "..X.X.X..",
                    "...X.X...",
                    "....X....",
                    ".........",
                    "........."),
            shape("sweeping_edge", Enchantments.SWEEPING_EDGE, 2,
                    ".........",
                    ".........",
                    "....XXXXX",
                    "...XX....",
                    "..XX.....",
                    "..XX.....",
                    "...XX....",
                    "....XXXXX",
                    "........."),
            shape("swift_sneak", Enchantments.SWIFT_SNEAK, 3,
                    "X........",
                    ".X.......",
                    "..X......",
                    ".........",
                    "X........",
                    ".X.......",
                    "..X......",
                    ".........",
                    "........."),
            shape("thorns", Enchantments.THORNS, 4,
                    "XXXXXXXXX",
                    "X.......X",
                    "X.......X",
                    "X.......X",
                    "X.......X",
                    "X.......X",
                    "X.......X",
                    "X.......X",
                    "XXXXXXXXX"),
            shape("unbreaking", Enchantments.UNBREAKING, 2,
                    "...XXX...",
                    "..XXXXX..",
                    "...XXX...",
                    "...XXX...",
                    "...XXX...",
                    "...XXX...",
                    "...XXX...",
                    "..XXXXX..",
                    "........."),
            shape("vanishing_curse", Enchantments.VANISHING_CURSE, 6,
                    ".XXXXXXX.",
                    ".X.....X.",
                    ".X.....X.",
                    ".........",
                    ".........",
                    ".........",
                    ".X.....X.",
                    ".X.....X.",
                    ".XXXXXXX."),
            shape("wind_burst", Enchantments.WIND_BURST, 4,
                    "....X....",
                    "....X....",
                    "..XX.XX..",
                    "....X....",
                    "XXXXXXXXX",
                    "....X....",
                    "..XX.XX..",
                    "....X....",
                    "....X....")
    );

    static {
        for (int i = 0; i < SHAPES.size(); i++) {
            for (int j = i + 1; j < SHAPES.size(); j++) {
                if (Arrays.equals(SHAPES.get(i).pattern(), SHAPES.get(j).pattern())) {
                    throw new IllegalStateException("Duplicate infusion patterns: "
                            + SHAPES.get(i).id() + " and " + SHAPES.get(j).id());
                }
            }
        }
    }

    public static WeaverShape match(boolean[] cells) {
        for (WeaverShape shape : SHAPES) {
            if (shape.matches(cells)) return shape;
        }
        return null;
    }

    public static boolean isEmpty(boolean[] cells) {
        for (boolean cell : cells) {
            if (cell) return false;
        }
        return true;
    }

    public static String pack(boolean[] cells) {
        long[] words = new long[WORDS];
        for (int i = 0; i < CELLS; i++) {
            if (cells[i]) {
                words[i / 64] |= 1L << (i % 64);
            }
        }
        StringBuilder sb = new StringBuilder(WORDS * 16);
        for (long word : words) {
            sb.append(String.format("%016x", word));
        }
        return sb.toString();
    }

    public static boolean[] unpack(String packed) {
        boolean[] cells = new boolean[CELLS];
        if (packed == null || packed.length() != WORDS * 16) return cells;
        for (int word = 0; word < WORDS; word++) {
            long bits = Long.parseUnsignedLong(packed.substring(word * 16, word * 16 + 16), 16);
            for (int b = 0; b < 64; b++) {
                int index = word * 64 + b;
                if (index < CELLS && (bits & (1L << b)) != 0) {
                    cells[index] = true;
                }
            }
        }
        return cells;
    }

    private LightWeaverShapes() {
    }
}
