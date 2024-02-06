package tech.xigam.cch.utils;

import lombok.Getter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.command.modifiers.Arguments;
import tech.xigam.cch.command.BaseCommand;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Interaction {
    private final boolean isSlash, inGuild;
    @Getter
    private final ComplexCommandHandler commandHandler;

    @Getter
    private final User user;
    @Getter
    @Nullable
    private final Member member;
    @Getter
    @Nullable
    private final Message message;
    @Getter
    private final MessageChannel channel;
    @Getter
    @Nullable
    private final Guild guild;
    @Getter
    private final BaseCommand command;

    private SlashCommandInteractionEvent slashExecutor = null;

    private boolean sendToDMs = false;
    @Getter private boolean ephemeral = false;
    @Getter private boolean deferred = false;

    private final Map<String, Object> arguments = new HashMap<>();
    private final List<String> rawArguments = new ArrayList<>();

    private final List<ActionRow> actionRows = new ArrayList<>();

    public Interaction(ComplexCommandHandler commandHandler, SlashCommandInteractionEvent event, BaseCommand command) {
        this.commandHandler = commandHandler;
        this.isSlash = true;
        this.inGuild = event.isFromGuild();
        this.slashExecutor = event;

        this.user = event.getUser();
        this.member = event.getMember();
        this.message = null;
        this.channel = event.getChannel();
        this.guild = event.getGuild();
        this.command = command;

        if (command instanceof Arguments argsCmd) {
            Map<String, OptionType> argumentTypes = Argument.toOptions(
                    argsCmd.getArguments().toArray(new Argument[0])
            );

            for (var entry : argumentTypes.entrySet()) {
                var mapping = event.getOption(entry.getKey());
                if (mapping == null) continue;

                switch (entry.getValue()) {
                    case STRING -> this.arguments.put(entry.getKey(), mapping.getAsString());
                    case INTEGER, NUMBER -> this.arguments.put(entry.getKey(), mapping.getAsLong());
                    case BOOLEAN -> this.arguments.put(entry.getKey(), mapping.getAsBoolean());
                    case MENTIONABLE -> this.arguments.put(entry.getKey(), mapping.getAsMentionable());
                    case USER -> this.arguments.put(entry.getKey(), mapping.getAsMember());
                    case ROLE -> this.arguments.put(entry.getKey(), mapping.getAsRole());
                    case CHANNEL -> this.arguments.put(entry.getKey(), mapping.getAsChannel());
                    case ATTACHMENT -> this.arguments.put(entry.getKey(), mapping.getAsAttachment());
                }
            }
        }
    }

    public Interaction(
            ComplexCommandHandler commandHandler,
            Message message, MessageChannel channel,
            List<String> arguments, BaseCommand command
    ) {
        this.commandHandler = commandHandler;
        this.isSlash = false;
        this.inGuild = message.isFromGuild();

        this.user = message.getAuthor();
        this.member = message.getMember();
        this.message = message;
        this.channel = channel;
        this.command = command;

        if (channel instanceof GuildChannel guildChannel)
            this.guild = guildChannel.getGuild();
        else this.guild = null;

        if (command instanceof Arguments argsCmd) {
            Argument[] args = argsCmd.getArguments().toArray(new Argument[0]);

            try {
                int attachmentCount = 0;
                for (int i = 0; i < args.length; i++) {
                    Argument argument = args[i];
                    if (argument.trailing) {
                        String combined = String.join(" ", arguments.subList(i, arguments.size()));
                        this.arguments.put(argument.reference, combined);
                        this.rawArguments.add(combined);
                        break;
                    }

                    switch (argument.argumentType) {
                        default -> this.arguments.put(argument.reference, arguments.get(argument.position));

                        case INTEGER, NUMBER -> this.arguments.put(argument.reference, Long.parseLong(arguments.get(argument.position)));
                        case BOOLEAN -> this.arguments.put(argument.reference, Boolean.parseBoolean(arguments.get(argument.position)));
                        case MENTIONABLE, USER -> this.arguments.put(argument.reference, guild.getMemberById(arguments.get(argument.position).replaceAll("[^0-9]", "")));
                        case ROLE -> this.arguments.put(argument.reference, guild.getRoleById(arguments.get(argument.position).replaceAll("[^0-9]", "")));
                        case CHANNEL -> this.arguments.put(argument.reference, guild.getGuildChannelById(arguments.get(argument.position).replaceAll("[^0-9]", "")));
                        case ATTACHMENT -> this.arguments.put(argument.reference, message.getAttachments().get(attachmentCount++));
                    }
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        this.rawArguments.addAll(arguments);
    }

    // ---------- DATA METHODS ---------- \\

    @Deprecated(since = "1.6.0")
    public Map<String, Object> getArguments() {
        return this.arguments; // Returns a list of STRING-MAPPED arguments.
    }

    @Deprecated(since = "1.6.0")
    public List<String> getRawArguments() {
        return this.rawArguments; // Returns a list of RAW UN-ORDERED ARGUMENTS.
    }

    public <T> T getArgument(String reference, Class<T> type) {
        return type.cast(this.arguments.get(reference));
    }

    public <T> T getArgument(String reference, T fallback, Class<T> type) {
        return type.cast(this.arguments.getOrDefault(reference, fallback));
    }

    @Nullable
    public SlashCommandInteractionEvent getSlashExecutor() {
        return this.slashExecutor;
    }

    public boolean isSlash() {
        return this.isSlash;
    }

    public boolean isFromGuild() {
        return this.inGuild;
    }

    // ---------- UTILITY METHODS ---------- \\

    public void deferReply() {
        if (this.isSlash)
            this.slashExecutor.deferReply(ephemeral).queue();
        else this.getChannel().sendTyping().queue();

        this.deferred = true;
    }

    public Interaction setEphemeral() {
        this.ephemeral = true;
        return this;
    }

    public Interaction setEphemeral(boolean sendToDMs) {
        this.ephemeral = true;
        this.sendToDMs = sendToDMs;
        return this;
    }

    // ---------- INTERACTABLE METHODS ---------- \\

    public Interaction addButtons(Button... buttons) {
        this.actionRows.add(ActionRow.of(buttons));
        return this;
    }

    public Interaction addSelectMenu(SelectMenu menu) {
        this.actionRows.add(ActionRow.of(menu));
        return this;
    }

    // ---------- REPLY METHODS ---------- \\


    public Interaction sendMessage(String message) {
        getChannel().sendMessage(message).queue();
        return this;
    }

    public Interaction sendMessage(MessageEmbed message) {
        getChannel().sendMessageEmbeds(message).queue();
        return this;
    }

    public Interaction sendFile(FileUpload... file) {
        getChannel().sendFiles(file).queue();
        return this;
    }

    public void modal(Modal modal) {
        if (!this.isSlash()) throw new IllegalStateException("Cannot send a modal to a non-slash command!");
        this.slashExecutor.replyModal(modal).queue();
    }

    public void reply(String message) {
        this.reply(message, this.commandHandler.mentionDefault);
    }

    public void reply(MessageEmbed embed) {
        this.reply(embed, this.commandHandler.mentionDefault);
    }

    public void reply(FileUpload... files) {
        this.send(Arrays.asList(files), false);
    }

    public void reply(File... files) {
        var uploads = new ArrayList<FileUpload>();
        for (var file : files) {
            uploads.add(FileUpload.fromData(file));
        }
        this.send(uploads, false);
    }

    public void reply(String fileName, byte[] data) {
        this.send(FileUpload.fromData(data, fileName), false);
    }

    public void reply(String message, boolean mentionUser) {
        this.send(message, mentionUser);
    }

    public void reply(MessageEmbed message, boolean mentionUser) {
        this.send(message, mentionUser);
    }

    public void execute(Consumer<Interaction> consumer, long after, TimeUnit timeUnit) {
        Interaction interaction = this;
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                consumer.accept(interaction);
            }
        }, timeUnit.toMillis(after));
    }

    /**
     * Internal message sending method.
     *
     * @param message     The message to send.
     * @param mentionUser Whether to mention the user.
     */
    @SuppressWarnings("unchecked")
    private void send(Object message, boolean mentionUser) {
        if (this.isSlash()) {
            if (this.isDeferred()) {
                WebhookMessageCreateAction<Message> send;

                if (message instanceof String string) {
                    send = this.slashExecutor.getHook().sendMessage(string);
                } else if (message instanceof MessageEmbed embed) {
                    send = this.slashExecutor.getHook().sendMessageEmbeds(embed);
                } else if (message instanceof Collection<?> files) {
                    send = this.slashExecutor.getHook().sendFiles((Collection<FileUpload>) files);
                } else {
                    throw new IllegalArgumentException("Invalid message type: " + message.getClass().getName());
                }

                if (!this.actionRows.isEmpty()) send = send.addComponents(this.actionRows);
                send.queue();
            } else {
                ReplyCallbackAction send;

                if (message instanceof String string) {
                    send = this.slashExecutor.reply(string);
                } else if (message instanceof MessageEmbed embed) {
                    send = this.slashExecutor.replyEmbeds(embed);
                } else if (message instanceof Collection<?> files) {
                    send = this.slashExecutor.replyFiles((Collection<FileUpload>) files);
                } else {
                    throw new IllegalArgumentException("Invalid message type: " + message.getClass().getName());
                }

                if (!this.actionRows.isEmpty()) send = send.addComponents(this.actionRows);
                send.setEphemeral(this.isEphemeral()).queue();
            }
        } else {
            if (this.isEphemeral() && this.sendToDMs) {
                this.getUser().openPrivateChannel().queue(privateChannel -> {
                    MessageCreateAction send;

                    if (message instanceof String string) {
                        send = privateChannel.sendMessage(string);
                    } else if (message instanceof MessageEmbed embed) {
                        send = privateChannel.sendMessageEmbeds(embed);
                    } else if (message instanceof Collection<?> files) {
                        send = privateChannel.sendFiles((Collection<FileUpload>) files);
                    } else {
                        throw new IllegalArgumentException("Invalid message type: " + message.getClass().getName());
                    }

                    if (!this.actionRows.isEmpty()) send = send.addComponents(this.actionRows);
                    send.queue();
                });
            } else {
                MessageCreateAction send;
                var replyingTo = this.getMessage();
                if (replyingTo == null)
                    throw new IllegalArgumentException("Cannot reply to a null message!");

                if (message instanceof String string) {
                    send = replyingTo.reply(string);
                } else if (message instanceof MessageEmbed embed) {
                    send = replyingTo.replyEmbeds(embed);
                } else if (message instanceof Collection<?> files) {
                    send = replyingTo.replyFiles((Collection<FileUpload>) files);
                } else {
                    throw new IllegalArgumentException("Invalid message type: " + message.getClass().getName());
                }

                if (!this.actionRows.isEmpty()) send = send.setComponents(this.actionRows);
                send.mentionRepliedUser(mentionUser).queue();
            }
        }
    }
}
