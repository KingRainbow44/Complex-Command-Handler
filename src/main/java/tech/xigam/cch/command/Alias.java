package tech.xigam.cch.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.utils.Interaction;

import java.util.List;

public record Alias(String label, Command aliasOf) implements BaseCommand {
    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getDescription() {
        return this.aliasOf.getDescription();
    }

    @Override
    public void execute(Interaction interaction) {
        aliasOf.execute(interaction);
    }

    @Override
    public void prepareForExecution(GenericCommandInteractionEvent event, ComplexCommandHandler handler) {
        aliasOf.prepareForExecution(event, handler);
    }

    @Override
    public void prepareForExecution(List<String> arguments, Message message, Member sender, MessageChannel channel, boolean skipArguments, ComplexCommandHandler handler) {
        aliasOf.prepareForExecution(arguments, message, sender, channel, skipArguments, handler);
    }

    @Override
    public void prepareForCompletion(CommandAutoCompleteInteractionEvent event, ComplexCommandHandler handler) {
        aliasOf.prepareForCompletion(event, handler);
    }

    @Override
    public void prepareForCallback(String cmdLabel, ButtonInteractionEvent event, ComplexCommandHandler handler) {
        aliasOf.prepareForCallback(cmdLabel, event, handler);
    }

    @Override
    public void prepareForCallback(String cmdLabel, StringSelectInteractionEvent event, ComplexCommandHandler handler) {
        aliasOf.prepareForCallback(cmdLabel, event, handler);
    }
}
