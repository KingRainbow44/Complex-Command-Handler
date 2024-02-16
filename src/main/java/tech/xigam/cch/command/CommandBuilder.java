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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Setter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class CommandBuilder {
    private final String label, description;

    private List<SubCommand> subCommands = new ArrayList<>();
    private Consumer<Interaction> executor;
    private Consumer<Callback> callback;
    private Consumer<Completion> completer;
    private Collection<Argument> arguments;
    private Collection<Permission> permissions;

    private boolean guildOnly = false,
            nsfw = false,
            baseless = false;

    /**
     * @param subCommands Any sub-commands to be registered.
     * @return The builder.
     */
    public CommandBuilder subCommand(SubCommand... subCommands) {
        this.subCommands.addAll(Arrays.asList(subCommands));
        return this;
    }

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
        var command = new SuperCommand(this.label, this.description) {
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
            public boolean isBaseless() {
                return CommandBuilder.this.baseless;
            }
        };

        this.subCommands.forEach(command::registerSubCommand);

        return command;
    }

    /**
     * @return The command as a sub-command.
     */
    public SubCommand asSub() {
        return (SubCommand) this.build();
    }

    private static abstract class SuperCommand
            extends SubCommand implements Arguments, Restricted, Limited, Completable, Callable, Baseless {
        public SuperCommand(String label, String description) {
            super(label, description);
        }
    }
}
