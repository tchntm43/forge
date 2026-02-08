package forge.adventure.util;

import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.DialogData;
import forge.adventure.data.EnemyData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.TradeScene;
import forge.adventure.stage.WorldStage;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.MyRandom;

import java.util.*;
import java.util.function.Consumer;

public final class TradeController
{
    private static TradeController instance;
    private EnemySprite currentMob;
    private EnemyData currentEnemy;
    private Deck enemyDeck;
    private CardPool allPlayerCards;
    private Array<PaperCard> approvedPlayerTradeCards;
    private Array<PaperCard> enemyTradeCards;
    private Array<PaperCard> enemyCardsWanted;
    private Map<PaperCard, Integer> enemyCardPrices;
    private Array<PaperCard> removedCards;

    public Array<PaperCard> getCounterOffer_playerCards()
    {
        return counterOffer_playerCards;
    }

    public Array<PaperCard> getCounterOffer_enemyCards()
    {
        return counterOffer_enemyCards;
    }

    private Array<PaperCard> counterOffer_playerCards;
    private Array<PaperCard> counterOffer_enemyCards;
    public boolean tradeFound;

    private Array<PaperCard> playerCardsWanted;

    public static TradeController getInstance()
    {
        if(instance == null)
        {
            instance = new TradeController();
        }
        return instance;
    }

    private TradeController()
    {
        approvedPlayerTradeCards = new Array<>();
        enemyTradeCards          = new Array<>();
    }

    public EnemySprite getCurrentMob()
    {
        return this.currentMob;
    }

    public void setCurrentEnemy(EnemySprite mob)
    {
        this.currentEnemy = (mob == null) ? null : mob.getData();
    }

    // Use this method only with Adventure Mode
    public DialogData startTrading(EnemySprite mob)
    {
        currentMob = mob;
        setCurrentEnemy(mob);
        DialogData root = new DialogData();
        root.text = "This " + mob.getName() + " would rather trade cards than fight.\n\n"
                + "Do you want to trade cards instead?";

        // Option 1: say yes – we'll flesh this out later
        DialogData optYes = new DialogData();
        optYes.name = "Yes, let's trade!";
        optYes.callback = new Consumer<Object>() {
            @Override
            public void accept(Object ignored) {
                initTradeData();
                Forge.switchScene(TradeScene.getInstance());
            }
        };

        // Option 2: decline, remove mob and clear collision state
        DialogData optNo = new DialogData();
        optNo.name = "Not interested";
        optNo.callback = new Consumer<Object>() {
            @Override
            public void accept(Object ignored)
            {
                WorldStage.getInstance().onMobTradeFinished(mob, true);
            }
        };

        root.options = new DialogData[] { optYes, optNo };

        return root;
    }

    private void initTradeData()
    {
        AdventurePlayer ap = AdventurePlayer.current();

        CardPool collection = ap.getCollectionCards(true);

        CardPool tradable = new CardPool();

        for (Map.Entry<PaperCard, Integer> entry : collection) {
            PaperCard pc  = entry.getKey();
            Integer owned = entry.getValue();

            if (pc == null || owned == null || owned <= 0) {
                continue;
            }

            // How many copies are tied up in decks (max across all decks)
            int used = ap.getCopiesUsedInDecks(pc);
            int free = owned - used;

            if (free > 0)
            {
                if (pc.isVeryBasicLand()) continue;

                tradable.add(pc, free);
            }
        }

        allPlayerCards = tradable;
    }

    public void finishTrade(boolean removeMob)
    {
        if (currentMob != null) {
            WorldStage.getInstance().onMobTradeFinished(currentMob, removeMob);
            currentMob = null;
        }

        // Return to map scene
        Forge.switchToLast();
    }

    public CardPool getTradablePlayerCards() {
        return allPlayerCards;
    }

    public Array<PaperCard> getApprovedPlayerTradeCards() {
        return approvedPlayerTradeCards;
    }

