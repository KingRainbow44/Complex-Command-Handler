package tech.xigam.cch.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import tech.xigam.cch.ComplexCommandHandler;
import tech.xigam.cch.utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command implements BaseCommand
{
    private final String label, description;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    
    private List<String> interactiveArguments = new ArrayList<>();
    
    public Command(String label, String description) {
        this.label = label;
        this.description = description;
    }
    
    public Command(String label, String description, String... argumentQuestions) {
        this.label = label;
        this.description = description;
        this.interactiveArguments = List.of(argumentQuestions);
    }

    @Override
    public String getLabel() {
        return this.label;
    }
    
    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void prepareForExecution(List<String> arguments, Message message, Member sender, TextChannel channel, boolean skipArguments, ComplexCommandHandler handler) {
        List<String> args = new ArrayList<>(arguments); boolean executeBase = true;
        
        for(String argument : arguments) {
            if(!subCommands.containsKey(argument))
                continue;
            
            executeBase = false; args.remove(argument);
            getSubCommand(argument)
                    .prepareForExecution(args, message, sender, channel, false, handler);
        }

        if(executeBase) {
            if(this instanceof Arguments) {
                int requiredArguments = 0;
                for(Argument argument : ((Arguments) this).getArguments()) {
                    if(argument.required) requiredArguments++;
                }
                
                if(args.size() < requiredArguments) {
                    handler.onArgumentError.accept(
                            new Interaction(handler, message, channel, arguments, this)
                    ); return;
                }
            }
            
            if (interactiveArguments.size() == 0 || skipArguments) {
                execute(new Interaction(handler, message, channel, arguments, this));
            } else {
                new InteractiveArguments(
                        message, sender, this, interactiveArguments, handler
                );
            }
        }
    }

    @Override
    public void prepareForExecution(SlashCommandInteractionEvent event, ComplexCommandHandler handler) {
        String subCommand = null;
        if (this instanceof Baseless) {
            subCommand = event.getSubcommandName();
        } else {
            OptionMapping option = event.getOption("action");
            if (option != null) {
                subCommand = option.getAsString();
            }
        }

        if (subCommand != null) {
            if (subCommands.containsKey(subCommand)) {
                getSubCommand(subCommand)
                        .execute(new Interaction(handler, event, getSubCommand(subCommand)));
                return;
            }
        }

        execute(new Interaction(handler, event, this));
    }

    @Override
    public void prepareForCompletion(CommandAutoCompleteInteractionEvent event, ComplexCommandHandler handler) {
        String subCommand = null;
        if (this instanceof Baseless) {
            subCommand = event.getSubcommandName();
        } else {
            OptionMapping option = event.getOption("action");
            if (option != null) {
                subCommand = option.getAsString();
            }
        }

        if (subCommand != null) {
            if (subCommands.containsKey(subCommand)) {
                var subCmd = getSubCommand(subCommand);
                if (subCmd instanceof Completable)
                    ((Completable) subCmd).complete(new Completion(event));
                return;
            }
        }

        if (this instanceof Completable)
            ((Completable) this).complete(new Completion(event));
    }

    @Override
    public void prepareForCallback(String cmdLabel, ButtonInteractionEvent event, ComplexCommandHandler handler) {
        if (subCommands.containsKey(cmdLabel)) {
            var subCmd = this.getSubCommand(cmdLabel);
            if (subCmd instanceof Callable)
                ((Callable) subCmd).callback(new Callback(event));
            return;
        }

        if (this instanceof Callable)
            ((Callable) this).callback(new Callback(event));
    }

    @Override
    public void prepareForCallback(String cmdLabel, SelectMenuInteractionEvent event, ComplexCommandHandler handler) {
        if (subCommands.containsKey(cmdLabel)) {
            var subCmd = this.getSubCommand(cmdLabel);
            if (subCmd instanceof Callable)
                ((Callable) subCmd).callback(new Callback(event));
            return;
        }

        if (this instanceof Callable)
            ((Callable) this).callback(new Callback(event));
    }

    public final Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }

    protected final void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getLabel(), subCommand);
    }

    private SubCommand getSubCommand(String label) {
        return subCommands.get(label);
    }
}
