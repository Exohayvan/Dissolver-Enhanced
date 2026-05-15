package net.exohayvan.dissolver_enhanced.data;

import java.util.ArrayList;
import java.math.BigInteger;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import net.minecraft.nbt.NbtElement;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.ServerCompat;
import net.exohayvan.dissolver_enhanced.migration.LegacyNamespaceMigration;

// https://fabricmc.net/wiki/tutorial:persistent_states#player_specific_persistent_state
public class StateSaverAndLoader extends PersistentState {
    public PlayerData sharedData = new PlayerData();
    public HashMap<UUID, PlayerData> players = new HashMap<>();
    private static final Codec<StateSaverAndLoader> STATE_CODEC = Codec.of(
            StateSaverAndLoader::encode,
            StateSaverAndLoader::decode
    );

    private static NbtCompound storeData(NbtCompound playerNbt, PlayerData playerData) {
        if (playerData.NAME != "") playerNbt.putString("NAME", playerData.NAME);
        playerNbt.putString("EMC_BIG", EmcNumber.nonNegative(playerData.EMC).toString());
        playerNbt.putInt("EMC", EmcNumber.toIntSaturated(playerData.EMC));
        playerNbt = storeList(playerNbt, "LEARNED_ITEMS", playerData.LEARNED_ITEMS);

        return playerNbt;
    }

    private static PlayerData getData(NbtCompound playerNbt, PlayerData playerData) {
        playerData.NAME = getStringCompat(playerNbt, "NAME");
        playerData.EMC = loadEmc(playerNbt);
        playerData.LEARNED_ITEMS = migrateLearnedItemIds(getList(playerNbt, "LEARNED_ITEMS"));

        return playerData;
    }

    private static BigInteger loadEmc(NbtCompound playerNbt) {
        if (hasKey(playerNbt, "EMC_BIG")) {
            return EmcNumber.parse(getStringCompat(playerNbt, "EMC_BIG"));
        }

        return EmcNumber.of(getIntCompat(playerNbt, "EMC"));
    }

    private static List<String> migrateLearnedItemIds(List<String> learnedItems) {
        List<String> migrated = new ArrayList<>();

        for (String itemId : learnedItems) {
            String migratedItemId = migrateItemId(itemId);
            if (!migrated.contains(migratedItemId)) {
                migrated.add(migratedItemId);
            }
        }

        return migrated;
    }

    private static String migrateItemId(String itemId) {
        return LegacyNamespaceMigration.migrateIdentifier(itemId);
    }

    // STRING LISTS

    private static NbtCompound storeList(NbtCompound playerNbt, String key, List<String> list) {
        int listLength = list.size();
        playerNbt.putInt(key + "_SIZE", listLength);

        int index = -1;
        for (String value : list) {
            index++;
            playerNbt.putString(key + ":" + index, value);
        };

        return playerNbt;
    }

    private static List<String> getList(NbtCompound playerNbt, String key) {
        int listLength = getIntCompat(playerNbt, key + "_SIZE");
        List<String> list = new ArrayList<>();

        for (int i = 0; i < listLength; i++) {
            list.add(getStringCompat(playerNbt, key + ":" + i));
        }

        return list;
    }

    private static boolean hasKey(NbtCompound nbt, String key) {
        return nbt.get(key) != null;
    }

    private static NbtCompound getCompoundCompat(NbtCompound nbt, String key) {
        NbtElement element = nbt.get(key);
        return element instanceof NbtCompound compound ? compound : new NbtCompound();
    }

    private static String getStringCompat(NbtCompound nbt, String key) {
        return readString(nbt.get(key));
    }

    private static int getIntCompat(NbtCompound nbt, String key) {
        return readInt(nbt.get(key));
    }

