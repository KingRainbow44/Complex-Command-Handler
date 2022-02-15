package tech.xigam.cch.command;

import java.util.ArrayList;
import java.util.List;

public abstract class SubCommand extends Command implements BaseCommand {
    private final List<Alias> aliases = new ArrayList<>();
    
    public SubCommand(String label, String description) {
        super(label, description);
    }
    
    public final List<Alias> getAliases() {
        return aliases;
    }
        
    protected final void createAlias(String alias) {
        this.aliases.add(new Alias(alias, this));
    }
}
