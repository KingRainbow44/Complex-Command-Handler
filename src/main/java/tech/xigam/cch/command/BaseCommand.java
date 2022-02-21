package tech.xigam.cch.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.utils.Interaction;

import java.util.List;

/**
 * An interface used by {@link Command}, {@link SubCommand}, and {@link Alias}
 */
public interface BaseCommand {
    /*
     * General command information.
     */
    String getLabel();

    String getDescription();

    /*
     * Back-end code.
     */
    void execute(Interaction interaction);

    void prepareForExecution(List<String> arguments, Message message, Member sender, TextChannel channel, boolean skipArguments, ComplexCommandHandler handler);

    void prepareForExecution(SlashCommandInteractionEvent event, ComplexCommandHandler handler);

    void prepareForCompletion(CommandAutoCompleteInteractionEvent event, ComplexCommandHandler handler);
}
