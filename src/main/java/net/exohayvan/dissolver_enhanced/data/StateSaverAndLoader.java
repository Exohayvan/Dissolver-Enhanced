package net.exohayvan.dissolver_enhanced.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.migration.LegacyNamespaceMigration;

// https://fabricmc.net/wiki/tutorial:persistent_states#player_specific_persistent_state
public class StateSaverAndLoader extends SavedData {
    public PlayerData sharedData = new PlayerData();
    public HashMap<UUID, PlayerData> players = new HashMap<>();

    private static CompoundTag storeData(CompoundTag playerNbt, PlayerData playerData) {
        if (playerData.NAME != "") playerNbt.putString("NAME", playerData.NAME);
        playerNbt.putInt("EMC", playerData.EMC);
        playerNbt = storeList(playerNbt, "LEARNED_ITEMS", playerData.LEARNED_ITEMS);

        return playerNbt;
    }

    private static PlayerData getData(CompoundTag playerNbt, PlayerData playerData) {
        playerData.NAME = playerNbt.getStringOr("NAME", "");
        playerData.EMC = playerNbt.getIntOr("EMC", 0);
        playerData.LEARNED_ITEMS = migrateLearnedItemIds(getList(playerNbt, "LEARNED_ITEMS"));

        return playerData;
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

    private static CompoundTag storeList(CompoundTag playerNbt, String key, List<String> list) {
        int listLength = list.size();
        playerNbt.putInt(key + "_SIZE", listLength);

        int index = -1;
        for (String value : list) {
            index++;
            playerNbt.putString(key + ":" + index, value);
        };

        return playerNbt;
    }

    private static List<String> getList(CompoundTag playerNbt, String key) {
        int listLength = playerNbt.getIntOr(key + "_SIZE", 0);
        List<String> list = new ArrayList<>();

        for (int i = 0; i < listLength; i++) {
            list.add(playerNbt.getStringOr(key + ":" + i, ""));
        }

        return list;
    }

    // STORE DATA

    public CompoundTag save(CompoundTag nbt) {
        nbt = storePlayersData(nbt, StateSaverAndLoader::storeData);

        return nbt;
    }

    private interface StoreDataInterface {
        CompoundTag store(CompoundTag playerNbt, PlayerData playerData);
    }

    private CompoundTag storePlayersData(CompoundTag nbt, StoreDataInterface func) {
        // PLAYER SPECIFIC

        CompoundTag playersNbt = nbt.contains("players") ? nbt.getCompoundOrEmpty("players") : new CompoundTag();
        
        players.forEach((uuid, playerData) -> {
            CompoundTag playerNbt = new CompoundTag();
            playerNbt = func.store(playerNbt, playerData);
            playersNbt.put(uuid.toString(), playerNbt);
        });

        nbt.put("players", playersNbt);

        // GLOBAL DATA
        
        CompoundTag globalNbt = new CompoundTag();
        globalNbt = func.store(globalNbt, sharedData);
        nbt.put("globalData", globalNbt);

        return nbt;
    }

    // GET DATA

    public static StateSaverAndLoader createFromNbt(CompoundTag nbt) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        state = getPlayersData(nbt, state, StateSaverAndLoader::getData);

        return state;
    }

    private interface GetDataInterface {
        PlayerData get(CompoundTag playerNbt, PlayerData playerData);
    }

    private static StateSaverAndLoader getPlayersData(CompoundTag nbt, StateSaverAndLoader state, GetDataInterface func) {
        // PLAYER SPECIFIC

        CompoundTag playersNbt = nbt.getCompoundOrEmpty("players");
        
        playersNbt.keySet().forEach(key -> {
            PlayerData playerData = new PlayerData();
            CompoundTag playerNbt = playersNbt.getCompoundOrEmpty(key);

            playerData = func.get(playerNbt, playerData);

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        // GLOBAL DATA
        
        CompoundTag globalNbt = nbt.getCompoundOrEmpty("globalData");
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

    public static void setGlobalEMC(MinecraftServer server, int emc) {
        StateSaverAndLoader serverState = getServerState(server);
        PlayerData globalData = serverState.sharedData;

        globalData.EMC = emc;
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EMCHelper.sendStateToClient((Player)player);
        }
    }

    // PLAYER MANAGER

    private static final Codec<StateSaverAndLoader> CODEC = CompoundTag.CODEC.xmap(
            StateSaverAndLoader::createFromNbt,
            state -> state.save(new CompoundTag())
    );
    private static final SavedDataType<StateSaverAndLoader> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, DissolverEnhanced.MOD_ID),
            StateSaverAndLoader::new,
            CODEC,
            null
    );
    private static final SavedDataType<StateSaverAndLoader> OLD_TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, DissolverEnhanced.OLD_MOD_ID),
            StateSaverAndLoader::new,
            CODEC,
            null
    );

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        SavedDataStorage persistentStateManager = server.getLevel(Level.OVERWORLD).getDataStorage();

        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        StateSaverAndLoader state = persistentStateManager.get(TYPE);
        if (state == null) {
            state = persistentStateManager.get(OLD_TYPE);
            if (state != null) {
                DissolverEnhanced.LOGGER.info("Migrating player EMC state from {} to {}.", DissolverEnhanced.OLD_MOD_ID, DissolverEnhanced.MOD_ID);
                persistentStateManager.set(TYPE, state);
            }
        }
        if (state == null) {
            state = new StateSaverAndLoader();
            persistentStateManager.set(TYPE, state);
        }

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        state.setDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        if (player.level().getServer() == null) return new PlayerData();

        // Either get the player by the uuid, or we don't have data for him yet, make a new player state
        PlayerData playerState = getPlayerState(player, getSaver(player));

        return playerState;
    }

    private static StateSaverAndLoader getSaver(LivingEntity player) {
        MinecraftServer server = player.level().getServer();
        return getServerState(server);
    }

    // this can also return server state if mod config is not set to "private emc"
    private static PlayerData getPlayerState(LivingEntity player, StateSaverAndLoader serverState) {
        if (ModConfig.PRIVATE_EMC) return serverState.players.computeIfAbsent(player.getUUID(), uuid -> new PlayerData());
        return serverState.sharedData;
    }

    public static void setPlayerEMC(LivingEntity player, int emc) {
        if (player.level().getServer() == null) return;

        StateSaverAndLoader serverState = getSaver(player);
        PlayerData playerState = getPlayerState(player, serverState);
        
        // store player name
        if (playerState.NAME == "") {
            String playerName = player.getDisplayName().getString();
            playerState.NAME = playerName;
        }

        playerState.EMC = emc;
        updateState(player, serverState, playerState);
    }

    public static void setPlayerLearned(LivingEntity player, List<String> learnedList) {
        if (player.level().getServer() == null) return;

        StateSaverAndLoader serverState = getSaver(player);
        PlayerData playerState = getPlayerState(player, serverState);

        playerState.LEARNED_ITEMS = learnedList;
        updateState(player, serverState, playerState);
    }

    private static void updateState(LivingEntity player, StateSaverAndLoader serverState, PlayerData playerState) {
        if (ModConfig.PRIVATE_EMC) {
            serverState.players.put(player.getUUID(), playerState);
            EMCHelper.sendStateToClient((Player)player);
        } else {
            serverState.sharedData = playerState;
            updateAllServerPlayers(player.level().getServer());
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
