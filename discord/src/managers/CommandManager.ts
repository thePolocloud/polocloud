import { REST, Routes, ChatInputCommandInteraction } from 'discord.js';
import { GitHubStatsEmbedCommand } from '../commands/github/GitHubStatsEmbedCommand';
import { RemoveGitHubStatsEmbedCommand } from '../commands/github/RemoveGitHubStatsEmbedCommand';
import { BStatsEmbedCommand } from '../commands/bstats/BStatsEmbedCommand';
import { RemoveBStatsEmbedCommand } from '../commands/bstats/RemoveBStatsEmbedCommand';
import { ContributorsEmbedCommand } from '../commands/contributors/ContributorsEmbedCommand';
import { RemoveContributorsEmbedCommand } from '../commands/contributors/RemoveContributorsEmbedCommand';
import { ServerInfoCommand } from '../commands/basics/ServerInfoCommand';
import { ClearCommand } from '../commands/basics/ClearCommand';
import { TicketCommand } from '../commands/basics/TicketCommand';
import { KickCommand } from '../commands/basics/KickCommand';
import { BanCommand } from "../commands/basics/BanCommand";
import { ReleaseCommand } from '../commands/basics/ReleaseCommand';
import { GitHubStatsUpdateService } from '../services/github/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/bstats/BStatsUpdateService';
import { TicketService } from '../services/ticket/TicketService';
import { ContributorsUpdateService } from "../services/contributors/ContributorsUpdateService";
import { Logger } from '../utils/Logger';
import { Command } from '../interfaces/Command';

export class CommandManager {
    private commands: Map<string, Command> = new Map();
    private logger: Logger;

    constructor(
        githubStatsUpdateService: GitHubStatsUpdateService,
        bStatsUpdateService: BStatsUpdateService,
        contributorsUpdateService: ContributorsUpdateService,
        ticketService: TicketService)
    {
        this.logger = new Logger('CommandManager');
        this.loadCommands(githubStatsUpdateService, bStatsUpdateService, ticketService, contributorsUpdateService);
    }

    private loadCommands(githubStatsUpdateService: GitHubStatsUpdateService, bStatsUpdateService: BStatsUpdateService, ticketService: TicketService, contributorsUpdateService: ContributorsUpdateService): void {
        try {
            // Load all commands
            const serverInfoCommand = new ServerInfoCommand();
            const clearCommand = new ClearCommand();
            const ticketCommand = new TicketCommand(ticketService);
            const kickCommand = new KickCommand();
            const banCommand = new BanCommand();
            const releaseCommand = new ReleaseCommand();

            // Load GitHub stats embed commands
            const githubStatsEmbedCommand = new GitHubStatsEmbedCommand(githubStatsUpdateService);
            const removeGitHubStatsEmbedCommand = new RemoveGitHubStatsEmbedCommand(githubStatsUpdateService);

            // Load bStats embed commands
            const bStatsEmbedCommand = new BStatsEmbedCommand(bStatsUpdateService);
            const removeBStatsEmbedCommand = new RemoveBStatsEmbedCommand(bStatsUpdateService);

            // Load Contributors emebd commands
            const contributorsEmbedCommand = new ContributorsEmbedCommand(contributorsUpdateService);
            const removeContriburosEmbedCommand = new RemoveContributorsEmbedCommand(contributorsUpdateService);

            this.commands.set(releaseCommand.data.name, releaseCommand)
            this.commands.set(banCommand.data.name, banCommand);
            this.commands.set(kickCommand.data.name, kickCommand)
            this.commands.set(serverInfoCommand.data.name, serverInfoCommand);
            this.commands.set(clearCommand.data.name, clearCommand);
            this.commands.set(ticketCommand.data.name, ticketCommand);
            this.commands.set(githubStatsEmbedCommand.data.name, githubStatsEmbedCommand);
            this.commands.set(removeGitHubStatsEmbedCommand.data.name, removeGitHubStatsEmbedCommand);
            this.commands.set(bStatsEmbedCommand.data.name, bStatsEmbedCommand);
            this.commands.set(removeBStatsEmbedCommand.data.name, removeBStatsEmbedCommand);
            this.commands.set(contributorsEmbedCommand.data.name, contributorsEmbedCommand);
            this.commands.set(removeContriburosEmbedCommand.data.name, removeContriburosEmbedCommand);

            this.logger.info(`${this.commands.size} commands loaded`);
        } catch (error) {
            this.logger.error('Error loading commands:', error);
            throw error;
        }
    }

    public async registerCommands(): Promise<void> {
        try {
            const rest = new REST({ version: '10' }).setToken(process.env['DISCORD_TOKEN']!);
            const commands = Array.from(this.commands.values()).map(cmd => cmd.data.toJSON());

            this.logger.info(`Registering ${commands.length} commands:`, commands.map(cmd => cmd.name));

            await rest.put(
                Routes.applicationCommands(process.env['DISCORD_CLIENT_ID']!),
                { body: commands }
            );

            this.logger.info('Commands registered successfully with Discord');
        } catch (error) {
            this.logger.error('Error registering commands:', error);
            throw error;
        }
    }

    public async handleCommand(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            const command = this.commands.get(interaction.commandName);

            if (!command) {
                this.logger.warn(`Command not found: ${interaction.commandName}`);
                await interaction.reply({
                    content: 'This command is not available.',
                    ephemeral: true
                });
                return;
            }

            await command.execute(interaction);
        } catch (error) {
            this.logger.error(`Error executing command ${interaction.commandName}:`, error);

            try {
                if (interaction.replied || interaction.deferred) {
                    await interaction.editReply('An error occurred while executing this command. Please try again later.');
                } else {
                    await interaction.reply({
                        content: 'An error occurred while executing this command. Please try again later.',
                        ephemeral: true
                    });
                }
            } catch (replyError) {
                this.logger.error('Error sending error reply:', replyError);
            }
        }
    }

    public getCommand(name: string): Command | undefined {
        return this.commands.get(name);
    }

    public getAllCommands(): Command[] {
        return Array.from(this.commands.values());
    }
}