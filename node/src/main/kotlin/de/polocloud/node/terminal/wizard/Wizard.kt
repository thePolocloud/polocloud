package de.polocloud.node.terminal.wizard

import de.polocloud.common.commands.InputContext
import de.polocloud.node.terminal.WizardPrompt
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.StringsCompleter

/**
 * Runs an ordered list of [WizardStep]s through a [WizardPrompt], one question at a time,
 * and turns the collected answers into a [T] once every step is answered.
 *
 * Steps can be revisited: answering `back` returns to the previous step (its answer is
 * discarded so it can be re-entered), and `exit` abandons the wizard entirely ([run]
 * returns `null`). Background terminal output is suppressed for the duration via
 * [WizardPrompt.beginQuiet] so it can't interleave with the questions.
 */
abstract class Wizard<T>(private val prompt: WizardPrompt, private val title: String) {

    private val context = InputContext()

    /** Builds the ordered questions of this wizard. Called once per [run]. */
    protected abstract fun steps(): List<WizardStep<*>>

    /** Turns the fully answered [context] into the finished [T]. */
    protected abstract fun build(context: InputContext): T

    fun run(): T? {
        val steps = steps()
        val answeredIndexes = ArrayDeque<Int>()
        var index = 0
        var error: String? = null

        prompt.beginQuiet()
        try {
            while (index < steps.size) {
                val step = steps[index]
                if (step.skip(context)) {
                    index++
                    continue
                }
                val raw = ask(steps, index, answeredIndexes, error)
                error = null

                if (raw.equals("exit", ignoreCase = true)) {
                    prompt.display("&8$title cancelled.&r")
                    return null
                }

                if (raw.equals("back", ignoreCase = true)) {
                    if (answeredIndexes.isEmpty()) {
                        error = "There is no previous step."
                        continue
                    }
                    index = answeredIndexes.removeLast()
                    context.remove(steps[index].argument)
                    continue
                }

                if (!step.argument.predication(raw)) {
                    error = "That answer isn't valid, please try again."
                    continue
                }

                val value = try {
                    step.argument.buildResult(raw, context)
                } catch (e: Exception) {
                    error = e.message ?: "That answer isn't valid, please try again."
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                val validationError = (step as WizardStep<Any?>).extraValidation(value, context)
                if (validationError != null) {
                    error = validationError
                    continue
                }

                context.append(step.argument, value)
                answeredIndexes.addLast(index)
                index++
            }

            return build(context)
        } finally {
            prompt.resetCompleter()
            prompt.endQuiet()
        }
    }

    private fun ask(steps: List<WizardStep<*>>, index: Int, answered: List<Int>, error: String?): String {
        val step = steps[index]
        val hints = step.argument.defaultArgs(context)
        prompt.setCompleter(if (hints.isEmpty()) NullCompleter() else StringsCompleter(hints))

        // Wipe the previous question off the screen and redraw from a clean slate, so the
        // "already answered" checklist below reads as a running profile of the group
        // being set up rather than a wall of scrollback.
        prompt.clearScreen()
        prompt.display("")
        if (answered.isNotEmpty()) {
            answered.forEach { prompt.display(checklistEntry(steps[it])) }
            prompt.display("")
        }
        prompt.display("&8(${index + 1}/${steps.size})&r &f${step.question(context)}")
        prompt.display("&7  ${step.description(context)}&r")
        prompt.display("")
        if (hints.isNotEmpty()) {
            prompt.display("&8  Possible answers: &3${hints.joinToString("&8, &3")}&r")
            prompt.display("")
        }
        if (index == 0) {
            prompt.display("&8  Type 'back' to return to the previous step, or 'exit' to cancel.&r")
            prompt.display("")
        }
        if (error != null) {
            prompt.display("&c  $error&r")
            prompt.display("")
        }

        return prompt.awaitInput("&8 > &f").trim()
    }

    /** Renders an already-answered step as one line of the "setup so far" checklist. */
    private fun checklistEntry(step: WizardStep<*>): String {
        @Suppress("UNCHECKED_CAST")
        val answeredStep = step as WizardStep<Any?>
        val value = context.arg(answeredStep.argument)
        return "&8  ✓ &7${answeredStep.label}: &f${answeredStep.format(value)}&r"
    }
}