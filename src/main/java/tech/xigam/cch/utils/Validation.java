package tech.xigam.cch.utils;

import java.util.Set;

public interface Validation {
    /**
     * Checks if the given string is a URL using a Regex.
     *
     * @param url The URL to check.
     * @return Whether the URL is valid.
     */
    static boolean isUrl(String url) {
        return url.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    }

    /**
     * Checks if a set contains the following values only.
     *
     * @param set The set to check.
     * @param values The values to check for.
     * @return Whether the set contains the values only.
     * @param <T> The type of the set.
     */
    static <T> boolean isOnly(Set<T> set, T... values) {
        for (T value : values) {
            if (!set.contains(value)) {
                return false;
            }
        }
        return true;
    }
}
