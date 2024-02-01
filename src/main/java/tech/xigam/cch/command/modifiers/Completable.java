package tech.xigam.cch.command.modifiers;

import tech.xigam.cch.utils.Completion;

public interface Completable {
    void complete(Completion completion);
}
