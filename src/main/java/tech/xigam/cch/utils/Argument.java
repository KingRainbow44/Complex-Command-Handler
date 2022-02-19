package tech.xigam.cch.utils;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Map;

public final class Argument 
{
    private Argument(
            String label, String description,
            String reference, OptionType type, boolean required,
            int positionInArguments
    ) {
        this.label = label;
        this.description = description;
        this.reference = reference;
        this.argumentType = type;
        this.required = required;
        this.position = positionInArguments;
    }
    
    public Argument range(int min, int max) {
        if(argumentType != OptionType.INTEGER)
            return this;
        this.min = min; this.max = max;
        return this;
    }
    
    public static Argument create(
            String label, String description,
            String reference, OptionType type, boolean required,
            int positionInArguments
    ) {
        return new Argument(label, description, reference, type, required, positionInArguments);
    }

    public static Argument createWithChoices(
            String label, String description,
            String reference, OptionType type, boolean required,
            int positionInArguments, String... choices
    ) {
        Argument argument = create(label, description, reference, type, required, positionInArguments);
        argument.choices = choices;
        return argument;
    }

    public static Argument createTrailingArgument(
            String label, String description,
            String reference, OptionType type, boolean required,
            int positionInArguments
    ) {
        Argument argument = create(label, description, reference, type, required, positionInArguments);
        argument.trailing = true;
        return argument;
    }

    public static Map<String, OptionType> toOptions(Argument... arguments) {
        Map<String, OptionType> options = new java.util.HashMap<>();
        for (Argument argument : arguments) {
            options.put(argument.reference, argument.argumentType);
        }
        return options;
    }

    public static Command.Choice[] toChoices(Argument argument) {
        Command.Choice[] commandChoices = new Command.Choice[argument.choices.length];
        for (int i = 0; i < argument.choices.length; i++) {
            commandChoices[i] = new Command.Choice(argument.choices[i], argument.choices[i]);
        } return commandChoices;
    }
    
    public String label = null;
    public String description = null;

    public String reference = null;
    public OptionType argumentType = null;
    public boolean required = false;
    
    public int position = -1;
    
    /*
     * Extra arguments.
     */
    
    public String[] choices = null;
    public int min = -1, max = -1;
    public boolean trailing = false;
}