    private static String readString(NbtElement element) {
        if (element == null) return "";

        for (String methodName : new String[] { "comp_3831", "asString", "method_10714", "method_68658" }) {
            try {
                Method method = element.getClass().getMethod(methodName);
                Object value = method.invoke(element);
                if (value instanceof String stringValue) return stringValue;
                if (value instanceof Optional<?> optional && optional.orElse(null) instanceof String stringValue) return stringValue;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        String raw = element.toString();
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static int readInt(NbtElement element) {
        if (element == null) return 0;

        for (String methodName : new String[] { "intValue", "method_10701", "method_10698", "method_68659" }) {
            try {
                Method method = element.getClass().getMethod(methodName);
                Object value = method.invoke(element);
                if (value instanceof Number number) return number.intValue();
                if (value instanceof Optional<?> optional && optional.orElse(null) instanceof Number number) return number.intValue();
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            return Integer.parseInt(element.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    // STORE DATA

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt = storePlayersData(nbt, StateSaverAndLoader::storeData);

        return nbt;
    }

    private interface StoreDataInterface {
        NbtCompound store(NbtCompound playerNbt, PlayerData playerData);
    }

    private NbtCompound storePlayersData(NbtCompound nbt, StoreDataInterface func) {
        // PLAYER SPECIFIC

        NbtCompound playersNbt = hasKey(nbt, "players") ? getCompoundCompat(nbt, "players") : new NbtCompound();
        
        players.forEach((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt = func.store(playerNbt, playerData);
            playersNbt.put(uuid.toString(), playerNbt);
        });

        nbt.put("players", playersNbt);

        // GLOBAL DATA
        
        NbtCompound globalNbt = new NbtCompound();
        globalNbt = func.store(globalNbt, sharedData);
        nbt.put("globalData", globalNbt);

        return nbt;
    }

    // GET DATA

    public static StateSaverAndLoader createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        state = getPlayersData(nbt, state, StateSaverAndLoader::getData);

        return state;
    }

    private interface GetDataInterface {
        PlayerData get(NbtCompound playerNbt, PlayerData playerData);
    }

    private static StateSaverAndLoader getPlayersData(NbtCompound nbt, StateSaverAndLoader state, GetDataInterface func) {
        // PLAYER SPECIFIC

        NbtCompound playersNbt = getCompoundCompat(nbt, "players");
        
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();
            NbtCompound playerNbt = getCompoundCompat(playersNbt, key);

            playerData = func.get(playerNbt, playerData);

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        // GLOBAL DATA
        
        NbtCompound globalNbt = getCompoundCompat(nbt, "globalData");
        PlayerData playerData = new PlayerData();

        playerData = func.get(globalNbt, playerData);

        state.sharedData = playerData;

        return state;
    }

    // GLOBAL

    public static PlayerData getGlobalData(MinecraftServer server) {
        StateSaverAndLoader serverState = getServerState(server);
        return serverState.sharedData;
    }

    public static void setGlobalEMC(MinecraftServer server, BigInteger emc) {
        StateSaverAndLoader serverState = getServerState(server);
        PlayerData globalData = serverState.sharedData;

        globalData.EMC = EmcNumber.nonNegative(emc);
        serverState.sharedData = globalData;

        updateAllServerPlayers(server);
    }

    public static void setGlobalLearned(MinecraftServer server, List<String> learnedList) {
        StateSaverAndLoader serverState = getServerState(server);
        PlayerData globalData = serverState.sharedData;

        globalData.LEARNED_ITEMS = learnedList;
        serverState.sharedData = globalData;

        updateAllServerPlayers(server);
    }

    private static void updateAllServerPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            EMCHelper.sendStateToClient((PlayerEntity)player);
        }
    }

    // PLAYER MANAGER

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        StateSaverAndLoader state = getState(persistentStateManager, DissolverEnhanced.MOD_ID);
        if (state == null) {
            state = getState(persistentStateManager, DissolverEnhanced.OLD_MOD_ID);
            if (state != null) {
                DissolverEnhanced.LOGGER.info("Migrating player EMC state from {} to {}.", DissolverEnhanced.OLD_MOD_ID, DissolverEnhanced.MOD_ID);
                setState(persistentStateManager, DissolverEnhanced.MOD_ID, state);
            }
        }
        if (state == null) {
            state = new StateSaverAndLoader();
            setState(persistentStateManager, DissolverEnhanced.MOD_ID, state);
        }

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        state.markDirty();

        return state;
    }

    private static <T> DataResult<T> encode(StateSaverAndLoader state, DynamicOps<T> ops, T prefix) {
        NbtCompound nbt = state.writeNbt(new NbtCompound(), null);
        T converted = Dynamic.convert(NbtOps.INSTANCE, ops, nbt);
        return DataResult.success(converted);
    }

    private static <T> DataResult<Pair<StateSaverAndLoader, T>> decode(DynamicOps<T> ops, T input) {
        NbtElement converted = Dynamic.convert(ops, NbtOps.INSTANCE, input);
        if (!(converted instanceof NbtCompound nbt)) {
            return DataResult.error(() -> "Expected compound NBT for " + DissolverEnhanced.MOD_ID + " persistent state");
        }

        return DataResult.success(Pair.of(createFromNbt(nbt, null), input));
    }

    private static StateSaverAndLoader getState(PersistentStateManager persistentStateManager, String id) {
        try {
            Object type = createStateType(id);
            Method get = findMethod(persistentStateManager.getClass(), "method_20786", "get", 1, 2);
            Object state = get.getParameterCount() == 1
                    ? get.invoke(persistentStateManager, type)
                    : get.invoke(persistentStateManager, type, id);
            return state instanceof StateSaverAndLoader saver ? saver : null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load Dissolver Enhanced persistent state", exception);
        }
    }

    private static void setState(PersistentStateManager persistentStateManager, String id, StateSaverAndLoader state) {
        try {
            Method set = findMethod(persistentStateManager.getClass(), "method_123", "set", 2);
            if (set.getParameterTypes()[0] == String.class) {
                set.invoke(persistentStateManager, id, state);
                return;
            }

            set.invoke(persistentStateManager, createStateType(id), state);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to save Dissolver Enhanced persistent state", exception);
        }
    }

    private static Object createStateType(String id) throws ReflectiveOperationException {
        Class<?> newTypeClass = findClass("net.minecraft.class_10741", "net.minecraft.world.PersistentStateType");
        if (newTypeClass != null) {
            Constructor<?> constructor = newTypeClass.getConstructor(String.class, Supplier.class, Codec.class, findDataFixTypesClass());
            return constructor.newInstance(id, (Supplier<StateSaverAndLoader>) StateSaverAndLoader::new, STATE_CODEC, null);
        }

        Class<?> oldTypeClass = findClass("net.minecraft.class_18$class_8645", "net.minecraft.world.PersistentState$Type");
        if (oldTypeClass == null) {
            throw new ClassNotFoundException("No supported PersistentState type class found");
        }

        Constructor<?> constructor = oldTypeClass.getConstructor(Supplier.class, BiFunction.class, findDataFixTypesClass());
        BiFunction<NbtCompound, RegistryWrapper.WrapperLookup, StateSaverAndLoader> loader = StateSaverAndLoader::createFromNbt;
        return constructor.newInstance((Supplier<StateSaverAndLoader>) StateSaverAndLoader::new, loader, null);
    }

    private static Class<?> findDataFixTypesClass() throws ClassNotFoundException {
        Class<?> dataFixTypes = findClass("net.minecraft.class_4284", "net.minecraft.datafixer.DataFixTypes");
        if (dataFixTypes == null) {
            throw new ClassNotFoundException("DataFixTypes");
        }
        return dataFixTypes;
    }

    private static Class<?> findClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, String intermediaryName, String namedName, int... parameterCounts) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(intermediaryName) && !method.getName().equals(namedName)) {
                continue;
            }

