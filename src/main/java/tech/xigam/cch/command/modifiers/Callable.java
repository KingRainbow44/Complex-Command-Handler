package tech.xigam.cch.command.modifiers;

import tech.xigam.cch.utils.Callback;

/**
 * Declares a class as callable, allowing:
 * - buttons to work
 * - select menus to work
 * - forms to work
 */
public interface Callable {
    void callback(Callback callback);
}
