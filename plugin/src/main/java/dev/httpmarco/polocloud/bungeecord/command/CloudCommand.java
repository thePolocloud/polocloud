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

package dev.httpmarco.polocloud.bungeecord.command;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.services.CloudService;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;

public class CloudCommand extends Command {

    public CloudCommand() {
        super("cloud");
    }

    // todo: make Messages! -> Adding Minimessages
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage("Cloud | Sei ein Spieler");
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!(sender.hasPermission("cloud.command"))) {
            sender.sendMessage("§cYou dont have the Permissions");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "group", "groups" -> {
                this.handleGroupCommand(player, args);
            }
            case "service", "ser", "serv" -> {
                this.handleServiceCommand(player, args);
            }
            default -> {
                this.sendUsage(player);
            }
        }

        this.sendUsage(player);
    }

    private void sendUsage(ProxiedPlayer player) {
        player.sendMessage("Cloud | /cloud group list - Hole alle Gruppen ab");
        player.sendMessage("Cloud | /cloud group <GroupName> - Info aller Gruppen");
        player.sendMessage("Cloud | /cloud group shutdown <GroupName> - Stoppe alle Server der Gruppe");

        player.sendMessage("Cloud | /cloud service list - Hole alle service ab");
        player.sendMessage("Cloud | /cloud service <ServiceName> - Info aller services");
        player.sendMessage("Cloud | /cloud service shutdown <ServiceName> - Stoppe den service");
        // TODO: Adding copy Command for Copy the Server into the Template!
    }

    private void handleGroupCommand(ProxiedPlayer player, String[] args) {
        switch (args.length) {
            case 2 -> {
                if (args[1].equalsIgnoreCase("list")) {
                    final List<CloudGroup> groups = CloudAPI.instance().groupProvider().groups();
                    player.sendMessage("Following &3" + groups.size() + " &1groups are loading&2:");
                    groups.forEach(cloudGroup -> player.sendMessage("&2- &4" + cloudGroup.name() + "&2: (&1" + cloudGroup + "&2)"));
                    return;
                }

                final String groupName = args[0];
                if (!CloudAPI.instance().groupProvider().isGroup(groupName)) {
                    player.sendMessage("This group does not exists&2!");
                    return;
                }
                final CloudGroup group = CloudAPI.instance().groupProvider().group(groupName);

                player.sendMessage("Name&2: &3" + groupName);
                player.sendMessage("Platform&2: &3" + group.platform().version());
                player.sendMessage("Memory&2: &3" + group.memory());
                player.sendMessage("Minimum online services&2: &3" + group.minOnlineServices());
                player.sendMessage("Properties &2(&1" + group.properties().properties().size() + "&2): &3");

                group.properties().properties().forEach((groupProperties, o) -> {
                    player.sendMessage("   &2- &1" + groupProperties.id() + " &2= &1" + o.toString());
                });
            }
            case 3 -> {
                if (!(args[1].equalsIgnoreCase("shutdown"))) {
                    this.sendUsage(player);
                    return;
                }
                final String groupName = args[2];
                if (!CloudAPI.instance().groupProvider().isGroup(groupName)) {
                    player.sendMessage("This group does not exists&2!");
                    return;
                }
                final CloudGroup cloudGroup = CloudAPI.instance().groupProvider().group(groupName);
                CloudAPI.instance().serviceProvider().services(cloudGroup).forEach(CloudService::shutdown);
                player.sendMessage("You successfully stopped all services of group &3" + groupName + "&2!");
            }
        }
    }

    private void handleServiceCommand(ProxiedPlayer player, String[] args) {
        switch (args.length) {
            case 2 -> {
                if (args[1].equalsIgnoreCase("list")) {
                    final List<CloudService> services = CloudAPI.instance().serviceProvider().services();
                    player.sendMessage("Following &3" + services.size() + " &1services are loading&2:");
                    services.forEach(cloudService -> player.sendMessage("&2- &4" + cloudService.name() + "&2: (&1" + cloudService.group().name() + "&2)"));
                    return;
                }

                final String serviceName = args[1];
                var service = CloudAPI.instance().serviceProvider().service(serviceName);
                if (service == null) {
                    player.sendMessage("This services does not exists&2!");
                    return;
                }

                player.sendMessage("Name&2: &3" + serviceName);
                player.sendMessage("Platform&2: &3" + service.group().platform().version());
                player.sendMessage("Current memory&2: &3-1");
                player.sendMessage("Players&2: &3-1");
                player.sendMessage("Maximal players&2: &3" + service.maxPlayers());
                player.sendMessage("Port &2: &3" + service.port());
                player.sendMessage("State&2: &3" + service.state());
                player.sendMessage("Properties &2(&1" + service.properties().properties().size() + "&2): &3");

                service.properties().properties().forEach((groupProperties, o) -> {
                    player.sendMessage("   &2- &1" + groupProperties.id() + " &2= &1" + o.toString());
                });
            }
            case 3 -> {
                if (args[1].equalsIgnoreCase("shutdown")) {
                    final String serviceName = args[2];
                    var service = CloudAPI.instance().serviceProvider().service(serviceName);
                    if (service == null) {
                        player.sendMessage("This services does not exists&2!");
                        return;
                    }

                    service.shutdown();
                    player.sendMessage("This services " + service.name() + " stopping now..");
                    return;
                }
                this.sendUsage(player);
            }
        }
    }

}
