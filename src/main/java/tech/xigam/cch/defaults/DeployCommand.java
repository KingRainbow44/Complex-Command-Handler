package tech.xigam.cch.defaults;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import tech.xigam.cch.command.Arguments;
import tech.xigam.cch.command.Command;
import tech.xigam.cch.utils.Argument;
import tech.xigam.cch.utils.Interaction;

import java.util.Collection;
import java.util.List;

public abstract class DeployCommand extends Command implements Arguments {
    public DeployCommand() {
        super("deploy", "Deploys all registered commands to your guild or globally.");
    }

    protected abstract boolean permissionCheck(Interaction interaction);

    @Override
    public void execute(Interaction interaction) {
        if (!permissionCheck(interaction)) {
            interaction.reply("You don't have permission to deploy commands.");
            return;
        }

        var global = interaction.getArgument("global", false, Boolean.class);
        var delete = interaction.getArgument("delete", false, Boolean.class);

        if (delete) {
            interaction.getCommandHandler().downsert(global ? null : interaction.getGuild());
            interaction.reply("Deleted all commands.");
        } else {
            interaction.getCommandHandler().deployAll(global ? null : interaction.getGuild());
            interaction.reply("Deployed all commands.");
        }
    }

    @Override
    public Collection<Argument> getArguments() {
        return List.of(
                Argument.createWithChoices("global", "Should the commands be deployed globally?", "global", OptionType.BOOLEAN, true, 0),
                Argument.createWithChoices("delete", "Should commands be deleted instead of upserted?", "delete", OptionType.BOOLEAN, false, 1)
        );
    }
}
