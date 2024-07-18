/*
 * Copyright 2024 Mirco Lindenau | HttpMarco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.httpmarco.polocloud.base.terminal.commands;

import dev.httpmarco.polocloud.base.groups.GroupCommand;
import dev.httpmarco.polocloud.base.node.NodeCommand;
import dev.httpmarco.polocloud.base.services.ServiceCommand;
import dev.httpmarco.polocloud.base.templates.TemplateCommand;
import dev.httpmarco.polocloud.base.terminal.commands.defaults.*;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Accessors(fluent = true)
public final class CommandService {

    private final List<Object> commands = new ArrayList<>();

    public CommandService() {
        this.registerCommand(new NodeCommand());
        this.registerCommand(new GroupCommand());
        this.registerCommand(new ServiceCommand());
        this.registerCommand(new ReloadCommand());
        this.registerCommand(new PropertyCommand());
        this.registerCommand(new InfoCommand());
        this.registerCommand(new ClearCommand());
        this.registerCommand(new HelpCommand());
        this.registerCommand(new ShutdownCommand());
        this.registerCommand(new TemplateCommand());
    }

    public void registerCommand(Object command) {
        this.commands.add(command);
    }

    @SneakyThrows
    public void call(String[] args) {
        var main = args[0];
        for (var command : commands) {

            var mainCommand = command.getClass().getDeclaredAnnotation(Command.class);

            if (mainCommand == null) {
                continue;
            }

            if (!(main.equalsIgnoreCase(mainCommand.command()) || Arrays.stream(mainCommand.aliases()).anyMatch(it -> it.equalsIgnoreCase(main)))) {
                continue;
            }

            for (var method : command.getClass().getDeclaredMethods()) {
                if (args.length == 1 && method.isAnnotationPresent(DefaultCommand.class)) {
                    method.invoke(command);
                    continue;
                }

                if (!method.isAnnotationPresent(SubCommand.class)) {
                    continue;
                }

                var commandData = method.getDeclaredAnnotation(SubCommand.class);
                if (isSubCommand(args, commandData)) {
                    var params = new LinkedList<>();
                    var argIndex = 1;

                    for (var arg : commandData.args()) {
                        if (arg.startsWith("<") && arg.endsWith("...>")) {
                            var restArgs = Arrays.copyOfRange(args, argIndex, args.length);
                            params.add(String.join(" ", restArgs));
                            break;
                        } else if (arg.startsWith("<") && arg.endsWith(">")) {
                            if (argIndex < args.length) {
                                params.add(args[argIndex]);
                                argIndex++;
                            }
                        } else {
                            if (argIndex < args.length && arg.equals(args[argIndex])) {
                                argIndex++;
                            }
                        }
                    }

                    var parameters = method.getParameters();
                    for (int index = 0; index < parameters.length; index++) {
                        var parameter = parameters[index];

                        if (params.size() > index) {
                            if (parameter.getType().equals(Integer.class) || parameter.getType().equals(int.class)) {
                                params.set(index, Integer.parseInt((String) params.get(index)));
                            }
                        }
                    }

                    method.invoke(command, params.toArray());
                }
            }
        }
    }

    private static boolean isSubCommand(String[] args, SubCommand commandData) {
        var index = 0;
        var commandArgs = commandData.args();

        for (var i = 1; i < args.length; i++) {
            if (index >= commandArgs.length) {
                return false;
            }

            var subPart = commandArgs[index];

            if (subPart.startsWith("<") && subPart.endsWith("...>")) {
                return true;
            }

            if (subPart.startsWith("<") && subPart.endsWith(">")) {
                index++;
                continue;
            }

            if (!subPart.equalsIgnoreCase(args[i])) {
                return false;
            }

            index++;
        }

        return index == commandArgs.length;
    }
}
