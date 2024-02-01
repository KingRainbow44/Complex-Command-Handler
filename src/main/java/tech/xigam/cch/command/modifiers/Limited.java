package tech.xigam.cch.command.modifiers;

/**
 * Declares a command as limited, allowing you to change:
 * - if it is shown to underage users
 * - if it showed in guilds only
 */
public interface Limited {
    /**
     * @return Whether the command is only accessible in guilds.
     */
    default boolean isGuildOnly() {
        return false;
    }

    /**
     * @return Whether the command is NSFW.
     */
    default boolean isNsfw() {
        return false;
    }
}
