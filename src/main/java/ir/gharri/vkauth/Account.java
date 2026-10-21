package ir.gharri.vkauth;

/** One stored account. Times are epoch millis; lastLogin = 0 means "not recorded". */
public record Account(String hash, long registered, long lastLogin) {
}
