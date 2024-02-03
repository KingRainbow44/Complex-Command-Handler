package tech.xigam.cch.command.modifiers;

/**
 * An identification interface.
 * Lets the deployer know that you can make sub-commands instead of action-commands.
 */
public interface Baseless {
    /**
     * @return Whether this command is baseless.
     */
    default boolean baseless() {
        return true;
    }
}
