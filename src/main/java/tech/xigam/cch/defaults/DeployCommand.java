package tech.xigam.cch.defaults;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Nullable;
import tech.xigam.cch.command.modifiers.Arguments;
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

    @Nullable
    protected MessageEmbed embedify(String text) {
        return null;
    }

    @Override
    public void execute(Interaction interaction) {
        if (!permissionCheck(interaction)) {
            interaction.reply("You don't have permission to deploy commands.");
            return;
        }

        var global = interaction.getArgument("global", false, Boolean.class);
        var delete = interaction.getArgument("delete", false, Boolean.class);

        if (!global && !interaction.isFromGuild()) {
            global = true;
            var embed = this.embedify("You can't deploy slash commands to a DM, deploying globally instead.");
            if (embed == null)
                interaction.reply("You can't deploy slash commands to a DM, deploying globally instead.");
            else interaction.reply(embed);
        }

        if (delete) {
            interaction.getCommandHandler().downsert(global ? null : interaction.getGuild());
            var embed = this.embedify("Deleted all commands.");
            if (embed == null) interaction.reply("Deleted all commands.");
            else interaction.reply(embed);
        } else {
            interaction.getCommandHandler().deployAll(global ? null : interaction.getGuild());
            var embed = this.embedify("Deployed all commands.");
            if (embed == null) interaction.reply("Deployed all commands.");
            else interaction.reply(embed);
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
