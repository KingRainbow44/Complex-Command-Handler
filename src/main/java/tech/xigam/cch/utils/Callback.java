package tech.xigam.cch.utils;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A callback for buttons and forms.
 */
public final class Callback {
    @Getter
    private final String reference;

    /**
     * This is the user who pressed the button.
     * This will always be non-null.
     */
    @Getter
    @NotNull
    private final User user;

    /**
     * This is the guild member who pressed the button.
     * It will only be non-null if the button was pressed in a guild.
     */
    @Nullable
    private final Member member;

    /**
     * This is the context in which the user pressed the button.
     */
    @Getter private final InteractionContextType context;

    private GenericComponentInteractionCreateEvent interactionExecutor;
    private ModalInteractionEvent modalExecutor;

    private boolean ephemeral = false;
    private Type deferred = Type.NONE;
    private List<String> selected = new ArrayList<>();

    private final List<ActionRow> rows = new ArrayList<>();

    public Callback(ButtonInteractionEvent event) {
        this.interactionExecutor = event;
        this.member = event.getMember();
        this.user = event.getUser();

        var rawReference = event.getComponentId();
        this.reference = rawReference.split(">")[1];

        this.context = event.getContext();
    }

    public Callback(StringSelectInteractionEvent event) {
        this.interactionExecutor = event;
        this.member = event.getMember();
        this.user = event.getUser();

        var rawReference = event.getComponentId();
        this.reference = rawReference.split(">")[1];

        this.context = event.getContext();
        this.selected = event.getSelectedOptions().stream().map(SelectOption::getValue).toList();
    }

    public Callback(ModalInteractionEvent event) {
        this.modalExecutor = event;
        this.member = event.getMember();
        this.user = event.getUser();

        var rawReference = event.getModalId();
        this.reference = rawReference.split(">")[1];

        this.context = event.getContext();
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

    public Callback setEphemeral() {
        this.ephemeral = true;
        return this;
    }

    public Callback deferEdit() {
        this.interactionExecutor.deferEdit().queue();
        this.deferred = Type.EDIT;
        return this;
    }

    public Callback deferReply() {
        this.interactionExecutor
                .deferReply()
                .setEphemeral(this.ephemeral)
                .queue();
        this.deferred = Type.REPLY;
        return this;
    }

    /**
     * Adds an action row with the components.
     *
     * @param components The components to add to the action row.
     * @return The callback instance for chaining.
     */
    public Callback with(ActionComponent... components) {
        this.rows.add(ActionRow.of(components));
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

    /**
     * Replies with a modal.
     *
     * @param modal The modal to reply with.
     */
    public void reply(Modal modal) {
        this.interactionExecutor.replyModal(modal).queue();
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

                WebhookMessageEditAction<Message> action;
                if (message instanceof String msg) {
                    action = hook.editOriginal(msg);
                } else if (message instanceof MessageEmbed embed) {
                    action = hook.editOriginalEmbeds(embed);
                } else {
                    throw new RuntimeException("Cannot edit non-message content");
                }

                if (!this.rows.isEmpty()) {
                    action.setComponents(this.rows);
                    this.rows.clear();
                }
                action.queue();
            }

            case REPLY -> {
                var hook = this.interactionExecutor.getHook();

                WebhookMessageCreateAction<?> action;
                if (message instanceof String msg) {
                    action = hook.sendMessage(msg);
                } else if (message instanceof MessageEmbed embed) {
                    action = hook.sendMessageEmbeds(embed);
                } else {
                    throw new RuntimeException("Cannot reply without message content");
                }

                if (!this.rows.isEmpty()) {
                    action.setComponents(this.rows);
                    this.rows.clear();
                }
                action.queue();
            }

            case NONE -> {
                if (type == Type.EDIT) {
                    MessageEditCallbackAction edit;
                    if (message instanceof String msg) {
                        edit = this.interactionExecutor.editMessage(msg);
                    } else if (message instanceof MessageEmbed embed) {
                        edit = this.interactionExecutor.editMessageEmbeds(embed);
                    } else {
                        throw new RuntimeException("Cannot edit non-message content");
                    }

                    if (!this.rows.isEmpty()) {
                        edit.setComponents(this.rows);
                        this.rows.clear();
                    }
                    edit.queue();

                    this.deferred = Type.EDIT;
                } else {
                    ReplyCallbackAction reply;
                    if (message instanceof String msg) {
                        reply = this.interactionExecutor.reply(msg);
                    } else if (message instanceof MessageEmbed embed) {
                        reply = this.interactionExecutor.replyEmbeds(embed);
                    } else {
                        throw new RuntimeException("Cannot reply without message content");
                    }

                    if (!this.rows.isEmpty()) {
                        reply.setComponents(this.rows);
                        this.rows.clear();
                    }
                    reply
                            .setEphemeral(this.ephemeral)
                            .queue();

                    this.deferred = Type.REPLY;
                }
            }
        }
    }

    private enum Type {
        NONE, EDIT, REPLY
    }
}
