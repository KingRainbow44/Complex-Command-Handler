package tech.xigam.cch.utils;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.Nullable;

public final class MenuOption {
    public static MenuOption create(
            String label, String reference
    ) {
        return MenuOption.create(label, null, reference, null);
    }

    public static MenuOption create(
            String label, @Nullable String description,
            String reference
    ) {
        return MenuOption.create(label, description, reference, null);
    }

    public static MenuOption create(
            String label, @Nullable String description,
            String reference, @Nullable Emoji emoji
    ) {
        return new MenuOption(label, description, reference, emoji);
    }

    private MenuOption(
            String label, @Nullable String description,
            String reference, @Nullable Emoji emoji
    ) {
        this.label = label;
        this.description = description;
        this.reference = reference;
        this.emoji = emoji;
    }

    public SelectOption asOption() {
        var option = SelectOption.of(this.label, this.reference);
        if (this.description != null) option = option.withDescription(this.description);
        if (this.emoji != null) option = option.withEmoji(this.emoji);

        return option;
    }

    public final String label, description;
    public final String reference;
    public final Emoji emoji;
}
