package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.commands.CommandArgumentType;
import dev.httpmarco.polocloud.api.groups.ClusterGroupService;
import dev.httpmarco.polocloud.node.platforms.PlatformService;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class GroupCommand extends Command {

    public GroupCommand(ClusterGroupService groupService, PlatformService platformService) {
        super("group");

        // argument for group name
        var groupIdArgument = CommandArgumentType.Text("name");
        var groupArgument = CommandArgumentType.ClusterGroup(groupService, "group");

        syntax(context -> {
            log.info("Following &b{} &7groups are loading&8:", groupService.groups().size());
            groupService.groups().forEach(group -> log.info("&8- &4{}&8: (&7{}&8)", group.name(), group));
        }, CommandArgumentType.Keyword("list"));

        var platformArgument = CommandArgumentType.Platform(platformService, "platform");
        var platformVersionArgument = CommandArgumentType.PlatformVersion(platformService, "version");
        var minMemoryArgument = CommandArgumentType.Integer("minMemory");
        var maxMemoryArgument = CommandArgumentType.Integer("maxMemory");
        var minOnlineArgument = CommandArgumentType.Integer("minOnline");
        var maxOnlineArgument = CommandArgumentType.Integer("maxOnline");
        var staticService = CommandArgumentType.Boolean("staticService");

        syntax(context -> groupService.create(context.arg(groupIdArgument),
                        new String[0],
                        new PlatformGroupDisplay(context.arg(platformArgument).platform(), context.arg(platformVersionArgument).version()),
                        context.arg(minMemoryArgument),
                        context.arg(maxMemoryArgument),
                        context.arg(staticService),
                        context.arg(minOnlineArgument),
                        context.arg(maxOnlineArgument)
                ).whenComplete((group, throwable) -> {
                    if (throwable == null) {
                        log.info("Successfully create group {}", group.name());
                    } else {
                        log.warn("Cannot create group: {}", throwable.getMessage());
                    }
                }), "Create a new cluster group.",
                CommandArgumentType.Keyword("create"),
                groupIdArgument,
                platformArgument,
                platformVersionArgument,
                minMemoryArgument,
                maxMemoryArgument,
                minOnlineArgument,
                maxOnlineArgument
        );


        syntax(context -> groupService.delete(context.arg(groupArgument).name()).whenComplete((s, throwable) -> {
            // todo delete group
        }), CommandArgumentType.Keyword("delete"), groupArgument);

        syntax(context -> {
            // todo info
        }, groupArgument, CommandArgumentType.Keyword("info"));

        syntax(context -> {

        }, groupArgument, CommandArgumentType.Keyword("shutdown"));


        var editKey = CommandArgumentType.Text("key");
        var editValue = CommandArgumentType.Text("value");

        syntax(context -> {

        }, groupArgument, CommandArgumentType.Keyword("edit"), editKey, editValue);

    }
}
