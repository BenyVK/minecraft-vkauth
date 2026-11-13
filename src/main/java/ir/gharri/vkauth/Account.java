package ir.gharri.vkauth;

/**
 * One stored account. Times are epoch millis; lastLogin = 0 means "not recorded".
 * name = the exact spelling the account was registered with (null when unknown).
 */
public record Account(String hash, long registered, long lastLogin, String name) {

    public Account(String hash, long registered, long lastLogin) {
        this(hash, registered, lastLogin, null);
    }
}
