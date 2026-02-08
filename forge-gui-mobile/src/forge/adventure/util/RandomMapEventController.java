package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import forge.Forge;
import forge.adventure.character.PlayerSprite;
import forge.adventure.character.ThiefShopActor;
import forge.adventure.data.*;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.DuplicateScene;
import forge.adventure.scene.NomadsBazaarScene;
import forge.adventure.scene.RewardScene;
import com.badlogic.gdx.utils.Array;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.ICardFace;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Aggregates;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RandomMapEventController
{

    private static RandomMapEventController instance;

    private final Random rand;
    private EnemyData chosenEnemy;

    private enum RandomEventType {
        DANGEROUS_ENEMY,
        THIEF_MERCHANT,
        HIGH_STAKES_DUEL,
        NOMADS_BAZAAR,
        GOLD_CHEST,
        RIVER,
        LOST_CARD,
        DUPLICATE
    }

    private static final RandomEventType[] EVENT_TYPES = RandomEventType.values();

    private RandomMapEventController()
    {
        this.rand = new Random();
    }

    public static RandomMapEventController getInstance()
    {
        if (instance == null)
        {
            instance = new RandomMapEventController();
        }
        return instance;
    }

    /**
     * Builds and returns a DialogData tree for a random event.
     */
    public DialogData getRandomEvent()
    {
        int index = rand.nextInt(EVENT_TYPES.length);

        return buildRandomEvent(index);
    }

    private DialogData buildRandomEvent(int index) {
        if (index < 0 || index >= EVENT_TYPES.length) {
            index = 0;
        }

        RandomEventType type = EVENT_TYPES[index];

        switch (type) {
            case DANGEROUS_ENEMY:
                return buildDangerousEnemyEvent();
            case THIEF_MERCHANT:
                return buildThiefMerchantEvent();
            case HIGH_STAKES_DUEL:
                return buildHighStakesDuelEvent();
            case NOMADS_BAZAAR:
                return buildNomadsBazaarEvent();
            case GOLD_CHEST:
                return buildGoldChestEvent();
            case RIVER:
                return buildRiverEvent();
            case LOST_CARD:
                return buildLostCardEvent();
            case DUPLICATE:
                return buildDuplicateCardEvent();
            default:
                return buildGoldChestEvent();
        }
    }

    // === Individual event builders ===

    /**
     * 1. Dangerous enemy guarding a hoard.
     */
    private DialogData buildDangerousEnemyEvent()
    {
        DialogData root = new DialogData();
        chosenEnemy = pickRandomDangerousEnemy();
        String enemyName;
        if (chosenEnemy != null)
        {
            enemyName = chosenEnemy.getName();
        }
        else 
        {
            enemyName = "Error of Ruto";
        }

        root.text =
                "You discover a dangerous " + enemyName + " guarding a hoard of valuable spells.\n\n"
                        + "Do you want to fight this enemy for the spoils?\n"
                        + "(Note: this enemy will start with a random card from their deck on the battlefield!)";

        DialogData opt1 = new DialogData();
        opt1.name = "Yes, duel this monster";

        DialogData opt2 = new DialogData();
        opt2.name = "Not interested";

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                startDangerousEnemyDuel();
            }
        };

        root.options = new DialogData[]{opt1, opt2};
        return root;
    }

    private void startDangerousEnemyDuel()
    {
        if (chosenEnemy == null) {
            System.out.println("DangerousEnemy Debug -> tried to start duel, but no enemy was chosen.");
            return;
        }

        WorldStage.getInstance().startBattleAgainst(chosenEnemy, true, false);
    }

    private void startHighStakesDuel()
    {
        if (chosenEnemy == null) {
            System.out.println("HighStakes Debug -> tried to start duel, but no enemy was chosen.");
            return;
        }

        WorldStage.getInstance().startBattleAgainst(chosenEnemy, false, true);
    }

    private EnemyData pickRandomDangerousEnemy() {
        World world = WorldSave.getCurrentSave().getWorld();
        PlayerSprite player = WorldStage.getInstance().getPlayerSprite();
        int currentBiome = World.highestBiome(
                world.getBiome((int) player.getX() / world.getTileSize(),
                        (int) player.getY() / world.getTileSize()));

        try {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
            if (handle.exists()) {
                @SuppressWarnings("unchecked")
                Array<EnemyData> enemyJSON = json.fromJson(Array.class, EnemyData.class, handle);

                enemyJSON = dangerousEnemyFilter(enemyJSON, currentBiome);

                if (enemyJSON.size == 0) {
                    System.out.println("DangerousEnemy Debug -> no enemies available after filter.");
                    return null;
                }

                int idx = rand.nextInt(enemyJSON.size);
                return enemyJSON.get(idx);
            } else {
                System.out.println("DangerousEnemy Debug -> ENEMIES file not found at " + Paths.ENEMIES);
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private EnemyData pickRandomHighStakesEnemy() {
        World world = WorldSave.getCurrentSave().getWorld();
        PlayerSprite player = WorldStage.getInstance().getPlayerSprite();
        int currentBiome = World.highestBiome(
                world.getBiome((int) player.getX() / world.getTileSize(),
                        (int) player.getY() / world.getTileSize()));

        try {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
            if (handle.exists()) {
                @SuppressWarnings("unchecked")
                Array<EnemyData> enemyJSON = json.fromJson(Array.class, EnemyData.class, handle);

                enemyJSON = HighStakesFilter(enemyJSON, currentBiome);

                if (enemyJSON.size == 0) {
                    System.out.println("HighStakes Debug -> no enemies available after filter.");
                    return null;
                }

                int idx = rand.nextInt(enemyJSON.size);
                return enemyJSON.get(idx);
            } else {
                System.out.println("HighStakes Debug -> ENEMIES file not found at " + Paths.ENEMIES);
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Array<EnemyData> dangerousEnemyFilter(Array<EnemyData> enemyJSON, int currentBiome)
    {
        String biomeTag   = getCurrentBiomeTag(currentBiome);

        for (int i = enemyJSON.size - 1; i >= 0; i--)
        {
            EnemyData e = enemyJSON.get(i);

            // 1) Life band
            if (e.life < 20 || e.life > 35)
            {
                enemyJSON.removeIndex(i);
                continue;
            }

            // 2) Biome tag (if we know it)
            if (biomeTag != null && biomeTag.length() > 0)
            {
                if (!hasTag(e.questTags, biomeTag))
                {
                    enemyJSON.removeIndex(i);
                    continue;
                }
            }
        }
        return enemyJSON;
    }

    private Array<EnemyData> HighStakesFilter(Array<EnemyData> enemyJSON, int currentBiome)
    {
        String biomeTag   = getCurrentBiomeTag(currentBiome);

        for (int i = enemyJSON.size - 1; i >= 0; i--)
        {
            EnemyData e = enemyJSON.get(i);

            // 1) Life band
            if (e.life > 35)
            {
                enemyJSON.removeIndex(i);
                continue;
            }

            // 2) Biome tag (if we know it)
            if (biomeTag != null && biomeTag.length() > 0)
            {
                if (!hasTag(e.questTags, biomeTag))
                {
                    enemyJSON.removeIndex(i);
                    continue;
                }
            }
        }
        return enemyJSON;
    }

    private String getCurrentBiomeTag(int currentBiome)
    {
        String currentBiomeTag = getBiomeQuestTagForIndex(currentBiome);
        return currentBiomeTag;
    }

    private boolean hasTag(String[] tags, String wanted) {
        if (tags == null || wanted == null) {
            return false;
        }
        for (String t : tags) {
            if (t != null && t.equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    private String getBiomeQuestTagForIndex(int biomeIndex) {
        switch (biomeIndex) {
            case 1: return "BiomeColorless";
            case 2: return "BiomeWhite";
            case 3: return "BiomeBlue";
            case 4: return "BiomeBlack";
            case 5: return "BiomeRed";
            case 6: return "BiomeGreen";
            default: return null;
        }
    }

    public void openDangerousEnemyRewards()
    {
        //3–5 Rare/Mythic cards + chance at 1 restricted card
        Array<Reward> rewards = new Array<>();

        RewardData rarePool = new RewardData();
        rarePool.type = "card";
        rarePool.probability = 1.0f;
        rarePool.count = 3;        // base
        rarePool.addMaxCount = 2;  // up to +2 more
        rarePool.rarity = new String[]{"Rare", "Mythic Rare"};

        rewards.addAll(rarePool.generate(false, true));

        // 10%: one restricted card (if any exist)
        if (rand.nextFloat() < 0.1f) {
            RewardData restrictedPool = new RewardData();
            restrictedPool.type = "card";
            restrictedPool.probability = 1.0f;
            restrictedPool.count = 1;
            restrictedPool.addMaxCount = 0;
            // You can refine this to pick from Restricted list, etc.
            // For now maybe just another rare/mythic or special filter.
            restrictedPool.rarity = new String[]{"Mythic Rare"};
            rewards.addAll(restrictedPool.generate(false, true));
        }

        // Open as a normal reward choice screen (not a shop)
        RewardScene.instance().loadRewards(rewards, RewardScene.Type.Loot, null);
        Forge.switchScene(RewardScene.instance());
    }

    public void openHighStakesRewards()
    {
        //3–5 random cards + 3000 gold
        Array<Reward> rewards = new Array<>();

        RewardData cardPool = new RewardData();
        cardPool.type = "card";
        cardPool.probability = 1.0f;
        cardPool.count = 3;        // base
        cardPool.addMaxCount = 2;  // up to +2 more

        rewards.addAll(cardPool.generate(false, true));

        Reward goldPool = new Reward(3000);
        rewards.addAll(goldPool);

        // Open as a normal reward choice screen (not a shop)
        RewardScene.instance().loadRewards(rewards, RewardScene.Type.Loot, null);
        Forge.switchScene(RewardScene.instance());
    }

    /**
     * 2. Thieves' guild merchant with discounted stolen goods.
     */
    private DialogData buildThiefMerchantEvent()
    {
        DialogData root = new DialogData();
        root.text =
                "A ramshackle cart and a shifty-looking fellow greet you.\n\n"
                        + "He offers to let you buy some cards from his cart. "
                        + "It is obvious these are stolen goods, and he's trying to get rid of them for a low price.";

        DialogData opt1 = new DialogData();
        opt1.name = "See what he has";

        DialogData opt2 = new DialogData();
        opt2.name = "Not interested";

        root.options = new DialogData[]{opt1, opt2};

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                openThiefMerchantShop();
            }
        };
        return root;
    }

    /**
     * 3. High-stakes duel: big payout or lose half your gold.
     */
    private DialogData buildHighStakesDuelEvent()
    {
        DialogData root = new DialogData();
        chosenEnemy = pickRandomHighStakesEnemy();
        String enemyName;
        if (chosenEnemy != null)
        {
            enemyName = chosenEnemy.getName();
        }
        else
        {
            enemyName = "Error of Ruto";
        }

        root.text =
                "You are confronted by an egotistical " + enemyName + " who immediately challenges you to a duel.\n\n"
                        + "If you win, you get cards plus 3000 gold!\n"
                        + "But if you lose, you will lose half of your gold!.\n"
                        + "(FAIR FIGHT: no starting cards, no item bonuses, both start at 20 life)";


        DialogData opt1 = new DialogData();
        opt1.name = "Let's duel!";

        DialogData opt2 = new DialogData();
        opt2.name = "Not interested";

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                startHighStakesDuel();
            }
        };

        root.options = new DialogData[]{opt1, opt2};
        return root;
    }

    /**
     * 4. Nomad's Bazaar: buy (almost) any card.
     */
    private DialogData buildNomadsBazaarEvent()
    {
        DialogData root = new DialogData();
        root.text =
                "You spot a collection of large pavilion tents with numerous merchants.\n\n"
                        + "This is a wandering Nomad's Bazaar!\n"
                        + "You can buy (almost) any card you want here!";

        DialogData opt1 = new DialogData();
        opt1.name = "Let's go shopping";

        DialogData opt2 = new DialogData();
        opt2.name = "Not interested";

        root.options = new DialogData[]{opt1, opt2};

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                openNomadsBazaar();
            }
        };
        return root;
    }

    private DialogData buildGoldChestEvent()
    {
        DialogData root = new DialogData();
        final int goldAmount = rand.nextInt(500) + 750;
        root.text =
                "While traveling, you spot something metal sticking out from under a boulder.\n\n"
                + "It's a treasure chest with gold!\n"
                + "You gain " + goldAmount + " gold!";
        DialogData opt1 = new DialogData();
        opt1.name = "Pocket gold and continue";

        root.options = new DialogData[]{ opt1};

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().addGoldp(goldAmount);
            }
        };

        return root;
    }

    private DialogData buildRiverEvent()
    {
        DialogData root = new DialogData();
        final int goldLost = Math.min(AdventurePlayer.current().getGold(), 500);
        final int shardsLost = Math.min(AdventurePlayer.current().getShards(), 25);
        final int lifeLost = Math.min(AdventurePlayer.current().getLife(), 3);

        root.text =
                "While crossing a river, the flimsy bridge breaks under you!\n\n"
                        + "Some of your valuables plummet toward the abyss. You have only a moment to react.\n";
        DialogData opt1 = new DialogData();
        opt1.name = "Lose " + goldLost + " gold";
        DialogData opt2 = new DialogData();
        opt2.name = "Lose " + shardsLost + " shards";
        DialogData opt3 = new DialogData();
        opt3.name = "Save both, but lose " + lifeLost + " life.";

        root.options = new DialogData[]{ opt1, opt2, opt3};

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().takeGold(goldLost);
            }
        };
        opt2.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().takeShards(shardsLost);
            }
        };
        opt3.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().loseLife(lifeLost);
            }
        };

        return root;
    }

    private DialogData buildLostCardEvent()
    {
        DialogData root = new DialogData();
        root.text =
                "You spot a loose card someone lost on the side of the road.\n\n"
                        + "Well, it's yours now! ";

        DialogData opt1 = new DialogData();
        opt1.name = "Add card to collection...";

        root.options = new DialogData[]{opt1};

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                obtainLostCard();
            }
        };
        return root;
    }

    //additional methods needed

    private void openThiefMerchantShop() {
        // 1) Build temporary ShopData
        ShopData data = new ShopData();
        data.name = "Thieves' Cart";
        data.description = "A shady merchant selling stolen goods at a discount.";
        data.restockPrice = 0;
        data.spriteAtlas = "";
        data.sprite = "";
        data.unlimited = false;

        // 2) Build RewardData template that only allows Rare/MythicRare
        RewardData rd = new RewardData();
        rd.type = "randomCard";
        rd.count = 8;
        rd.addMaxCount = 0;
        rd.rarity = new String[] { "Rare", "MythicRare" };

        // 3) Generate the actual rewards
        Array<Reward> rewards = new Array<Reward>();
        rewards.addAll(rd.generate(false, true));

        // 4) Create a ThiefShopActor
        int fakeId = -999; //
        ThiefShopActor thiefShop = new ThiefShopActor(MapStage.getInstance(), fakeId, rewards, data);

        // 5) Open the RewardScene as a Shop
        RewardScene.instance().loadRewards(rewards, RewardScene.Type.Shop, thiefShop);
        Forge.switchScene(RewardScene.instance());
    }

    private void openNomadsBazaar() {
        // 1) Build list of all candidate faces, excluding restricted and banned cards
        List<ICardFace> faces = FModel.getMagicDb().getCommonCards().streamAllFaces()
                .filter(face -> !CardUtil.isRestricted(face.getName()))
                .filter(face -> !CardUtil.isBanned(face.getName()))
                .collect(Collectors.toList());

        if (faces.isEmpty()) {
            //FOptionPane.showMessageDialog("No cards are available in the Nomad's Bazaar right now.");
            return;
        }

        NomadsBazaarScene.instance().reset();

        Forge.switchScene(NomadsBazaarScene.instance());
    }

    private void obtainLostCard()
    {
        RewardData rd = new RewardData();
        rd.type = "randomCard";
        rd.count = 1;                  // only generate 1 card
        rd.addMaxCount = 0;
        Array<Reward> rewards = new Array<Reward>();
        rewards.addAll(rd.generate(false, true));
        if (rewards.size == 0) {
            System.out.println("LostCard: no reward generated.");
            return;
        }
        RewardScene.instance().loadRewards(rewards, RewardScene.Type.Loot, null);
        Forge.switchScene(RewardScene.instance());
    }

    /**
     * 7. Opportunity to duplicate a card
     */
    private DialogData buildDuplicateCardEvent()
    {
        DialogData root = new DialogData();

        root.text =
                "A swirling portal opens before you! A being of light steps forth and says:\n\n"
                        + "I hunger for shards... so hungry.\n"
                        + "If you give me some, I can duplicate any card you own.";

        DialogData opt1 = new DialogData();
        opt1.name = "Pay 25 shards: Duplicate a non-restricted card";

        DialogData opt2 = new DialogData();
        opt2.name = "Pay 200 shards: Duplicate a restricted card";

        DialogData opt3 = new DialogData();
        opt3.name = "Not interested";

        if(AdventurePlayer.current().getShards() < 25)
        {
            opt1.isDisabled = true;
        }

        if(AdventurePlayer.current().getShards() < 200 || !CardUtil.hasRestricted(AdventurePlayer.current().getCollectionCards(true)))
        {
            opt2.isDisabled = true;
        }

        opt1.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().takeShards(25);
                startCardDuplicator(false);
            }
        };

        opt2.callback = new Consumer() {
            @Override
            public void accept(Object ignored) {
                AdventurePlayer.current().takeShards(200);
                startCardDuplicator(true);
            }
        };

        root.options = new DialogData[]{opt1, opt2, opt3};
        return root;
    }

    private void startCardDuplicator(boolean useRestricted)
    {
        AdventurePlayer ap = AdventurePlayer.current();

        CardPool pool = ap.getCollectionCards(true);
        Array<PaperCard> duplicateOptions = new Array<>();

        for (Map.Entry<PaperCard, Integer> entry : pool)
        {
            PaperCard pc = entry.getKey();
            Integer count = entry.getValue();

            if (pc == null || count == null || count <= 0 || pc.isVeryBasicLand())
            {
                continue;
            }
            if(useRestricted && !CardUtil.isRestricted(pc))
            {
                continue;
            }
            if(!useRestricted && CardUtil.isRestricted(pc))
            {
                continue;
            }
            duplicateOptions.add(pc);
        }

        DuplicateScene.getInstance().setDuplicateOptions(duplicateOptions);
        Forge.switchScene(DuplicateScene.getInstance());
    }
}
