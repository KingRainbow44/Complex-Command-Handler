package tech.xigam.cch;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.xigam.cch.command.*;
import tech.xigam.cch.command.modifiers.Arguments;
import tech.xigam.cch.command.modifiers.Baseless;
import tech.xigam.cch.command.modifiers.Limited;
import tech.xigam.cch.command.modifiers.Restricted;
import tech.xigam.cch.utils.Argument;
import tech.xigam.cch.utils.Interaction;
import tech.xigam.cch.utils.InteractiveArguments;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.dv8tion.jda.api.interactions.commands.Command.Type.*;

/**
 * The main command handler.
 * Should be initialized on build.
 */
public final class ComplexCommandHandler extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger("CCH");

    private JDA jdaInstance;
    private final boolean usePrefix;

    private final Map<String, BaseCommand> commands = new HashMap<>();
    private final Map<String, InteractiveArguments> argumentSessions = new HashMap<>();

    private String prefix;

    public Consumer<Interaction> onArgumentError = interaction -> {};
    public BiConsumer<Interaction, Exception> onContextError = (interaction, exception) -> {};

    public boolean mentionDefault = true;

    public ComplexCommandHandler(boolean usePrefix) {
        this.usePrefix = usePrefix;
    }

    public ComplexCommandHandler setPrefix(String prefix) {
        this.prefix = prefix; return this;
    }

    public ComplexCommandHandler registerCommand(BaseCommand command) {
        commands.put(command.getLabel(), command);
        return this;
    }

    public void setJda(JDA jda) {
        jda.addEventListener(this);
        this.jdaInstance = jda;
    }

    public Map<String, BaseCommand> getRegisteredCommands() {
        return new HashMap<>(this.commands);
    }

    /*
     * Executing commands.
     */

    private void runCommand(String command, MessageReceivedEvent event) {
        executeCommand(command, event.getMessage(), event.getMember(), event.getChannel());
    }

    private void runCommand(String command, MessageUpdateEvent event) {
        executeCommand(command, event.getMessage(), event.getMember(), event.getChannel());
    }

    private void runCommand(SlashCommandInteractionEvent event) {
        if (!commands.containsKey(event.getName())) {
            event.reply("Command not found.")
                    .queue();
            return;
        }

        commands.get(event.getName())
                .prepareForExecution(event, this);
    }

    private void runCommand(UserContextInteractionEvent event) {
        if (!commands.containsKey(event.getName())) {
            event.reply("Command not found.")
                    .queue();
            return;
        }

        commands.get(event.getName())
                .prepareForExecution(event, this);
    }

    private void runCommand(MessageContextInteractionEvent event) {
        if (!commands.containsKey(event.getName())) {
            event.reply("Command not found.")
                    .queue();
            return;
        }

        commands.get(event.getName())
                .prepareForExecution(event, this);
    }

    private void executeCommand(String command, Message message2, Member member, MessageChannel textChannel) {
        if (!commands.containsKey(command))
            return;

        String message = message2.getContentRaw();
        String[] splitMessage = message.split(" ");

        List<String> arguments = new ArrayList<>();
        for (String argument : splitMessage)
            if (!argument.startsWith(this.prefix))
                arguments.add(argument);
        commands.get(command).prepareForExecution(
                arguments, message2,
                member, textChannel,
                false, this
        );
    }

    /*
     * Handling interactive arguments.
     */

    public void checkMessageInteraction(Message message) {
        if (message.getMember() == null) return;

        String memberId = message.getMember().getId();
        String channelId = message.getChannel().getId();
        if (!argumentSessions.containsKey(memberId)) return;
        if (!channelId.matches(
                argumentSessions.get(memberId).getChannel().getId()
        )) return;

        argumentSessions.get(memberId)
                .advance(message);
    }

    public void destroyInteraction(InteractiveArguments session) {
        argumentSessions.remove(
                session.getMember().getId()
        );
    }

    /*
     * Event handling.
     */

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!this.usePrefix) return;

        if (event.getAuthor().isBot())
            return;

        if (!event.getMessage().getContentRaw().startsWith(prefix)) {
            this.checkMessageInteraction(event.getMessage()); return;
        }

        String message = event.getMessage().getContentRaw();
        if (message.split(this.prefix).length < 2)
            return;
        this.runCommand(
                message.split(this.prefix)[1].split(" ")[0], event
        );
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!usePrefix) return;

        if (event.getAuthor().isBot())
            return;

        String message = event.getMessage().getContentRaw();
        if (!message.startsWith(prefix))
            return;

        if (message.split(this.prefix).length < 2)
            return;
        this.runCommand(
                message.split(this.prefix)[1].split(" ")[0], event
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        this.runCommand(event);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!commands.containsKey(event.getName()))
            return;

        var command = commands.get(event.getName());
        command.prepareForCompletion(event, this);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String rawReference = event.getComponentId();
        if (!rawReference.startsWith("<"))
            return;

        var label = rawReference.split("<")[1].split(">")[0];
        var command = commands.get(label);
        command.prepareForCallback(label, event, this);
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String rawReference = event.getComponentId();
        if (!rawReference.startsWith("<"))
            return;

        var label = rawReference.split("<")[1].split(">")[0];
        var command = commands.get(label);
        command.prepareForCallback(label, event, this);
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        this.runCommand(event);
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        this.runCommand(event);
    }

    /**
     * Delete and create commands.
     */

    public void downsert(@Nullable Guild guild) {
        if (guild == null) {
            jdaInstance.updateCommands()
                    .addCommands().queue();
        } else {
            guild.updateCommands()
                    .addCommands().queue();
        }
    }

    public void deployAll(@Nullable Guild guild) {
        try {
            Collection<CommandData> commands = new ArrayList<>();
            this.commands.forEach((label, command) -> {
                var action = (SlashCommandData) (command.commandType() == SLASH ?
                        Commands.slash(command.getLabel(), command.getDescription()) :
                        Commands.context(command.commandType(), command.getLabel()));

                for (SubCommand subCommand : ((Command) command).getSubCommands().values()) {
                    if (command instanceof Baseless baseless && baseless.isBaseless()) {
                        SubcommandData cmdData = new SubcommandData(subCommand.getLabel(), subCommand.getDescription());
                        if (subCommand instanceof Arguments argsCmd) {
                            for (Argument argument : argsCmd.getArguments()) {
                                OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                                if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                    argumentData.addChoices(Argument.toChoices(argument));
                                else if (argument.argumentType == OptionType.INTEGER && argument.min != -1 && argument.max != -1)
                                    argumentData.setRequiredRange(argument.min, argument.max);
                                if (argument.completable)
                                    argumentData.setAutoComplete(true);
                                cmdData = cmdData.addOptions(argumentData);
                            }
                        }

                        if (command.commandType() == SLASH) {
                            action = action.addSubcommands(cmdData);
                        }
                    } else {
                        OptionData options = new OptionData(OptionType.STRING, "action", "Execute another sub-command/action of this command.", false);
                        options = options.addChoice(subCommand.getLabel(), subCommand.getLabel());

                        if (subCommand instanceof Arguments argsCmd) {
                            for (Argument argument : argsCmd.getArguments()) {
                                OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                                if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                    argumentData.addChoices(Argument.toChoices(argument));
                                else if (argument.argumentType == OptionType.INTEGER && argument.min != -1 && argument.max != -1)
                                    argumentData.setRequiredRange(argument.min, argument.max);
                                if (argument.completable)
                                    argumentData.setAutoComplete(true);

                                if (command.commandType() == SLASH) {
                                    action = action.addOptions(argumentData);
                                }
                            }
                        }

                        if (command.commandType() == SLASH) {
                            action = action.addOptions(options);
                        }
                    }

                    for (Alias alias : subCommand.getAliases()) {
                        CommandCreateAction subAction;
                        if (guild == null) {
                            subAction = jdaInstance.upsertCommand(alias.getLabel(), alias.getDescription());
                        } else subAction = guild.upsertCommand(alias.getLabel(), alias.getDescription());

                        if (subCommand instanceof Arguments argsCmd) {
                            for (Argument argument : argsCmd.getArguments()) {
                                OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                                if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                    argumentData.addChoices(Argument.toChoices(argument));
                                else if (argument.argumentType == OptionType.INTEGER && argument.min != -1 && argument.max != -1)
                                    argumentData.setRequiredRange(argument.min, argument.max);
                                if (argument.completable)
                                    argumentData.setAutoComplete(true);
                                subAction = subAction.addOptions(argumentData);
                            }
                        }
                    }
                }

                if (command instanceof Arguments argsCmd) {
                    for (Argument argument : argsCmd.getArguments()) {
                        OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                        if (argument.choices != null && argument.argumentType == OptionType.STRING)
                            argumentData.addChoices(Argument.toChoices(argument));
                        else if (argument.argumentType == OptionType.INTEGER && argument.min != -1 && argument.max != -1)
                            argumentData.setRequiredRange(argument.min, argument.max);
                        if (argument.completable)
                            argumentData.setAutoComplete(true);

                        if (command.commandType() == SLASH) {
                            action = action.addOptions(argumentData);
                        }
                    }
                }

                if (command instanceof Restricted restricted) {
                    action = action.setDefaultPermissions(restricted.toMemberPermissions());
                }

                if (command instanceof Limited limited) {
                    action = action
                            .setGuildOnly(limited.isGuildOnly())
                            .setNSFW(limited.isNsfw());
                }

                commands.add(action);
            });

            if (guild == null)
                jdaInstance.updateCommands()
                        .addCommands(commands).queue();
            else
                guild.updateCommands()
                        .addCommands(commands).queue();
        } catch (Exception exception) {
            this.logger.warn("Unable to deploy slash-commands.", exception);
        }
    }

    /**
     * Downsert, then upsert commands.
     */
    @Deprecated public void deploy(@Nullable Guild guild) {
        if (guild == null) {
            jdaInstance.updateCommands()
                    .addCommands().queue();
        } else {
            guild.updateCommands()
                    .addCommands().queue();
        }

        commands.forEach((label, command) -> {
            CommandCreateAction action;
            if (guild == null) {
                action = jdaInstance.upsertCommand(label, command.getDescription());
            } else action = guild.upsertCommand(label, command.getDescription());

            for(SubCommand subCommand : ((Command) command).getSubCommands().values()) {
                if (command instanceof Baseless) {
                    SubcommandData cmdData = new SubcommandData(subCommand.getLabel(), subCommand.getDescription());
                    if (subCommand instanceof Arguments) {
                        for(Argument argument : ((Arguments) subCommand).getArguments()) {
                            OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                            if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                argumentData.addChoices(Argument.toChoices(argument));
                            if (argument.completable)
                                argumentData.setAutoComplete(true);
                            cmdData = cmdData.addOptions(argumentData);
                        }
                    }
                    action = action.addSubcommands(cmdData);
                } else {
                    OptionData options = new OptionData(OptionType.STRING, "action", "Execute another sub-command/action of this command.", false);
                    options = options.addChoice(subCommand.getLabel(), subCommand.getLabel());

                    if (subCommand instanceof Arguments) {
                        for(Argument argument : ((Arguments) subCommand).getArguments()) {
                            OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                            if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                argumentData.addChoices(Argument.toChoices(argument));
                            if (argument.completable)
                                argumentData.setAutoComplete(true);
                            action = action.addOptions(argumentData);
                        }
                    }

                    action = action.addOptions(options);
                }

                for(Alias alias : subCommand.getAliases()) {
                    CommandCreateAction subAction;
                    if (guild == null) {
                        subAction = jdaInstance.upsertCommand(alias.getLabel(), alias.getDescription());
                    } else subAction = guild.upsertCommand(alias.getLabel(), alias.getDescription());

                    if (subCommand instanceof Arguments) {
                        for(Argument argument : ((Arguments) subCommand).getArguments()) {
                            OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                            if (argument.choices != null && argument.argumentType == OptionType.STRING)
                                argumentData.addChoices(Argument.toChoices(argument));
                            if (argument.completable)
                                argumentData.setAutoComplete(true);
                            subAction = subAction.addOptions(argumentData);
                        }
                    }
                }
            }

            if (command instanceof Arguments) {
                for(Argument argument : ((Arguments) command).getArguments()) {
                    OptionData argumentData = new OptionData(argument.argumentType, argument.label, argument.description, argument.required);
                    if (argument.choices != null && argument.argumentType == OptionType.STRING)
                        argumentData.addChoices(Argument.toChoices(argument));
                    if (argument.completable)
                        argumentData.setAutoComplete(true);
                    action = action.addOptions(argumentData);
                }
            }

            action.queue();
        });
    }
}