            for (int parameterCount : parameterCounts) {
                if (method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        }

        throw new NoSuchMethodException(owner.getName() + "." + intermediaryName + "/" + namedName);
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        if (ServerCompat.getServer(player) == null) return new PlayerData();

        // Either get the player by the uuid, or we don't have data for him yet, make a new player state
        PlayerData playerState = getPlayerState(player, getSaver(player));

        return playerState;
    }

    private static StateSaverAndLoader getSaver(LivingEntity player) {
        MinecraftServer server = ServerCompat.getServer(player);
        return getServerState(server);
    }

    // this can also return server state if mod config is not set to "private emc"
    private static PlayerData getPlayerState(LivingEntity player, StateSaverAndLoader serverState) {
        if (ModConfig.PRIVATE_EMC) return serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
        return serverState.sharedData;
    }

    public static void setPlayerEMC(LivingEntity player, BigInteger emc) {
        if (ServerCompat.getServer(player) == null) return;

        StateSaverAndLoader serverState = getSaver(player);
        PlayerData playerState = getPlayerState(player, serverState);
        
        // store player name
        if (playerState.NAME == "") {
            String playerName = player.getDisplayName().getString();
            playerState.NAME = playerName;
        }

        playerState.EMC = EmcNumber.nonNegative(emc);
        updateState(player, serverState, playerState);
    }

    public static void setPlayerLearned(LivingEntity player, List<String> learnedList) {
        if (ServerCompat.getServer(player) == null) return;

        StateSaverAndLoader serverState = getSaver(player);
        PlayerData playerState = getPlayerState(player, serverState);

        playerState.LEARNED_ITEMS = learnedList;
        updateState(player, serverState, playerState);
    }

    private static void updateState(LivingEntity player, StateSaverAndLoader serverState, PlayerData playerState) {
        if (ModConfig.PRIVATE_EMC) {
            serverState.players.put(player.getUuid(), playerState);
            EMCHelper.sendStateToClient((PlayerEntity)player);
        } else {
            serverState.sharedData = playerState;
            updateAllServerPlayers(ServerCompat.getServer(player));
        }
    }

    public static PlayerData getFromUuid(MinecraftServer server, UUID uuid) {
        StateSaverAndLoader serverState = getServerState(server);
        PlayerData playerState = serverState.players.getOrDefault(uuid, null);
        
        return playerState;
    }

    public static HashMap<String, PlayerData> getFullList(MinecraftServer server) {
        StateSaverAndLoader serverState = getServerState(server);
        HashMap<String, PlayerData> playersData = new HashMap<>();

        serverState.players.forEach((uuid, data) -> {
            playersData.put(data.NAME == "" ? uuid.toString() : data.NAME, data);
        });

        return playersData;
    }
}
