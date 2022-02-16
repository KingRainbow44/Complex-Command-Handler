package tech.xigam.cch.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.utils.Interaction;

import java.util.List;

public record Alias(String label, Command aliasOf) implements BaseCommand
{
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
    public void prepareForExecution(SlashCommandInteractionEvent event, ComplexCommandHandler handler) {
        aliasOf.prepareForExecution(event, handler);
    }

    @Override
    public void prepareForExecution(List<String> arguments, Message message, Member sender, TextChannel channel, boolean skipArguments, ComplexCommandHandler handler) {
        aliasOf.prepareForExecution(arguments, message, sender, channel, skipArguments, handler);
    }
}
