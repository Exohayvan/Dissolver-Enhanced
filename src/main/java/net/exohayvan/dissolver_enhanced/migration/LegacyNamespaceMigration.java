package net.exohayvan.dissolver_enhanced.migration;

import java.util.ArrayList;
import java.util.List;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

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

    public static void migrateChunkNbt(CompoundTag nbt) {
        if (migrateElement(nbt)) {
            DissolverEnhanced.LOGGER.info("Migrated legacy {} namespace data in loaded chunk NBT.", DissolverEnhanced.OLD_MOD_ID);
        }
    }

    private static boolean migrateElement(Tag element) {
        if (element instanceof CompoundTag compound) {
            return migrateCompound(compound);
        }

        if (element instanceof ListTag list) {
            return migrateList(list);
        }

        return false;
    }

    private static boolean migrateCompound(CompoundTag compound) {
        boolean changed = false;
        List<String> keys = new ArrayList<>(compound.getAllKeys());

        for (String key : keys) {
            Tag value = compound.get(key);
            if (value instanceof StringTag stringValue) {
                String migrated = migrateIdentifier(stringValue.getAsString());
                if (!migrated.equals(stringValue.getAsString())) {
                    compound.put(key, StringTag.valueOf(migrated));
                    changed = true;
                }
            } else if (value != null && migrateElement(value)) {
                changed = true;
            }
        }

        return changed;
    }

    private static boolean migrateList(ListTag list) {
        boolean changed = false;

        for (int i = 0; i < list.size(); i++) {
            Tag value = list.get(i);
            if (value instanceof StringTag stringValue) {
                String migrated = migrateIdentifier(stringValue.getAsString());
                if (!migrated.equals(stringValue.getAsString())) {
                    list.set(i, StringTag.valueOf(migrated));
                    changed = true;
                }
            } else if (migrateElement(value)) {
                changed = true;
            }
        }

        return changed;
    }
}
