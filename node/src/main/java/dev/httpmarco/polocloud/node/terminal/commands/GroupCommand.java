package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.commands.CommandArgumentType;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class GroupCommand extends Command {

    public GroupCommand(ClusterGroupService groupService) {
        super("group");

        // argument for group name
        var groupIdArgument = CommandArgumentType.Text("name");
        var groupArgument = CommandArgumentType.ClusterGroup(groupService, "group");

        syntax(context -> {
            log.info("Following &b{} &7groups are loading&8:", groupService.groups().size());
            groupService.groups().forEach(group -> log.info("&8- &4{}&8: (&7{}&8)", group.name(), group));
        }, CommandArgumentType.Keyword("list"));

        syntax(context -> {
            //todo remove group
        }, "Create a new cluster group.", CommandArgumentType.Keyword("create"), groupIdArgument);

        syntax(context -> groupService.delete(context.arg(groupArgument).name()).whenComplete((s, throwable) -> {
            // todo delete group
        }), CommandArgumentType.Keyword("delete"), groupArgument);

        syntax(context -> {
            // todo info
        }, groupArgument, CommandArgumentType.Keyword("info"));

        syntax(context -> {

        }, groupArgument, CommandArgumentType.Keyword("shutdown"));


        var editKey = CommandArgumentType.Keyword("key");
        var editValue = CommandArgumentType.Keyword("value");

        syntax(context -> {

        }, groupArgument, CommandArgumentType.Keyword("edit"), editKey, editValue);

    }
}
