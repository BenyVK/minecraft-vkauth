package ir.gharri.vkauth;

import java.util.UUID;

public interface Storage {

    String name();

    void init() throws Exception;

    /** @return the stored account, or null if it does not exist */
    Account getAccount(UUID uuid) throws Exception;

    void saveAccount(UUID uuid, String name, Account account) throws Exception;

    void updateLastLogin(UUID uuid, long time) throws Exception;

    void close();
}
