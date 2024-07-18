package dev.httpmarco.polocloud.rest.command;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.logging.Logger;
import dev.httpmarco.polocloud.base.terminal.commands.Command;
import dev.httpmarco.polocloud.base.terminal.commands.DefaultCommand;
import dev.httpmarco.polocloud.base.terminal.commands.SubCommand;
import dev.httpmarco.polocloud.rest.RestAPI;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Command(command = "rest", description = "Command for the REST API")
public final class RestCommand {

    private final RestAPI restAPI;
    private final Logger logger = CloudAPI.instance().logger();

    @DefaultCommand
    public void handle() {
        this.logger.info("&3user &1create &2<&1username&2> &2- &1Create a User for the Dashboard&2.");
    }

    @SubCommand(args = {"user", "create", "<username>"})
    public void createUserCommand(String username) {
        var password = this.restAPI.userManager().createUser(username);
        this.logger.info("The password is " + password);
    }

    @SubCommand(args = {"user", "delete", "<uuid>"})
    public void deleteUserCommand(String uuid) {
        var user = this.restAPI.userManager().findeUserByUUID(UUID.fromString(uuid));
        if (user == null) {
            this.logger.warn("Could not find a user with this UUID!");
            return;
        }
        this.restAPI.userManager().deleteUser(user);
    }

    @SubCommand(args = {"user", "list"})
    public void listUserCommand() {
        this.logger.info(this.restAPI.userManager().users().toString());
    }
}