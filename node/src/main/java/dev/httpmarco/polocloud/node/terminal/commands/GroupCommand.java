package dev.httpmarco.polocloud.node.terminal.commands;

import dev.httpmarco.polocloud.api.platforms.PlatformGroupDisplay;
import dev.httpmarco.polocloud.node.Node;
import dev.httpmarco.polocloud.node.commands.Command;
import dev.httpmarco.polocloud.node.commands.CommandArgumentType;
import dev.httpmarco.polocloud.node.groups.ClusterGroupEditFields;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class GroupCommand extends Command {

    public GroupCommand() {
        super("group");

        var groupService = Node.instance().groupService();
        var platformService = Node.instance().platformService();
        var clusterService = Node.instance().clusterService();

        // argument for group name
        var groupIdArgument = CommandArgumentType.Text("name");
        var groupArgument = CommandArgumentType.ClusterGroup(groupService, "group");

        syntax(context -> {
            log.info("Following &b{} &7groups are loading&8:", groupService.groups().size());
            groupService.groups().forEach(group -> log.info("&8- &f{}&8: (&7{}&8)", group.name(), group.details()));
        }, "List all registered groups&8.", CommandArgumentType.Keyword("list"));

        var platformArgument = CommandArgumentType.Platform(platformService, "platform");
        var platformVersionArgument = CommandArgumentType.PlatformVersion("version");
        var minMemoryArgument = CommandArgumentType.Integer("minMemory");
        var maxMemoryArgument = CommandArgumentType.Integer("maxMemory");
        var minOnlineArgument = CommandArgumentType.Integer("minOnline");
        var maxOnlineArgument = CommandArgumentType.Integer("maxOnline");
        var staticService = CommandArgumentType.Boolean("staticService");

        syntax(context -> groupService.create(context.arg(groupIdArgument),
                        new String[]{clusterService.localNode().data().name()},
                        new PlatformGroupDisplay(context.arg(platformArgument).platform(), context.arg(platformVersionArgument).version()),
                        context.arg(minMemoryArgument),
                        context.arg(maxMemoryArgument),
                        context.arg(staticService),
                        context.arg(minOnlineArgument),
                        context.arg(maxOnlineArgument)
                ).ifPresentOrElse(s -> log.warn("Cannot create group: {}", s), () -> log.info("Successfully create group {}", context.arg(groupIdArgument)))
                , "Create a new cluster group.",
                CommandArgumentType.Keyword("create"),
                groupIdArgument,
                platformArgument,
                platformVersionArgument,
                minMemoryArgument,
                maxMemoryArgument,
                staticService,
                minOnlineArgument,
                maxOnlineArgument
        );

        syntax(context ->
                groupService.delete(context.arg(groupArgument).name())
                        .ifPresentOrElse(
                                s -> log.warn("Cannot delete group: {}", s),
                                () -> log.info("Successfully delete group {} in cluster!", context.arg(groupArgument).name())
                        ), "Delete the selected group&8.", groupArgument, CommandArgumentType.Keyword("delete"));

        syntax(context -> {
            var group = context.arg(groupArgument);
            log.info("Name&8: &b{}", group.name());
            log.info("Runtime nodes&8: &b{}", String.join("&8, &b", group.nodes()));
            log.info("Platform&8: &b{}", group.platform().details());
            log.info("Static service&8: &b{}", group.staticService());
            log.info("Minimum memory&8: &b{}mb", group.minMemory());
            log.info("Maximum memory&8: &b{}mb", group.maxMemory());
            log.info("Minimum online services&8: &b{}", group.minOnlineServerInstances());
            log.info("Maximum online services&8: &b{}", group.maxOnlineServerInstances());
        }, "Show all information about a group&8.", groupArgument, CommandArgumentType.Keyword("info"));

        syntax(context -> {

        }, "Shutdown all services with this group&8.", groupArgument, CommandArgumentType.Keyword("shutdown"));


        var editKey = CommandArgumentType.Enum(ClusterGroupEditFields.class, "key");
        var editValue = CommandArgumentType.Text("value");

        syntax(context -> {
            var group = context.arg(groupArgument);
            var editableField = context.arg(editKey);
            var value = context.arg(editValue);

            log.info("{}:{}: {}", group.name(), editableField.name(), value);

        }, "Change a property of a group&8.", groupArgument, CommandArgumentType.Keyword("edit"), editKey, editValue);

    }
}
