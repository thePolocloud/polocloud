package de.polocloud.node.terminal.wizard

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

/**
 * A single question of a [Wizard].
 *
 * Validation and parsing of the raw answer are delegated to [argument] (the same
 * [TerminalArgument] types used by regular command syntaxes), so a wizard step behaves
 * exactly like the equivalent command-line argument. [extraValidation] covers checks a
 * bare [TerminalArgument] cannot express on its own (e.g. "this name is already taken",
 * which needs a service lookup, or a rule that relates two steps to each other) — it
 * returns a human-readable error, or `null` if the already-parsed [T] value is fine.
 *
 * [question] and [description] take the already-answered [InputContext] so a later step
 * can refer back to an earlier answer (e.g. "Which version of {platform} ...").
 *
 * [label] and [format] are only used to render this step's answer in the "already
 * answered" checklist [Wizard] shows above later questions — [label] defaults to the
 * underlying argument's key, [format] to its plain `toString()`.
 *
 * [skip] lets a step opt out based on earlier answers (e.g. a question that only makes
 * sense for one platform type) — evaluated against the already-answered [InputContext]
 * right before the step would be asked, so it can only depend on steps that come before it.
 * A skipped step is never asked and contributes nothing to the built result, so its
 * argument must be optional in [Wizard.build].
 */
class WizardStep<T>(
    val question: (InputContext) -> String,
    val description: (InputContext) -> String,
    val argument: TerminalArgument<T>,
    val label: String = argument.key,
    val format: (T) -> String = { it.toString() },
    val extraValidation: (T, InputContext) -> String? = { _, _ -> null },
    val skip: (InputContext) -> Boolean = { false },
)