package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.adventure.data.ShopData;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Reward;

public class ThiefShopActor extends ShopActor
{
    public ThiefShopActor(MapStage stage, int id, Array<Reward> rewardData, ShopData data)
    {
        super(stage, id, rewardData, data);
    }

    @Override
    public boolean canRestock() {
        return false;
    }

    @Override
    public float getPriceModifier() {
        return 0.66f;
    }

    @Override
    public int getRestockPrice() {
        return 0;
    }
}
