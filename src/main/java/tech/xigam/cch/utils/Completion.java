package tech.xigam.cch.utils;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.ArrayList;
import java.util.List;

public final class Completion {
    private final CommandAutoCompleteInteractionEvent completeEvent;

    private final List<Command.Choice> choices
            = new ArrayList<>();

    public Completion(
            CommandAutoCompleteInteractionEvent event
    ) {
        this.completeEvent = event;
    }

    /*
     * Data methods.
     */

    public String getFocusedOption() {
        return this.completeEvent.getFocusedOption().getName();
    }

    public String getInput() {
        return this.completeEvent.getFocusedOption().getValue();
    }

    /*
     * Reply with choices.
     */

    /**
     * Adds a choice to reply with.
     *
     * @param mapping This is the value that will be SHOWN to the user.
     * @param value   This is the value that will be handled internally by CCH.
     * @return An instance of this completion class.
     */

    public Completion addChoice(String mapping, Object value) {
        net.dv8tion.jda.api.interactions.commands.Command.Choice choice;
        if (value instanceof Long || value instanceof Integer) {
            choice = new net.dv8tion.jda.api.interactions.commands.Command.Choice(mapping, (int) value);
        } else if (value instanceof Double) {
            choice = new net.dv8tion.jda.api.interactions.commands.Command.Choice(mapping, (double) value);
        } else {
            choice = new net.dv8tion.jda.api.interactions.commands.Command.Choice(mapping, value.toString());
        }
        this.choices.add(choice);
        return this;
    }

    public void reply() {
        try {
            this.completeEvent.replyChoices(this.choices).queue();
        } catch (IllegalStateException ignored) {
        }
    }
}
