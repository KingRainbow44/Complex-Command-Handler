package tech.xigam.cch.command;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.Permission;
import tech.xigam.cch.command.modifiers.*;
import tech.xigam.cch.utils.Argument;
import tech.xigam.cch.utils.Callback;
import tech.xigam.cch.utils.Completion;
import tech.xigam.cch.utils.Interaction;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Setter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class CommandBuilder {
    private final String label, description;

    private Consumer<Interaction> executor;
    private Consumer<Callback> callback;
    private Consumer<Completion> completer;
    private Collection<Argument> arguments;
    private Collection<Permission> permissions;

    private boolean guildOnly = false, nsfw = false, baseless = false;

    /**
     * @param arguments The arguments to be used by the command.
     * @return The builder.
     */
    public CommandBuilder arguments(Argument... arguments) {
        this.arguments = List.of(arguments);
        return this;
    }

    /**
     * @param permissions The permissions to be used by the command.
     * @return The builder.
     */
    public CommandBuilder permissions(Permission... permissions) {
        this.permissions = List.of(permissions);
        return this;
    }

    /**
     * @return A command.
     */
    public BaseCommand build() {
        return new SuperCommand(this.label, this.description) {
            @Override
            public void execute(Interaction interaction) {
                executor.accept(interaction);
            }

            @Override
            public Collection<Argument> getArguments() {
                return CommandBuilder.this.arguments;
            }

            @Override
            public void callback(Callback callback) {
                CommandBuilder.this.callback.accept(callback);
            }

            @Override
            public void complete(Completion completion) {
                CommandBuilder.this.completer.accept(completion);
            }

            @Override
            public Collection<Permission> getPermissions() {
                return CommandBuilder.this.permissions;
            }

            @Override
            public boolean isNsfw() {
                return CommandBuilder.this.nsfw;
            }

            @Override
            public boolean isGuildOnly() {
                return CommandBuilder.this.guildOnly;
            }

            @Override
            public boolean baseless() {
                return CommandBuilder.this.baseless;
            }
        };
    }

    private static abstract class SuperCommand
            extends Command implements Arguments, Restricted, Limited, Completable, Callable, Baseless {
        public SuperCommand(String label, String description) {
            super(label, description);
        }
    }
}
