package database;

import main.models.User;
import main.models.FarmElement;

public class UpgradeManager {
    public static boolean upgradeItem(User user, FarmElement element) {
        int upgradeCost = element.getUpgradeCost();
        if (user.getCoins() >= upgradeCost && element.getLevel() < user.getLevel()) {
            user.setCoins(user.getCoins() - upgradeCost);
            element.upgrade();
            BarnDAO.saveBarn(user);
            return true;
        }
        return false;
    }
}
