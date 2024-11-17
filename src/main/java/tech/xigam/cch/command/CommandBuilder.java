package tech.xigam.cch.command;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import tech.xigam.cch.command.modifiers.*;
import tech.xigam.cch.utils.Argument;
import tech.xigam.cch.utils.Callback;
import tech.xigam.cch.utils.Completion;
import tech.xigam.cch.utils.Interaction;

import java.util.*;
import java.util.function.Consumer;

@Setter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class CommandBuilder {
    private final String label, description;

    private Set<InteractionContextType> context = new HashSet<>() {{
        this.add(InteractionContextType.GUILD);
    }};

    private List<SubCommand> subCommands = new ArrayList<>();
    private Consumer<Interaction> executor = interaction -> {};
    private Consumer<Callback> callback = callback -> {};
    private Consumer<Completion> completer = completion -> {};
    private Collection<Argument> arguments = new HashSet<>();
    private Collection<Permission> permissions = new HashSet<>();

    private boolean nsfw = false, baseless = false;

    /**
     * @param context The context of the command.
     * @return The builder.
     */
    public CommandBuilder context(Set<InteractionContextType> context) {
        this.context = context;
        return this;
    }

    /**
     * @param context The context of the command.
     * @return The builder.
     */
    public CommandBuilder context(InteractionContextType... context) {
        this.context = Set.of(context);
        return this;
    }

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
     * Placeholder method for 'setGuildOnly'.
     * @deprecated This method will be removed. Please use {@link #context(InteractionContextType...)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.9.0")
    public CommandBuilder setGuildOnly(boolean value) {
        throw new IllegalArgumentException("Use CommandBuilder#context(InteractionContextType...) instead.");
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
            public boolean isBaseless() {
                return CommandBuilder.this.baseless;
            }
        };

        command.setContext(this.context);

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
