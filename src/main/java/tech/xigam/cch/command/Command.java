package tech.xigam.cch.command;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.command.modifiers.*;
import tech.xigam.cch.utils.*;

import java.util.*;

public abstract class Command implements BaseCommand
{
    private final String label, description;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    private List<String> interactiveArguments = new ArrayList<>();

    /**
     * Added with CCH 1.9.0
     * This controls where the command can be accessed.
     */
    @Setter @Getter
    private Set<InteractionContextType> context = Set.of(InteractionContextType.GUILD);

    public Command(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public Command(String label, String description, String... argumentQuestions) {
        this.label = label;
        this.description = description;
        this.interactiveArguments = List.of(argumentQuestions);
    }

    /**
     * Overloaded constructor for changing the interaction context.
     * Interactive arguments do not work with interactions.
     * Added in CCH 1.9.0
     *
     * @param label The command label.
     * @param description The command description.
     * @param context The interaction context.
     */
    public Command(String label, String description, InteractionContextType... context) {
        this(label, description);

        this.setContext(context);
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void prepareForExecution(List<String> arguments, Message message, Member sender, MessageChannel channel, boolean skipArguments, ComplexCommandHandler handler) {
        // Check if the command can be executed in a guild.
        if (this instanceof Limited limited &&
                (limited.isGuildOnly() || Validation.isOnly(this.getContext(), InteractionContextType.GUILD)) &&
                !message.isFromGuild()) {
            handler.onContextError.accept(
                    new Interaction(handler, message, channel, arguments, this),
                    new Exception("This command can only be executed in a guild.")
            );
            return;
        }

        // Check if the executor has permission to execute the command.
        if (this instanceof Restricted restricted) {
            if (sender != null && !sender.hasPermission(restricted.getPermissions())) {
                handler.onContextError.accept(
                        new Interaction(handler, message, channel, arguments, this),
                        new Exception("You do not have permission to execute this command.")
                );
                return;
            }
        }

        var args = new ArrayList<>(arguments);
        boolean executeBase = true;

        for (var argument : arguments) {
            if (!subCommands.containsKey(argument))
                continue;

            executeBase = false;
            args.remove(argument);
            this.getSubCommand(argument)
                    .prepareForExecution(args, message, sender, channel, false, handler);
        }

        if (executeBase) {
            if (this instanceof Arguments thisArguments && this.interactiveArguments.isEmpty()) {
                var requiredArguments = 0;
                for (var argument : thisArguments.getArguments()) {
                    if (argument.required) requiredArguments++;
                }

                if (args.size() < requiredArguments) {
                    handler.onArgumentError.accept(
                            new Interaction(handler, message, channel, arguments, this)
                    );

                    return;
                }
            }

            var interaction = new Interaction(handler, message, channel, arguments, this);
            if (interactiveArguments.isEmpty() || skipArguments) try {
                this.execute(interaction);
            } catch (Throwable throwable) {
                handler.onExecutionError.accept(interaction, throwable);
            } else {
                var interactive = new InteractiveArguments(
                        message, sender, this, interactiveArguments, handler
                );
                handler.startInteractive(message, interactive);
            }
        }
    }

    @Override
    public void prepareForExecution(GenericCommandInteractionEvent event, ComplexCommandHandler handler) {
        // Check if the command can be executed in a guild.
        if (this instanceof Limited limited &&
                (limited.isGuildOnly() || Validation.isOnly(this.getContext(), InteractionContextType.GUILD)) &&
                !event.isFromGuild()) {
            handler.onContextError.accept(
                    new Interaction(handler, event, this),
                    new Exception("This command can only be executed in a guild.")
            );
            return;
        }

        // Check if the executor has permission to execute the command.
        if (this instanceof Restricted restricted) {
            var member = event.getMember();
            if (member != null && !member.hasPermission(restricted.getPermissions())) {
                handler.onContextError.accept(
                        new Interaction(handler, event, this),
                        new Exception("You do not have permission to execute this command.")
                );
                return;
            }
        }

        String subCommand = null;
        if (this instanceof Baseless) {
            subCommand = event.getSubcommandName();
        } else {
            var option = event.getOption("action");
            if (option != null) {
                subCommand = option.getAsString();
            }
        }

        // Create the primary interaction.
        var executor = this;
        var interaction = new Interaction(handler, event, this);

        // Check if we have a sub-command.
        if (subCommand != null) {
            if (subCommands.containsKey(subCommand)) {
                // Fetch the command and override the primary interaction.
                executor = getSubCommand(subCommand);
                interaction = new Interaction(handler, event, executor);
            }
        }

        try {
            executor.execute(interaction);
        } catch (Throwable throwable) {
            handler.onExecutionError.accept(
                    new Interaction(handler, event, this),
                    throwable
            );
        }
    }

    @Override
    public void prepareForCompletion(CommandAutoCompleteInteractionEvent event, ComplexCommandHandler handler) {
        String subCommand = null;
        if (this instanceof Baseless) {
            subCommand = event.getSubcommandName();
        } else {
            OptionMapping option = event.getOption("action");
            if (option != null) {
                subCommand = option.getAsString();
            }
        }

        Completable completable = null;
        if (subCommand != null) {
            if (subCommands.containsKey(subCommand)) {
                var subCmd = this.getSubCommand(subCommand);
                if (subCmd instanceof Completable subCompletable) {
                    completable = subCompletable;
                }
            }
        } else if (this instanceof Completable thisCompletable) {
            completable = thisCompletable;
        }

        if (completable == null) return;

        var completion = new Completion(event);
        try {
            completable.complete(completion);
        } catch (Throwable throwable) {
            handler.onCompletionError.accept(completion, throwable);
        }
    }

    @Override
    public void prepareForCallback(String cmdLabel, ButtonInteractionEvent event, ComplexCommandHandler handler) {
        var callback = new Callback(event);
        var callable = this instanceof Callable thisCallable ? thisCallable : null;

        if (subCommands.containsKey(cmdLabel)) {
            var subCmd = this.getSubCommand(cmdLabel);
            if (subCmd instanceof Callable subCallable) {
                callable = subCallable;
            }
        }

        this.doCallback(callable, callback, handler);
    }

    @Override
    public void prepareForCallback(String cmdLabel, StringSelectInteractionEvent event, ComplexCommandHandler handler) {
        var callback = new Callback(event);
        var callable = this instanceof Callable thisCallable ? thisCallable : null;

        if (subCommands.containsKey(cmdLabel)) {
            var subCmd = this.getSubCommand(cmdLabel);
            if (subCmd instanceof Callable subCallable) {
                callable = subCallable;
            }
        }

        this.doCallback(callable, callback, handler);
    }

    @Override
    public void prepareForCallback(String cmdLabel, ModalInteractionEvent event, ComplexCommandHandler handler) {
        var callback = new Callback(event);
        var callable = this instanceof Callable thisCallable ? thisCallable : null;

        if (subCommands.containsKey(cmdLabel)) {
            var subCmd = this.getSubCommand(cmdLabel);
            if (subCmd instanceof Callable subCallable) {
                callable = subCallable;
            }
        }

        this.doCallback(callable, callback, handler);
    }

    /**
     * Performs a safe callback.
     *
     * @param callable The callable.
     * @param callback The callback.
     * @param handler The handler.
     */
    private void doCallback(Callable callable, Callback callback, ComplexCommandHandler handler) {
        try {
            callable.callback(callback);
        } catch (Throwable throwable) {
            handler.onCallbackError.accept(callback, throwable);
        }
    }

    public final Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }

    protected final void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getLabel(), subCommand);
    }

    private SubCommand getSubCommand(String label) {
        return subCommands.get(label);
    }

    /**
     * Sets the context of a command.
     * This is used for singular commands.
     *
     * @param context The context of the command.
     */
    @Tolerate
    public void setContext(InteractionContextType... context) {
        this.context = Set.of(context);
    }
}
