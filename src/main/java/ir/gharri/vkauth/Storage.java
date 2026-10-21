package ir.gharri.vkauth;

import java.util.List;
import java.util.UUID;

public interface Storage {

    String name();

    void init() throws Exception;

    /** @return the stored account, or null if it does not exist */
    Account getAccount(UUID uuid) throws Exception;

    void saveAccount(UUID uuid, String name, Account account) throws Exception;

    void updateLastLogin(UUID uuid, long time) throws Exception;

    /** Every stored account (empty list if there is nothing). Works even if init() was not called. */
    List<StoredAccount> getAll() throws Exception;

    /** Deletes the whole storage (accounts.yml file / MySQL table). Used after a migration. */
    void deleteAll() throws Exception;

    void close();
}
