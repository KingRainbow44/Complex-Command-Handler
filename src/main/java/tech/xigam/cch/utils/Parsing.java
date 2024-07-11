package tech.xigam.cch.utils;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import tech.xigam.cch.command.modifiers.Arguments;

import java.util.ArrayList;
import java.util.Collection;

public interface Parsing {
    /**
     * Prepares {@link OptionData} for a {@link Arguments} command.
     *
     * @param args The arguments to parse.
     * @return The parsed {@link OptionData}.
     */
    static Collection<OptionData> parse(Arguments args) {
        var options = new ArrayList<OptionData>();
        for (var arg : args.getArguments()) {
            var data = new OptionData(arg.argumentType, arg.label, arg.description, arg.required);

            // Handle specific argument types.
            switch (arg.argumentType) {
                case STRING -> {
                    if (arg.choices == null) break;
                    data.addChoices(Argument.toChoices(arg));
                }
                case INTEGER -> {
                    if (arg.min == -1 || arg.max == -1) break;
                    data.setRequiredRange(arg.min, arg.max);
                }
            }

            if (arg.completable) {
                data.setAutoComplete(true);
            }

            options.add(data);
        }

        return options;
    }
}
