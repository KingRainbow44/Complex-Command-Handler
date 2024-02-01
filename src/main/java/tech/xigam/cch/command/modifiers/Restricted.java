package tech.xigam.cch.command.modifiers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;

import java.util.Collection;

public interface Restricted {
    /**
     * @return Permissions required for the user to execute this command.
     */
    Collection<Permission> getPermissions();

    /**
     * Converts the list of permissions into a member permissions object.
     *
     * @return An object to be used by JDA.
     */
    default DefaultMemberPermissions toMemberPermissions() {
        return DefaultMemberPermissions.enabledFor(this.getPermissions());
    }
}
