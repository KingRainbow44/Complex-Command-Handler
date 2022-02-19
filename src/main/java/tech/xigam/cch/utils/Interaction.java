package tech.xigam.cch.utils;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import tech.xigam.cch.command.Arguments;
import tech.xigam.cch.command.BaseCommand;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Interaction implements Cloneable
{
    /*
     * Global data.
     * Set regardless of position.
     */
    private final boolean isSlash;
    private final BaseCommand command;
    
    private final Member member;
    private final Message message;
    private final MessageChannel channel;
    private final Guild guild;

    private SlashCommandInteractionEvent slashExecutor = null;
    
    /*
     * Information storage.
     */
    private boolean ephemeral = false, sendToDMs = false;
    private boolean deferred = false;
    
    private final Map<String, Object> arguments = new HashMap<>();
    private final List<String> rawArguments = new ArrayList<>();

    public Interaction(SlashCommandInteractionEvent event, BaseCommand command) {
        this.isSlash = true;
        this.slashExecutor = event;
        this.command = command;

        this.member = event.getMember();
        this.message = null;
        this.channel = event.getChannel();
        this.guild = event.getGuild();

        if (command instanceof Arguments) {
            Map<String, OptionType> argumentTypes = Argument.toOptions(
                    ((Arguments) command).getArguments().toArray(new Argument[0])
            );
            
            for(Map.Entry<String, OptionType> entry : argumentTypes.entrySet()) {
                OptionMapping mapping = event.getOption(entry.getKey());
                if(mapping == null) continue;
                
                switch(entry.getValue()) {
                    case STRING -> this.arguments.put(entry.getKey(), mapping.getAsString());
                    case INTEGER, NUMBER -> this.arguments.put(entry.getKey(), mapping.getAsLong());
                    case BOOLEAN -> this.arguments.put(entry.getKey(), mapping.getAsBoolean());
                    case MENTIONABLE -> this.arguments.put(entry.getKey(), mapping.getAsMentionable());
                    case USER -> this.arguments.put(entry.getKey(), mapping.getAsUser());
                    case ROLE -> this.arguments.put(entry.getKey(), mapping.getAsRole());
                    case CHANNEL -> this.arguments.put(entry.getKey(), mapping.getAsMessageChannel());
                }
            }
        }
    }
    
    public Interaction(
            Message message, TextChannel channel, 
            List<String> arguments, BaseCommand command
    ) {
        this.isSlash = false; this.command = command;
        
        this.member = message.getMember();
        this.message = message;
        this.channel = channel;
        this.guild = channel.getGuild();
        
        if(command instanceof Arguments) {
            Argument[] args = ((Arguments) command).getArguments().toArray(new Argument[0]);
            
            try {
                for (int i = 0; i < arguments.size(); i++) {
                    Argument argument = args[i];
                    if (argument.trailing) {
                        String combined = String.join(" ", arguments.subList(i, arguments.size()));
                        this.arguments.put(argument.reference, combined);
                        this.rawArguments.add(combined);
                        break;
                    }

                    switch (argument.argumentType) {
                        default -> this.arguments.put(argument.reference, arguments.get(argument.position));

                        case INTEGER -> this.arguments.put(argument.reference, Long.parseLong(arguments.get(argument.position)));
                        case BOOLEAN -> this.arguments.put(argument.reference, Boolean.parseBoolean(arguments.get(argument.position)));
                    }
                }
            } catch (IndexOutOfBoundsException ignored) { }
        } this.rawArguments.addAll(arguments);
    }
    
    /*
     * Data methods.
     */
    
    public Map<String, Object> getArguments() {
        return this.arguments; // Returns a list of STRING-MAPPED arguments.
    }
    
    public <T> T getArgument(String reference, Class<T> type) {
        return type.cast(this.arguments.get(reference));
    }

    public <T> T getArgument(String reference, T fallback, Class<T> type) {
        return type.cast(this.arguments.getOrDefault(reference, fallback));
    }
    
    public List<String> getRawArguments() {
        return this.rawArguments; // Returns a list of RAW UN-ORDERED ARGUMENTS.
    }
    
    public Member getMember() {
        return this.member;
    }
    
    public MessageChannel getChannel() {
        return this.channel;
    }
    
    public Guild getGuild() {
        return this.guild;
    }
    
    @Nullable
    public Message getMessage() {
        return this.message;
    }

    @Nullable
    public SlashCommandInteractionEvent getSlashExecutor() {
        return this.slashExecutor;
    }
    
    public boolean isSlash() {
        return this.isSlash;
    }
    
    public boolean isDeferred() {
        return this.deferred;
    }
    
    public boolean isEphemeral() {
        return this.ephemeral;
    }
    
    /*
     * Utility methods.
     * Used to make the replying process easier.
     */
    
    public void deferReply() {
        if(this.isSlash)
            this.slashExecutor.deferReply(ephemeral).queue();
        else this.getChannel().sendTyping().queue();
        
        this.deferred = true;
    }
    
    public Interaction setEphemeral() {
        this.ephemeral = true; return this;
    }
    
    public Interaction setEphemeral(boolean sendToDMs) {
        this.ephemeral = true; this.sendToDMs = sendToDMs; return this;
    }
    
    // ---------- REPLY METHODS ---------- \\
    
    public Interaction sendMessage(String message) {
        getChannel().sendMessage(message).queue(); return this;
    }

    public Interaction sendMessage(MessageEmbed message) {
        getChannel().sendMessageEmbeds(message).queue(); return this;
    }
    
    public void reply(String message) {
        reply(message, true);
    }
    
    public void reply(MessageEmbed embed) {
        reply(embed, true);
    }
    
    public void reply(String message, boolean mentionUser) {
        if(isSlash()) {
            if(isDeferred()) {
                slashExecutor.getHook()
                        .sendMessage(message).queue();
            } else {
                slashExecutor.reply(message)
                        .setEphemeral(isEphemeral()).queue();
            }
        } else {
            if(isEphemeral() && sendToDMs) {
                getMember().getUser().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage(message).queue();
                });
            } else {
                getMessage().reply(message)
                        .mentionRepliedUser(mentionUser).queue();
            }
        }
    }

    public void reply(MessageEmbed message, boolean mentionUser) {
        if(isSlash()) {
            if(isDeferred()) {
                slashExecutor.getHook()
                        .sendMessageEmbeds(message).queue();
            } else {
                slashExecutor.replyEmbeds(message)
                        .setEphemeral(isEphemeral()).queue();
            }
        } else {
            if(isEphemeral() && sendToDMs) {
                getMember().getUser().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessageEmbeds(message).queue();
                });
            } else {
                getMessage().replyEmbeds(message)
                        .mentionRepliedUser(mentionUser).queue();
            }
        }
    }

    public void execute(Consumer<Interaction> consumer, long after, TimeUnit timeUnit) {
        try {
            Interaction messageClone = (Interaction) this.clone();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    consumer.accept(messageClone);
                }
            }, timeUnit.toMillis(after));
        } catch (CloneNotSupportedException ignored) { }
    }
}
