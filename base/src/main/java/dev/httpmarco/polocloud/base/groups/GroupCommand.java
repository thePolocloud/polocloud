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

package dev.httpmarco.polocloud.base.groups;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.GroupProperties;
import dev.httpmarco.polocloud.api.groups.platforms.PlatformVersion;
import dev.httpmarco.polocloud.api.services.CloudService;
import dev.httpmarco.polocloud.base.Node;
import dev.httpmarco.polocloud.base.logging.Logger;
import dev.httpmarco.polocloud.base.terminal.commands.Command;
import dev.httpmarco.polocloud.base.terminal.commands.DefaultCommand;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommand;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommandCompleter;
import org.jline.reader.Candidate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Command(command = "group", aliases = {"groups"}, description = "Manage or create your cluster groups")
public final class GroupCommand {

    private final Logger logger = Node.instance().logger();

    @DefaultCommand
    public void handle() {
        logger.info("&3groups list &2- &1List all groups&2.");
        logger.info("&3groups &2<&1name&2> &2- &1Get information about a group&2.");
        logger.info("&3groups &2<&1name&2> &3property set &2<&1key&2> &2<&1value&2> &2- &1Add a property to a group&2.");
        logger.info("&3groups versions &2<&1platform&2> &2- &1List all versions of a platform&2.");
        logger.info("&3groups create &2<&1name&2> &2<&1platform&2> &2<&1memory&2> &2<&1minOnlineCount&2> &2- &1Create a new group&2.");
        logger.info("&3groups delete &2<&1name&2> &2- &1Delete an existing group&2.");
        logger.info("&3groups edit &2<&1name&2> &2<&1key&2> &2<&1value&2> &2- &1Edit a value in a group&2.");
        logger.info("&3groups shutdown &2<&1name&2> &2- &1Shutdown all services of a group&2.");
    }

    @SubCommand(args = {"<name>", "property", "set", "<key>", "<value>"})
    public void handlePropertySet(String name, String key, String value) {
        if (!CloudAPI.instance().groupProvider().isGroup(name)) {
            logger.info("This group does not exists&2!");
            return;
        }
        var group = CloudAPI.instance().groupProvider().group(name);

        // todo check maybe exist

        group.properties().pool().put(key, value);
        group.update();
        logger.success("You add successfully the property " + key + " with value " + value + " to group " + group.name() + "&2.");
    }

    @SubCommandCompleter(completionPattern = {"<name>", "property", "set", "<key>", "<value>"})
    public void completePropertySet(int index, List<Candidate> candidates) {
        this.completeProperty(index, candidates);
    }

    @SubCommand(args = {"<name>", "property", "remove", "<key>"})
    public void handlePropertyRemove(String name, String key) {
        if (!CloudAPI.instance().groupProvider().isGroup(name)) {
            logger.info("This group does not exists&2!");
            return;
        }
        var group = CloudAPI.instance().groupProvider().group(name);

        if (!group.properties().pool().containsKey(key)) {
            logger.info("This property key does not exists in group pool!");
            return;
        }

        group.properties().pool().remove(key);
        group.update();
        logger.success("You remove successfully the property " + key + " from group " + group.name() + "&2.");
    }

    @SubCommandCompleter(completionPattern = {"<name>", "property", "remove", "<key>"})
    public void completePropertyRemove(int index, List<Candidate> candidates) {
        this.completeProperty(index, candidates);
    }

    @SubCommand(args = {"list"})
    public void handleList() {
        var groups = CloudAPI.instance().groupProvider().groups();
        logger.info("Following &3" + groups.size() + " &1groups are loading&2:");
        groups.forEach(cloudGroup -> logger.info("&2- &4" + cloudGroup.name() + "&2: (&1" + cloudGroup + "&2)"));
    }

    @SubCommand(args = {"<name>"})
    public void handleInfo(String name) {
        if (!CloudAPI.instance().groupProvider().isGroup(name)) {
            logger.info("This group does not exists&2!");
            return;
        }
        var group = CloudAPI.instance().groupProvider().group(name);

        logger.info("Name&2: &3" + name);
        logger.info("Platform&2: &3" + group.platform().version());
        logger.info("Memory&2: &3" + group.memory());
        logger.info("Minimum online services&2: &3" + group.minOnlineService());
        logger.info("Properties &2(&1" + group.properties().pool().size() + "&2): &3");

        group.properties().pool().forEach((propertyId, o) -> {
            logger.info("   &2- &1" + propertyId + " &2= &1" + o);
        });
    }

    @SubCommandCompleter(completionPattern = {"<name>"})
    public void completeInfoMethod(int index, List<Candidate> candidates) {
        if (index == 1) {
            candidates.addAll(CloudAPI.instance().groupProvider().groups().stream().map(it -> new Candidate(it.name())).toList());
        }
    }

