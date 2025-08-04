import { REST, Routes, ChatInputCommandInteraction } from 'discord.js';
import { GitHubStatsEmbedCommand } from '../commands/GitHubStatsEmbedCommand';
import { RemoveGitHubStatsEmbedCommand } from '../commands/RemoveGitHubStatsEmbedCommand';
import { BStatsEmbedCommand } from '../commands/BStatsEmbedCommand';
import { RemoveBStatsEmbedCommand } from '../commands/RemoveBStatsEmbedCommand';
import { ServerInfoCommand } from '../commands/ServerInfoCommand';
import { ClearCommand } from '../commands/ClearCommand';
import { TicketCommand } from '../commands/TicketCommand';
import { GitHubStatsUpdateService } from '../services/github/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/bstats/BStatsUpdateService';
import { TicketService } from '../services/ticket/TicketService';
import { Logger } from '../utils/Logger';
import { Command } from '../interfaces/Command';

export class CommandManager {
    private commands: Map<string, Command> = new Map();
    private logger: Logger;

    constructor(githubStatsUpdateService: GitHubStatsUpdateService, bStatsUpdateService: BStatsUpdateService, ticketService: TicketService) {
        this.logger = new Logger('CommandManager');
        this.loadCommands(githubStatsUpdateService, bStatsUpdateService, ticketService);
    }

    private loadCommands(githubStatsUpdateService: GitHubStatsUpdateService, bStatsUpdateService: BStatsUpdateService, ticketService: TicketService): void {
        try {
            // Load all commands
            const serverInfoCommand = new ServerInfoCommand();
            const clearCommand = new ClearCommand();
            const ticketCommand = new TicketCommand(ticketService);

            // Load GitHub stats embed commands
            const githubStatsEmbedCommand = new GitHubStatsEmbedCommand(githubStatsUpdateService);
            const removeGitHubStatsEmbedCommand = new RemoveGitHubStatsEmbedCommand(githubStatsUpdateService);

            // Load bStats embed commands
            const bStatsEmbedCommand = new BStatsEmbedCommand(bStatsUpdateService);
            const removeBStatsEmbedCommand = new RemoveBStatsEmbedCommand(bStatsUpdateService);

            this.commands.set(serverInfoCommand.data.name, serverInfoCommand);
            this.commands.set(clearCommand.data.name, clearCommand);
            this.commands.set(ticketCommand.data.name, ticketCommand);
            this.commands.set(githubStatsEmbedCommand.data.name, githubStatsEmbedCommand);
            this.commands.set(removeGitHubStatsEmbedCommand.data.name, removeGitHubStatsEmbedCommand);
            this.commands.set(bStatsEmbedCommand.data.name, bStatsEmbedCommand);
            this.commands.set(removeBStatsEmbedCommand.data.name, removeBStatsEmbedCommand);

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

    public async executeCommand(interaction: ChatInputCommandInteraction): Promise<void> {
        const commandName = interaction.commandName;
        const command = this.commands.get(commandName);

        if (!command) {
            this.logger.warn(`Unknown command: ${commandName}`);
            await interaction.reply({
                content: 'This command is not available.',
                ephemeral: true,
            });
            return;
        }

        await command.execute(interaction);
    }

    public getCommand(name: string): Command | undefined {
        return this.commands.get(name);
    }

    public getAllCommands(): Command[] {
        return Array.from(this.commands.values());
    }
}