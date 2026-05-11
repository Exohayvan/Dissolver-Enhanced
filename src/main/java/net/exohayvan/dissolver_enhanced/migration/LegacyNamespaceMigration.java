package net.exohayvan.dissolver_enhanced.migration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;

public final class LegacyNamespaceMigration {
    private static final String OLD_ID_PREFIX = DissolverEnhanced.OLD_MOD_ID + ":";
    private static final String NEW_ID_PREFIX = DissolverEnhanced.MOD_ID + ":";

    private LegacyNamespaceMigration() {
    }

    public static String migrateIdentifier(String value) {
        if (!value.startsWith(OLD_ID_PREFIX)) {
            return value;
        }

        return NEW_ID_PREFIX + value.substring(OLD_ID_PREFIX.length());
    }

    public static void migrateChunkNbt(NbtCompound nbt) {
        if (migrateElement(nbt)) {
            DissolverEnhanced.LOGGER.info("Migrated legacy {} namespace data in loaded chunk NBT.", DissolverEnhanced.OLD_MOD_ID);
        }
    }

    private static boolean migrateElement(NbtElement element) {
        if (element instanceof NbtCompound compound) {
            return migrateCompound(compound);
        }

        if (element instanceof NbtList list) {
            return migrateList(list);
        }

        return false;
    }

    private static boolean migrateCompound(NbtCompound compound) {
        boolean changed = false;
        List<String> keys = new ArrayList<>(compound.getKeys());

        for (String key : keys) {
            NbtElement value = compound.get(key);
            if (value instanceof NbtString stringValue) {
                String migrated = migrateIdentifier(stringValue.asString());
                if (!migrated.equals(stringValue.asString())) {
                    compound.put(key, NbtString.of(migrated));
                    changed = true;
                }
            } else if (value != null && migrateElement(value)) {
                changed = true;
            }
        }

        return changed;
    }

    private static boolean migrateList(NbtList list) {
        boolean changed = false;

        for (int i = 0; i < list.size(); i++) {
            NbtElement value = list.get(i);
            if (value instanceof NbtString stringValue) {
                String migrated = migrateIdentifier(stringValue.asString());
                if (!migrated.equals(stringValue.asString())) {
                    list.set(i, NbtString.of(migrated));
                    changed = true;
                }
            } else if (migrateElement(value)) {
                changed = true;
            }
        }

        return changed;
    }
}
