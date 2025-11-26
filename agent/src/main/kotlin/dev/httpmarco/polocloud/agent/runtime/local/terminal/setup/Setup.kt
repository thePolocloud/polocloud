package dev.httpmarco.polocloud.agent.runtime.local.terminal.setup

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.local.terminal.JLine3Terminal
import dev.httpmarco.polocloud.agent.runtime.local.terminal.LoggingColor
import dev.httpmarco.polocloud.agent.runtime.local.terminal.arguments.InputContext

abstract class Setup<T>(private val name: String, private val canExited: Boolean = true) {

    private lateinit var terminal: JLine3Terminal
    private val steps: ArrayDeque<SetupStep<*>> = ArrayDeque()
    private val completedSteps: ArrayDeque<SetupStep<*>> = ArrayDeque()
    val context = InputContext()

    abstract fun bindQuestion()

    abstract fun onComplete(result: InputContext): T

    fun attach(step: SetupStep<*>) {
        this.steps.add(step)
    }

    fun start(terminal: JLine3Terminal) {
        this.bindQuestion()
        this.terminal = terminal
        this.terminal.updatePrompt("&7$name&8 » &7")
        this.display()
    }

    fun stop() {
        this.terminal.clearScreen()
        this.terminal.resetPrompt()

        //todo
        //logger.flushLogs()
        i18n.info("agent.local-runtime.setup.exited", this.name)
    }

    fun next() {
        if (steps.size <= 1) {
            this.terminal.setupController.completeCurrentSetup()

            this.terminal.clearScreen()
            this.terminal.resetPrompt()
            i18n.info("agent.local-runtime.setup.completed", this.name)
            this.onComplete(context)
            return
        }

        val finished = this.steps.removeFirst()

        val value = context.arg(finished.argument)

        @Suppress("UNCHECKED_CAST")
        val typed = finished.action as (Any?) -> Unit
        typed(value)

        this.completedSteps.addLast(finished)

        this.display()
    }

    private fun display(wrongAnswer: Boolean = false, noPreviousStep: Boolean = false, answer: String = "") {
        val current = step()
        val translatedQuestion = i18n.get(current.questionKey)

        this.terminal.clearScreen()
        this.terminal.emptyLine()
        this.terminal.display(LoggingColor.translate("&8 > &f$translatedQuestion"))

        val defaultArgs = current.argument.defaultArgs(context).filter { it != current.argument.key }
        if (defaultArgs.isNotEmpty()) {
            this.terminal.display(
                LoggingColor.translate(
                    i18n.get(
                        "agent.local-runtime.setup.possible-answers",
                        defaultArgs.joinToString("&8, &3")
                    )
                )
            )
            this.terminal.emptyLine()
        }

        if (wrongAnswer) {
            this.terminal.display(LoggingColor.translate("&8 > &c" + current.argument.wrongReason(answer)))
        }

        this.terminal.display(
            i18n.get(
                if (canExited)
                    "agent.local-runtime.setup.info.with-exit"
                else
                    "agent.local-runtime.setup.info.without-exit"
            )
        )

        if (noPreviousStep) {
            this.terminal.display(LoggingColor.translate(i18n.get("agent.local-runtime.setup.no-previous-step")))
        }

        this.terminal.emptyLine()
    }

    fun acceptAnswer(answer: String) {
        if (canExited && answer.equals("exit", ignoreCase = true)) {
            this.terminal.setupController.exit()
            this.terminal.clearScreen()
            this.terminal.resetPrompt()
            return
        }

        if (answer.equals("back", ignoreCase = true)) {
            if (this.completedSteps.isEmpty()) {
                display(noPreviousStep = true)
                return
            }

            val lastSteps = completedSteps.removeLast()
            steps.addFirst(lastSteps)

            this.context.remove(lastSteps.argument)

            this.display()
            return
        }

        val argument = step().argument

        if (!argument.predication(answer)) {
            this.display(true, answer = answer)
            return
        }

        this.context.append(argument, argument.buildResult(answer, context))
        this.next()
    }

    fun step(): SetupStep<*> {
        return this.steps.first()
    }
}