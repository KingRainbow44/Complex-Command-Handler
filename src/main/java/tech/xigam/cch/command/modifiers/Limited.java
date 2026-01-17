package tech.xigam.cch.command.modifiers;

import net.dv8tion.jda.api.interactions.InteractionContextType;
import tech.xigam.cch.command.Command;

/**
 * Declares a command as limited, allowing you to change:
 * - if it is shown to underage users
 * - if it showed in guilds only
 */
public interface Limited {
    /**
     * @return Whether the command is only accessible in guilds.
     * @deprecated Use {@link Command#setContext(InteractionContextType...)} instead.
     */
    @Deprecated
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
