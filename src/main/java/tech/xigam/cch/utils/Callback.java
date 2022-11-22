package tech.xigam.cch.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A callback for buttons and forms.
 */
public final class Callback {
    private final String reference;
    @Nullable
    private final Member member;

    private final GenericComponentInteractionCreateEvent interactionExecutor;

    private Type deferred = Type.NONE;
    private List<String> selected = new ArrayList<>();

    public Callback(ButtonInteractionEvent event) {
        this.interactionExecutor = event;
        this.member = event.getMember();

        var rawReference = event.getComponentId();
        this.reference = rawReference.split(">")[1];
    }

    public Callback(StringSelectInteractionEvent event) {
        this.interactionExecutor = event;
        this.member = event.getMember();

        var rawReference = event.getComponentId();
        this.reference = rawReference.split(">")[1];

        this.selected = event.getSelectedOptions().stream().map(SelectOption::getValue).toList();
    }

    public String getReference() {
        return this.reference;
    }

    /**
     * Exists **only** for menus.
     *
     * @return A list of options the user has selected.
     */
    public List<String> getSelectedOptions() {
        return this.selected;
    }

    @Nullable
    public Member getMember() {
        return this.member;
    }

    // ---------- UTILITY METHODS ---------- \\

    public Callback deferEdit() {
        this.interactionExecutor.deferEdit().queue();
        this.deferred = Type.EDIT;
        return this;
    }

    public Callback deferReply() {
        this.interactionExecutor.deferReply().queue();
        this.deferred = Type.REPLY;
        return this;
    }

    // ---------- REPLY METHODS ---------- \\

    public void edit(String message) {
        this.send(message, Type.EDIT);
    }

    public void reply(String message) {
        this.send(message, Type.REPLY);
    }

    public void edit(MessageEmbed embed) {
        this.send(embed, Type.EDIT);
    }

    public void reply(MessageEmbed embed) {
        this.send(embed, Type.REPLY);
    }

    public Callback edit(Button... buttons) {
        this.edit(ActionRow.of(buttons));
        return this;
    }

    public Callback edit(SelectMenu menu) {
        this.edit(ActionRow.of(menu));
        return this;
    }

    public Callback edit(ActionRow row) {
        this.send(row, Type.EDIT);
        return this;
    }

    /**
     * Internal message sending/updating method.
     *
     * @param message The message to send.
     */
    private void send(Object message, Type type) {
        switch (this.deferred) {
            case EDIT -> {
                var hook = this.interactionExecutor.getHook();
                if (message instanceof String msg) {
                    hook.editOriginal(msg).queue();
                } else if (message instanceof MessageEmbed embed) {
                    hook.editOriginalEmbeds(embed).queue();
                } else if (message instanceof ActionRow row) {
                    hook.editOriginalComponents(row).queue();
                }
            }

            case REPLY -> {
                var hook = this.interactionExecutor.getHook();
                if (message instanceof String msg) {
                    hook.sendMessage(msg).queue();
                } else if (message instanceof MessageEmbed embed) {
                    hook.sendMessageEmbeds(embed).queue();
                }
            }

            case NONE -> {
                if (type == Type.EDIT) {
                    if (message instanceof String msg) {
                        this.interactionExecutor.editMessage(msg).queue();
                    } else if (message instanceof MessageEmbed embed) {
                        this.interactionExecutor.editMessageEmbeds(embed).queue();
                    }
                } else {
                    if (message instanceof String msg) {
                        this.interactionExecutor.reply(msg).queue();
                    } else if (message instanceof MessageEmbed embed) {
                        this.interactionExecutor.replyEmbeds(embed).queue();
                    }
                }
            }
        }
    }

    private enum Type {
        NONE, EDIT, REPLY
    }
}
