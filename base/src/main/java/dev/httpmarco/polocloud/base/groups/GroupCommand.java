package dev.httpmarco.polocloud.base.groups;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.base.terminal.commands.Command;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommand;

public class GroupCommand {

    @Command(command = "group", aliases = "groups")
    public void handle() {
        var logger = CloudAPI.instance().logger();
        logger.info("group create [name] [platform] [memory] [minOnlineCount] - creates a new group");
        logger.info("group delete [name] - delete a group");
        logger.info("group edit [name] [key] [value] - edit group properties");
        logger.info("group list - lists all groups");
    }

    @SubCommand(args = {"create"})
    public void create() {
        CloudAPI.instance().groupProvider().createGroup("lobby", "paper-1.20.6", 512, 5);
    }

}
