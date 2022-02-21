package tech.xigam.cch.command;

import tech.xigam.cch.utils.Completion;

public interface Completable {
    void complete(Completion completion);
}