    @SubCommand(args = {"versions", "<platform>"})
    public void handleVersions(String platform) {
        List<String> versions = Node.instance().groupProvider().platformService().validPlatformVersions().stream()
                .map(PlatformVersion::version).filter(version -> version.startsWith(platform.toLowerCase() + "-")).sorted().toList();

        if (versions.isEmpty()) {
            logger.info("No versions found for platform &3" + platform + "&2!");
            return;
        }

        logger.info(String.join(", ", versions));
    }

    @SubCommandCompleter(completionPattern = {"versions", "<platform>"})
    public void completeVersionsMethod(int index, List<Candidate> candidates) {
        if (index == 2) {
            Set<String> platformNames = new HashSet<>();
            Node.instance().groupProvider().platformService().validPlatformVersions().forEach(it -> platformNames.add(it.version().split("-")[0]));
            candidates.addAll(platformNames.stream().map(Candidate::new).toList());
        }
    }

    @SubCommand(args = {"create", "<name>", "<platform>", "<memory>", "<minOnlineCount>"})
    public void handleCreate(String name, String platform, int memory, int minOnlineCount) {

        var provider = Node.instance().groupProvider();

        if (provider.isGroup(name)) {
            Node.instance().logger().info("The group already exists!");
            return;
        }

        if (memory <= 0) {
            Node.instance().logger().info("The minimum memory value must be higher then 0. ");
            return;
        }

        if (!provider.platformService().isValidPlatform(platform)) {
            Node.instance().logger().info("The platform " + platform + " is an invalid type!");
            return;
        }

        provider.createGroup(name, platform, memory, minOnlineCount);
        var group = provider.group(name);

        // we must create a separate template directory
        Node.instance().templatesService().createTemplates(name, "every", (group.platform().proxy() ? "every_proxy" : "every_server"));
        // we set as default value all important templates
        group.properties().put(GroupProperties.TEMPLATES, name);
        // send changes to other nodes or update data files
        group.update();

        logger.success("Successfully created &3" + name + " &1group&2.");
    }

    @SubCommandCompleter(completionPattern = {"create", "<name>", "<platform>", "<memory>", "<minOnlineCount>"})
    public void completeCreateMethod(int index, List<Candidate> candidates) {
        if (index == 3) {
            candidates.addAll(((CloudGroupProviderImpl) CloudAPI.instance().groupProvider()).platformService().validPlatformVersions().stream().map(platformVersion -> new Candidate(platformVersion.version())).toList());
        }
    }

    @SubCommand(args = {"delete", "<name>"})
    public void handleDelete(String name) {
        if (CloudAPI.instance().groupProvider().deleteGroup(name)) {
            logger.success("Successfully deleted &3" + name + "&2!");
        } else {
            Node.instance().logger().warn("The group does not exists!");
        }
    }

    @SubCommandCompleter(completionPattern = {"delete", "<name>"})
    public void completeDeleteMethod(int index, List<Candidate> candidates) {
        if (index == 2) {
            candidates.addAll(CloudAPI.instance().groupProvider().groups().stream().map(it -> new Candidate(it.name())).toList());
        }
    }

    @SubCommand(args = {"edit", "<name>", "<key>", "<value>"})
    public void handleEdit(String name, String key, String value) {
        //todo
    }

    @SubCommand(args = {"shutdown", "<name>"})
    public void handleShutdown(String name) {
        var group = CloudAPI.instance().groupProvider().group(name);
        if (group == null) {
            logger.info("The group does not exists&2!");
            return;
        }
        CloudAPI.instance().serviceProvider().services(group).forEach(CloudService::shutdown);
        logger.info("You successfully stopped all services of group &3" + name + "&2!");
    }

    @SubCommandCompleter(completionPattern = {"shutdown", "<name>"})
    public void completeShutdownMethod(int index, List<Candidate> candidates) {
        if (index == 2) {
            candidates.addAll(CloudAPI.instance().groupProvider().groups().stream().map(it -> new Candidate(it.name())).toList());
        }
    }

    private void completeProperty(int index, List<Candidate> candidates) {
        if (index == 1) {
            candidates.addAll(CloudAPI.instance().groupProvider().groups().stream().map(it -> new Candidate(it.name())).toList());
        } else if (index == 4) {
            //todo
            //candidates.addAll(PropertyPool.PROPERTY_LIST.stream().map(property -> new Candidate(property.id())).toList());
        } else if (index == 5) {
            candidates.addAll(Arrays.asList(new Candidate("true"), new Candidate("false")));
        }
    }

}