package ir.gharri.vkauth;

import java.util.UUID;

/** An account together with its owner's uuid and name (used for migration). */
public record StoredAccount(UUID uuid, String name, Account account) {
}