    public void setApprovedPlayerTradeCards()
    {
        //only used if there are cards in tradeBinder
        approvedPlayerTradeCards = new Array<>();

        if(AdventurePlayer.current().getTradeBinder().countAll() > 0)
        {
            for(Map.Entry<PaperCard, Integer> entry : AdventurePlayer.current().getTradeBinder())
            {
                PaperCard card = entry.getKey();
                int amount = entry.getValue();
                for(int i =0; i < amount; i++)
                {
                    approvedPlayerTradeCards.add(card);
                }
            }
        }
    }

    public void setApprovedPlayerTradeCards(Array<TextraButton> buttons) {
        approvedPlayerTradeCards = new Array<>();

        if (buttons == null || buttons.size == 0) {
            return;
        }

        for (TextraButton btn : buttons) {
            if (btn == null) {
                continue;
            }

            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData)) {
                continue;
            }

            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;

            if (data.excluded) {
                continue;
            }

            if (data.card == null || data.count <= 0) {
                continue;
            }
            for(int i = 0; i < data.count; i++)
            {
                approvedPlayerTradeCards.add(data.card);
            }
        }
    }

    public void setEnemyTradeCards()
    {
        //This method should pick cards based on what the player has offered in approvedPlayerTradeCards
        //It should always generate 5-10 cards (excluding basic lands) from one of the enemy's decks, regardless of rarity
        //It should pick 1-5 additional non-restricted random cards that are derived based on the highest value among non-restricted cards in approvedPlayerTradeCards
        //If there are restricted cards in approvedPlayerTradeCards, it has a 50% chance to generate 1 random restricted card as trade.
        enemyTradeCards = new Array<>();

        if (currentEnemy == null)
        {
            return;
        }

        //--- Part 1: Cards directly from enemy deck
        try
        {
            String[] decks = currentEnemy.deck;
            if (decks == null || decks.length == 0)
            {
                return;
            }

            String chosenDeckPath;
            if (decks.length == 1)
            {
                chosenDeckPath = decks[0];
            }
            else
            {
                int idx = MyRandom.getRandom().nextInt(decks.length);
                chosenDeckPath = decks[idx];
            }

            enemyDeck = CardUtil.getDeck(
                    chosenDeckPath,
                    true,
                    false,
                    currentEnemy.colors,
                    currentEnemy.life > 13,
                    false
            );

            if (enemyDeck == null)
            {
                return;
            }

            CardPool main = enemyDeck.getMain();
            if (main == null || main.isEmpty())
            {
                return;
            }

            List<PaperCard> candidates = new ArrayList<>();

            for (Map.Entry<PaperCard, Integer> entry : main) {
                PaperCard pc = entry.getKey();
                if (pc == null)
                {
                    continue;
                }
                if (pc.isVeryBasicLand())
                {
                    continue;
                }
                if (CardUtil.isBanned(pc.getName()))
                {
                    continue;
                }

                boolean alreadyAdded = false;
                for (PaperCard existing : candidates)
                {
                    if (existing.getName().equals(pc.getName()))
                    {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded)
                {
                    candidates.add(pc);
                }
            }

            if (candidates.isEmpty())
            {
                return;
            }

            int max = Math.min(10, candidates.size());
            int min = Math.min(5, max);

            int range = max - min;
            int countToPick = (range > 0)
                    ? (min + MyRandom.getRandom().nextInt(range + 1))
                    : min;

            java.util.Collections.shuffle(candidates, MyRandom.getRandom());

            for (int i = 0; i < countToPick; i++)
            {
                PaperCard pc = candidates.get(i);
                enemyTradeCards.add(pc);
            }

            //---Part 2: 1-5 random cards based on value of highest non-restricted card in player offer
            Array<PaperCard> approvedNoRestricted = new Array<>();
            for(PaperCard testCard : approvedPlayerTradeCards)
            {
                if(!CardUtil.isRestricted(testCard) && !CardUtil.isBanned(testCard))
                {
                    approvedNoRestricted.add(testCard);
                }
            }
            PaperCard highestCard = getHighestValueCard(approvedNoRestricted);
            Array<PaperCard> possibleCards = new Array<>();
            int refPrice = CardUtil.getCardPrice(highestCard);
            int band15Low  = (int)Math.floor(refPrice * 0.85);
            int band15High = (int)Math.ceil(refPrice * 1.15);
            int band40Low  = (int)Math.floor(refPrice * 0.60);
            int band40High = (int)Math.ceil(refPrice * 1.40);
            Array<PaperCard> exactMatches = new Array<>();
            Array<PaperCard> band15       = new Array<>();
            Array<PaperCard> band40       = new Array<>();
            for (PaperCard testCard : CardUtil.getFullCardPool(false))
            {
                if (testCard == null)
                {
                    continue;
                }
                if (CardUtil.isBanned(testCard) || CardUtil.isRestricted(testCard) || testCard.isVeryBasicLand())
                {
                    continue;
                }
                int price = CardUtil.getCardPrice(testCard);
                if (price == refPrice)
                {
                    exactMatches.add(testCard);
                }
                else if (price >= band15Low && price <= band15High)
                {
                    band15.add(testCard);
                }
                else if (price >= band40Low && price <= band40High)
                {
                    band40.add(testCard);
                }
            }
            possibleCards.addAll(exactMatches);
            if (possibleCards.size < 10)
            {
                possibleCards.addAll(band15);
            }
            if (possibleCards.size < 10)
            {
                possibleCards.addAll(band40);
            }
            if (possibleCards.size > 0)
            {
                int numExtra = 1 + MyRandom.getRandom().nextInt(5);

                for (int i = 0; i < numExtra; i++)
                {
                    int idx = MyRandom.getRandom().nextInt(possibleCards.size);
                    PaperCard chosen = possibleCards.get(idx);
                    enemyTradeCards.add(chosen);
                }
            }

            //---Part 3: If player has offered a restricted-list card, enemy has 50% chance of offering 1 random restricted list card
            boolean playerHasRestricted = false;
            for(PaperCard testCard : approvedPlayerTradeCards)
            {
                if(CardUtil.isRestricted(testCard))
                {
                    playerHasRestricted = true;
                    break;
                }
            }
            if(playerHasRestricted)
            {
                int randomRestricted = MyRandom.getRandom().nextInt(2);
                if(randomRestricted == 0)
                {
                    String[] restrictedNames = Config.instance().getConfigData().restrictedCards;

                    if (restrictedNames != null && restrictedNames.length > 0)
                    {
                        ArrayList<PaperCard> restrictedPool = new ArrayList<>();

                        for (String name : restrictedNames)
                        {
                            if (name == null || name.isEmpty())
                            {
                                continue;
                            }

                            if (CardUtil.isBanned(name))
                            {
                                continue;
                            }

                            PaperCard pc = CardUtil.getCardByName(name);
                            if (pc != null)
                            {
                                restrictedPool.add(pc);
                            }
                        }

                        if (!restrictedPool.isEmpty())
                        {
                            int idx = MyRandom.getRandom().nextInt(restrictedPool.size());
                            PaperCard chosenRestricted = restrictedPool.get(idx);
                            enemyTradeCards.add(chosenRestricted);
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public Array<PaperCard> getEnemyTradeCards()
    {
        return enemyTradeCards;
    }

    public PaperCard getHighestValueCard(Array<PaperCard> cards)
    {
        PaperCard highestCard = null;
        for(PaperCard card : cards)
        {
            if(highestCard == null)
            {
                highestCard = card;
                continue;
            }
            if(CardUtil.getCardPrice(card) > CardUtil.getCardPrice(highestCard))
            {
                highestCard = card;
            }
        }
        return highestCard;
    }

    public void setEnemyCardsWanted(Array<TextraButton> buttons)
    {
        enemyCardsWanted = new Array<>();

        if (buttons == null || buttons.size == 0)
        {
            return;
        }

        for (TextraButton btn : buttons)
        {
            if (btn == null)
            {
                continue;
            }

            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }

            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;

            if (data.excluded || data.card == null)
            {
                continue;
            }
            enemyCardsWanted.add(data.card);
        }
    }

    public void setPlayerCardsWanted()
    {
        if (approvedPlayerTradeCards == null || approvedPlayerTradeCards.size == 0)
        {
            playerCardsWanted = new Array<>();
            return;
        }

        playerCardsWanted = new Array<>();
        Map<Integer, Integer> cardWeights = new HashMap<>();

        float propWanted = MyRandom.getRandom().nextFloat(0.25f, 0.75f);
        int numWanted = Math.round(approvedPlayerTradeCards.size * propWanted);
        if (numWanted <= 0)
        {
            numWanted = 1;
        }
        if (numWanted > approvedPlayerTradeCards.size)
        {
            numWanted = approvedPlayerTradeCards.size;
        }

        int sumWeight = 0;
        for(int i = 0; i < approvedPlayerTradeCards.size; i++)
        {
            PaperCard card = approvedPlayerTradeCards.get(i);
            if(card == null)
            {
                continue;
            }
            int weight = CardUtil.getCardPrice(card);
            if (weight <= 0)
            {
                weight = 1;
            }
            boolean inDeck = false;
            for(Map.Entry<PaperCard, Integer> entry : enemyDeck.getAllCardsInASinglePool())
            {
                String cardName = entry.getKey().getCardName();
                if(Objects.equals(cardName, approvedPlayerTradeCards.get(i).getCardName()))
                {
                    inDeck = true;
                    break;
                }
            }
            if(inDeck)
            {
                weight *= 2;
            }
            sumWeight += weight;
            cardWeights.put(i, weight);
        }

        if (cardWeights.isEmpty() || sumWeight <= 0)
        {
            return;
        }

        for(int i = 0; i < numWanted && !cardWeights.isEmpty(); i++)
        {
            int randomRoll = MyRandom.getRandom().nextInt(sumWeight);
            int accum = 0;

            Iterator<Map.Entry<Integer, Integer>> it = cardWeights.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<Integer, Integer> entry = it.next();
                accum += entry.getValue();
                if(accum >= randomRoll)
                {
                    playerCardsWanted.add(approvedPlayerTradeCards.get(entry.getKey()));
                    sumWeight -= entry.getValue();
                    it.remove();
                    break;
                }
            }
        }
    }

    public Array<PaperCard> getPlayerCardsWanted()
    {
        return playerCardsWanted;
    }

    public void setEnemyCardPrices()
    {
        enemyCardPrices = new HashMap<>();
        for(PaperCard card : enemyCardsWanted)
        {
            if(enemyCardPrices.containsKey(card))
            {
                continue;
            }
            enemyCardPrices.put(card, genEnemyPrice(card));
        }
        for(PaperCard card : playerCardsWanted)
        {
            if(enemyCardPrices.containsKey(card))
            {
                continue;
            }
            enemyCardPrices.put(card, genEnemyPrice(card));
        }
    }

    private int genEnemyPrice(PaperCard card)
    {
        int price = CardUtil.getCardPrice(card);
        float variation = 1 + MyRandom.getRandom().nextFloat(0.3f) - 0.15f;
        price = Math.round(price*variation);
        return price;
    }

    public Array<PaperCard> getEnemyCardsWanted()
    {
        return enemyCardsWanted;
    }

    public boolean playerOfferAccepted(Array<TextraButton> playerButtons, Array<TextraButton> enemyButtons)
    {
        if(playerButtons == null || playerButtons.size == 0 || enemyButtons == null || enemyButtons.size == 0)
        {
            return false;
        }

        int totalPlayerCardValue = 0;
        int totalEnemyCardValue = 0;

        for(TextraButton btn : playerButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            totalEnemyCardValue += enemyCardPrices.get(data.card);
        }
        for(TextraButton btn : enemyButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            totalPlayerCardValue += enemyCardPrices.get(data.card);
        }
        return totalPlayerCardValue >= totalEnemyCardValue;
    }

    public void exchangeCards(Array<PaperCard> enemyCards, Array<PaperCard> playerCards)
    {
        if(playerCards == null || playerCards.size == 0 || enemyCards == null || enemyCards.size == 0)
        {
            return;
        }
        AdventurePlayer ap = AdventurePlayer.current();
        for(PaperCard card : enemyCards)
        {
            ap.addCard(card, 1);
        }
        for(PaperCard card: playerCards)
        {
            if(ap.getTradeBinder().countAll() > 0)
            {
                ap.getTradeBinder().remove(card, 1);
            }
            ap.getCards().remove(card, 1);
        }
    }

    public void exchangeCardsFromButtons(Array<TextraButton> playerButtons, Array<TextraButton> enemyButtons)
    {
        if(playerButtons == null || playerButtons.size == 0 || enemyButtons == null || enemyButtons.size == 0)
        {
            return;
        }
        AdventurePlayer ap = AdventurePlayer.current();
        for(TextraButton btn : playerButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            ap.addCard(data.card, 1);
        }
        for(TextraButton btn : enemyButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            if(ap.getTradeBinder().countAll() > 0)
            {
                ap.getTradeBinder().remove(data.card, 1);
            }
            ap.getCards().remove(data.card, 1);
        }
    }

    public void createCounterOffer(Array<TextraButton> playerButtons, Array<TextraButton> enemyButtons)
    {
        if(playerButtons == null || playerButtons.size == 0 || enemyButtons == null || enemyButtons.size == 0)
        {
            return;
        }

        tradeFound = false;
        removedCards = new Array<>();
        int totalPlayerCardValue = 0;
        int totalEnemyCardValue = 0;
        Array<PaperCard> playerCardsInTrade = new Array<>();
        Array<PaperCard> enemyCardsInTrade = new Array<>();

        //create clones to work with and modify freely
        Array<PaperCard> playerPool = new Array<>();
        for(PaperCard card : playerCardsWanted)
        {
            playerPool.add(card);
        }
        Array<PaperCard> enemyPool = new Array<>();
        for(PaperCard card : enemyCardsWanted)
        {
            enemyPool.add(card);
        }

        //get existing offer and modify pools to exclude cards currently in trade offer.
        for(TextraButton btn : playerButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            for(int i = 0; i < enemyPool.size; i++)
            {
                if(Objects.equals(data.card.getCardName(), enemyPool.get(i).getCardName()))
                {
                    enemyPool.removeIndex(i);
                    break;
                }
            }
            enemyCardsInTrade.add(data.card);
            totalEnemyCardValue += enemyCardPrices.get(data.card);
        }
        for(TextraButton btn : enemyButtons)
        {
            if(btn == null)
            {
                continue;
            }
            Object obj = btn.getUserObject();
            if (!(obj instanceof TradeScene.CardButtonData))
            {
                continue;
            }
            TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
            if (data.excluded || data.card == null)
            {
                continue;
            }
            for(int i = 0; i < playerPool.size; i++)
            {
                if(Objects.equals(data.card.getCardName(), playerPool.get(i).getCardName()))
                {
                    playerPool.removeIndex(i);
                    break;
                }
            }
            playerCardsInTrade.add(data.card);
            totalPlayerCardValue += enemyCardPrices.get(data.card);
        }

        //main loop to try and make totalPlayerCardValue become >= totalEnemyCardValue
        int countIterations = 0;
        while(totalPlayerCardValue < totalEnemyCardValue)
        {
            //for testing, remove later
            countIterations++;
            System.out.println("----");
            System.out.println("loop iteration " + countIterations);
            System.out.println("totalPlayerCardValue = " + totalPlayerCardValue);
            System.out.println("totalEnemyCardValue = " + totalEnemyCardValue);

            if(playerPool.size > 0)
            {
                //add a random card the enemy wants that isn't currently in the trade
                int randIndex = MyRandom.getRandom().nextInt(playerPool.size);
                PaperCard testCard = playerPool.get(randIndex);
                System.out.println("adding " + testCard.getCardName());
                playerPool.removeIndex(randIndex);
                playerCardsInTrade.add(testCard);
                totalPlayerCardValue += enemyCardPrices.get(testCard);
            }
            else if(enemyCardsInTrade.size > 1)
            {
                //remove the least-valuable card from the list the player wants
                int leastValuable = 999999;
                PaperCard leastValuableCard = null;
                int leastIndex = 0;
                for(int i = 0; i<enemyCardsInTrade.size; i++)
                {
                    if(enemyCardPrices.get(enemyCardsInTrade.get(i)) < leastValuable)
                    {
                        leastValuableCard = enemyCardsInTrade.get(i);
                        leastValuable = enemyCardPrices.get(leastValuableCard);
                        leastIndex = i;
                    }
                }
                System.out.println("removing " + leastValuableCard.getCardName());
                enemyCardsInTrade.removeIndex(leastIndex);
                enemyPool.add(leastValuableCard);
                totalEnemyCardValue -= leastValuable;
            }
            else if(playerPool.size == 0 && enemyPool.size == 1)
            {
                //player unable to get highest value card they want, remove from consideration, reset and start again
                PaperCard removeCard = enemyPool.get(0);
                removedCards.add(removeCard);
                for(PaperCard card : enemyCardsWanted)
                {
                    if (Objects.equals(removeCard.getCardName(), card.getCardName()))
                    {
                        enemyCardsWanted.removeValue(card, true);
                        break;
                    }
                }
                playerPool.clear();
                enemyPool.clear();
                enemyCardsInTrade.clear();
                playerCardsInTrade.clear();
                totalEnemyCardValue = 0;
                totalPlayerCardValue = 0;
                for(PaperCard card : enemyCardsWanted)
                {
                    playerPool.add(card);
                }
                for(PaperCard card : playerCardsWanted)
                {
                    enemyPool.add(card);
                }
                for(TextraButton btn : playerButtons)
                {
                    if(btn == null)
                    {
                        continue;
                    }
                    Object obj = btn.getUserObject();
                    if (!(obj instanceof TradeScene.CardButtonData))
                    {
                        continue;
                    }
                    TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
                    if (data.excluded || data.card == null)
                    {
                        continue;
                    }
                    for(int i = 0; i < enemyPool.size; i++)
                    {
                        if(Objects.equals(data.card.getCardName(), enemyPool.get(i).getCardName()))
                        {
                            enemyPool.removeIndex(i);
                            break;
                        }
                    }
                    enemyCardsInTrade.add(data.card);
                    totalEnemyCardValue += enemyCardPrices.get(data.card);
                }
                for(TextraButton btn : enemyButtons)
                {
                    if(btn == null)
                    {
                        continue;
                    }
                    Object obj = btn.getUserObject();
                    if (!(obj instanceof TradeScene.CardButtonData))
                    {
                        continue;
                    }
                    TradeScene.CardButtonData data = (TradeScene.CardButtonData) obj;
                    if (data.excluded || data.card == null)
                    {
                        continue;
                    }
                    for(int i = 0; i < playerPool.size; i++)
                    {
                        if(Objects.equals(data.card.getCardName(), playerPool.get(i).getCardName()))
                        {
                            playerPool.removeIndex(i);
                            break;
                        }
                    }
                    playerCardsInTrade.add(data.card);
                    totalPlayerCardValue += enemyCardPrices.get(data.card);
                }
                for(PaperCard card : removedCards)
                {
                    for(PaperCard card2 : enemyCardsInTrade)
                    {
                        if(Objects.equals(card.getCardName(), card2.getCardName()))
                        {
                            enemyCardsInTrade.removeValue(card2, true);
                            break;
                        }
                    }
                }
            }
            if(totalPlayerCardValue >= totalEnemyCardValue)
            {
                counterOffer_enemyCards = enemyCardsInTrade;
                counterOffer_playerCards = playerCardsInTrade;
                tradeFound = true;
                break;
            }
            if(enemyPool.size == 0 && enemyCardsWanted.size == 0)
            {
                //no trade is possible
                tradeFound = false;
                break;
            }
        }
    }
}
