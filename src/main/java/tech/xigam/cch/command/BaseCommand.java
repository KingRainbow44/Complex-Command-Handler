package tech.xigam.cch.command;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.utils.Interaction;

import java.util.List;

import static tech.xigam.cch.utils.Validation.isUrl;

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

    void prepareForCallback(String cmdLabel, ButtonInteractionEvent event, ComplexCommandHandler handler);

    /**
     * Creates a button with proper handling for this command.
     *
     * @param style     The button style.
     * @param reference The reference to the button (or a URL).
     * @param text      The text to display on the button.
     * @return The button.
     */
    default Button createButton(ButtonStyle style, String reference, String text) {
        return Button.of(style, isUrl(reference) ? reference : "<" + this.getLabel().toLowerCase() + ">" + reference, text);
    }

    /**
     * Creates a button with proper handling for this command.
     *
     * @param style     The button style.
     * @param reference The reference to the button (or a URL).
     * @param emoji     The emoji to show on the button.
     * @return The button.
     */
    default Button createButton(ButtonStyle style, String reference, Emoji emoji) {
        return Button.of(style, isUrl(reference) ? reference : "<" + this.getLabel().toLowerCase() + ">" + reference, emoji);
    }

    /**
     * Creates a button with proper handling for this command.
     *
     * @param style     The button style.
     * @param reference The reference to the button (or a URL).
     * @param text      The text to show to the right of the emoji.
     * @param emoji     The emoji to show on the button.
     * @return The button.
     */
    default Button createButton(ButtonStyle style, String reference, String text, Emoji emoji) {
        return Button.of(style, isUrl(reference) ? reference : "<" + this.getLabel().toLowerCase() + ">" + reference, text, emoji);
    }
}
