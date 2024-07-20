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

package dev.httpmarco.polocloud.base.templates;

import dev.httpmarco.polocloud.base.Node;
import dev.httpmarco.polocloud.base.logging.Logger;
import dev.httpmarco.polocloud.base.terminal.commands.Command;
import dev.httpmarco.polocloud.base.terminal.commands.DefaultCommand;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommand;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommandCompleter;
import dev.httpmarco.pololcoud.common.files.FileUtils;
import lombok.SneakyThrows;
import org.jline.reader.Candidate;

import java.io.File;
import java.util.List;

@Command(command = "template", aliases = {"templates",}, description = "Manage or merge templates")
public final class TemplateCommand {

    private final Logger logger = Node.instance().logger();

    @DefaultCommand
    public void handle() {
        logger.info("&3template delete &2<&1name&2> &2- &1Delete a existing template&2.");
        logger.info("&3template list &2- &1List all available templates&2.");
        logger.info("&3template copy &2<&1source&2> &2<&1target&2> &2- &1Copy a existing template to another&2.");
    }

    @SubCommand(args = {"delete", "<name>"})
    public void handleDelete(String name) {
        if (Node.instance().templatesService().deleteTemplate(name)) {
            logger.success("The template &2'&4" + name + "&2' &1has been deleted successfully");
        } else {
            logger.info("The template &2'&4" + name + "&2' &1does not exists!");
        }
    }

    @SubCommandCompleter(completionPattern = {"delete", "<name>"})
    public void completeDeleteMethod(int index, List<Candidate> candidates) {
        if (index == 2) {
            candidates.addAll(Node.instance().templatesService().templates().stream().map(it -> new Candidate(it.id())).toList());
        }
    }

    @SneakyThrows
    @SubCommand(args = {"copy", "<from>", "<to>"})
    public void handleCopy(String from, String to) {
        var templatesFolder = TemplatesService.TEMPLATES.toFile();
        var fromTemplate = new File(templatesFolder, from);
        var toTemplate = new File(templatesFolder, to);

        if (!Node.instance().templatesService().isTemplate(from)) {
            logger.info("The template &2'&4" + from + "&2' &1 does not exists!");
            return;
        }

        if (!Node.instance().templatesService().isTemplate(to)) {
            Node.instance().templatesService().createTemplates(to);
        }

        //todo check with other nodes
        FileUtils.copyDirectoryContents(fromTemplate.toPath(), toTemplate.toPath());
        logger.info("The template &2'&4" + from + "&2' &1has been successfully copied to &2'&4" + to + "&2'");
    }

    @SubCommandCompleter(completionPattern = {"copy", "<from>", "<to>"})
    public void completeCopyMethod(int index, List<Candidate> candidates) {
        if (index == 2 || index == 3) {
            candidates.addAll(Node.instance().templatesService().templates().stream().map(it -> new Candidate(it.id())).toList());
        }
    }

    @SubCommand(args = {"list"})
    public void handleList() {
        if (Node.instance().templatesService().templates() == null) {
            logger.info("No templates were found");
        }
        logger.info("Following templates were found&2:");
        Node.instance().templatesService().templates().forEach(template -> logger.info("&2- &4" + template.id()));
    }
}

