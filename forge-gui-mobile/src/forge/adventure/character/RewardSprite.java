package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.adventure.data.RewardData;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.item.PaperCard;

import java.util.Arrays;
import java.util.Random;

/**
 * RewardSprite
 * Character sprite that represents reward pickups.
 */

public class RewardSprite extends CharacterSprite {
    private final static String default_reward = "[\n" +
            "\t\t{\n" +
            "\t\t\t\"type\": \"gold\",\n" +
            "\t\t\t\"count\": 10,\n" +
            "\t\t\t\"addMaxCount\": 100,\n" +
            "\t\t}\n" +
            "\t]";

    private int id;
    private RewardData[] rewards = null;

    public RewardSprite(String data, String _sprite){
        super(_sprite);
        if (data != null) {
            rewards = JSONStringLoader.parse(RewardData[].class, data, default_reward);
        } else { //Shouldn't happen, but make sure it doesn't fly by.
            System.err.print("Reward data is null. Using a default reward.");
            rewards = JSONStringLoader.parse(RewardData[].class, default_reward, default_reward);
        }
    }

    public RewardSprite(int _id, String data, String _sprite){
        this(data, _sprite);
        this.id = _id; //The ID is for remembering removals.
    }

    @Override
    void updateBoundingRect() { //We want rewards to take a full tile.
        boundingRect.set(getX(), getY(), getWidth(), getHeight());
    }

    public Array<Reward> getRewards() { //Get list of rewards.
        Array<Reward> ret = new Array<>();
        if (rewards == null) {
            return ret;
        }

        Random rewardRandom = new Random();

        boolean hasCardReward = false;
        for (RewardData rdata : rewards) {
            String t = rdata.type;
            if (t == null || t.isEmpty()) {
                t = "randomCard";
            }
            if ("card".equalsIgnoreCase(t)
                    || "randomCard".equalsIgnoreCase(t)
                    || "deckCard".equalsIgnoreCase(t)) {
                hasCardReward = true;
                break;
            }
        }

        if (hasCardReward) {
            try {
                var configData = Config.instance().getConfigData();
                String[] restricted = configData.restrictedCards;

                if (restricted != null && restricted.length > 0) {
                    int rollRestricted = rewardRandom.nextInt(100); // 0–99
                    if (rollRestricted < 10) {
                        String chosenName = restricted[rewardRandom.nextInt(restricted.length)];
                        PaperCard card = CardUtil.getCardByName(chosenName);

                        if (card != null) {
                            ret.add(new Reward(card, false)); // noSell = false so player can sell it
                            return ret; // skip normal chest generation
                        } else {
                            System.err.println("[RewardSprite] Restricted card not found: " + chosenName + " – falling back to normal chest loot.");
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("[RewardSprite] Error generating restricted chest: " + ex.getMessage());
            }
        }

        //If it doesn't generate a restricted-list card, continue with this logic
        for (RewardData rdata : rewards) {
            String type = rdata.type;
            if (type == null || type.isEmpty()) {
                type = "randomCard";
            }

            boolean isCardReward =
                    "card".equalsIgnoreCase(type)
                            || "randomCard".equalsIgnoreCase(type)
                            || "deckCard".equalsIgnoreCase(type);

            if (!isCardReward) {
                // Non-card rewards (gold, shards, items, life, etc.) – unchanged
                ret.addAll(rdata.generate(false, true));
                continue;
            }

            // ---- Card reward: apply custom rarity distribution per card ----

            int baseCount = rdata.count;
            int maxCount = Math.round(rdata.addMaxCount * Current.player().getDifficulty().rewardMaxFactor);
            int addedCount = (maxCount > 0 ? rewardRandom.nextInt(maxCount) : 0);
            int totalCards = baseCount + addedCount;

            for (int i = 0; i < totalCards; i++) {
                // 10% Mythic, 50% Rare, 40% Uncommon
                int roll = rewardRandom.nextInt(100);
                String rarityString;
                if (roll < 10) {
                    rarityString = "MythicRare";
                } else if (roll < 60) {
                    rarityString = "Rare";
                } else {
                    rarityString = "Uncommon";
                }

                // Clone original RewardData to preserve all filters (colors, text, etc.)
                RewardData oneCard = new RewardData(rdata);
                oneCard.count = 1;
                oneCard.addMaxCount = 0;
                oneCard.rarity = new String[]{rarityString};

                ret.addAll(oneCard.generate(false, true));
            }
        }

        return ret;
    }

    public int getId() {
        return id;
    }
}
