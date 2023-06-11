package tech.xigam.cch.utils;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Map;

@Accessors(fluent = true, chain = true)
public final class Argument
{
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
        }
        return commandChoices;
    }

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

    /*
     * Setter methods.
     */

    public Argument range(int min, int max) {
        if (argumentType != OptionType.INTEGER)
            return this;
        this.min = min;
        this.max = max;
        return this;
    }

    public final String label;
    public final String description;

    public final String reference;
    public final OptionType argumentType;
    public final boolean required;

    public final int position;

    /*
     * Extra arguments.
     */

    public String[] choices = null;
    public int min = -1, max = -1;
    /**
     * Whether this argument is a trailing argument.
     * Trailing arguments are arguments that take up the rest of the command.
     * This applies only to prefix commands.
     */
    @Setter public boolean trailing = false;
    @Setter public boolean completable = false;
}
