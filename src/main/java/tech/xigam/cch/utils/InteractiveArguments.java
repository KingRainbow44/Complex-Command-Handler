package tech.xigam.cch.utils;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.command.BaseCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InteractiveArguments {
    private static final Map<String, Integer> index = new HashMap<>();

    private final List<String> questions;
    @Getter private final Member member;
    private final BaseCommand command;
    @Getter private final TextChannel channel;
    private final Message message;

    private final ComplexCommandHandler handler;
    private final Map<Integer, String> answers = new HashMap<>();

    public InteractiveArguments(
            Message replyTo, Member member, BaseCommand toExecute,
            List<String> questions, ComplexCommandHandler handler
    ) {
        this.message = replyTo;
        this.member = member;
        this.command = toExecute;
        this.questions = questions;
        this.channel = replyTo.getChannel().asTextChannel();
        this.handler = handler;
    }

    public void start(Message replyTo) {
        if (index.containsKey(member.getId())) return;
        index.put(member.getId(), 0);

        replyTo.reply(
                questions.get(index.get(member.getId()))
        ).queue();
    }

    public void advance(Message response) {
        this.answers.put(
                index.get(this.member.getId()),
                response.getContentRaw()
        ); index.put(this.member.getId(), index.get(this.member.getId()) + 1);

        if ((index.get(this.member.getId()) + 1) > this.questions.size()) {
            this.handler.destroyInteraction(this);
            index.remove(this.member.getId());

            this.command.prepareForExecution(
                    new ArrayList<>(this.answers.values()), this.message, this.member,
                    this.channel, false, this.handler
            );
        } else {
            response.reply(
                    this.questions.get(index.get(this.member.getId()))
            ).mentionRepliedUser(false).queue();
        }
    }
}
