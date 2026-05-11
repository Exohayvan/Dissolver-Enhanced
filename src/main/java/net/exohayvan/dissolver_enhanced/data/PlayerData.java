package net.exohayvan.dissolver_enhanced.data;

import java.util.ArrayList;
import java.util.List;
// https://fabricmc.net/wiki/tutorial:persistent_states#more_involved_player_data
public class PlayerData {
    public String NAME = "";
    public int EMC = 0;
    public List<String> LEARNED_ITEMS = new ArrayList<>();

    // not stored
    // public int learnedItemsSize = 0;
    public int LEARNED_ITEMS_TOTAL_SIZE = 0;
    public String MESSAGE = "";
}
