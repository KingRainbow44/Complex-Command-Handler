package tech.xigam.cch.utils;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

/**
 * A callback for buttons and forms.
 */
public final class Callback {
    private final boolean isSlash, inGuild;
    private final String reference;

    private GenericComponentInteractionCreateEvent interactionExecutor = null;

    private boolean deferred = false;

    public Callback(ButtonInteractionEvent event) {
        this.isSlash = true;
        this.inGuild = event.isFromGuild();
        this.interactionExecutor = event;

        var rawReference = event.getComponentId();
        this.reference = rawReference.split(">")[1];
    }

    public String getReference() {
        return this.reference;
    }

    // ---------- UTILITY METHODS ---------- \\

    public Callback deferEdit() {
        if (this.isSlash)
            this.interactionExecutor.deferEdit().queue();
        this.deferred = true;
        return this;
    }

    // ---------- REPLY METHODS ---------- \\

    public void reply(String message) {
        
    }
}
