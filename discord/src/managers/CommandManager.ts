import { Client, REST, Routes } from 'discord.js';
import { GitHubStatsEmbedCommand } from '../commands/GitHubStatsEmbedCommand';
import { RemoveGitHubStatsEmbedCommand } from '../commands/RemoveGitHubStatsEmbedCommand';
import { ServerInfoCommand } from '../commands/ServerInfoCommand';
import { GitHubStatsUpdateService } from '../services/GitHubStatsUpdateService';
import { Logger } from '../utils/Logger';
import { Command } from '../interfaces/Command';

export class CommandManager {
    private commands: Map<string, Command> = new Map();
    private logger: Logger;

    constructor() {
        this.logger = new Logger('CommandManager');
    }

    public async loadCommands(githubStatsUpdateService?: GitHubStatsUpdateService): Promise<void> {
        try {
            // Load all commands
            const serverInfoCommand = new ServerInfoCommand();

            // Load GitHub stats embed commands if service is provided
            let githubStatsEmbedCommand: GitHubStatsEmbedCommand | undefined;
            let removeGitHubStatsEmbedCommand: RemoveGitHubStatsEmbedCommand | undefined;

            if (githubStatsUpdateService) {
                githubStatsEmbedCommand = new GitHubStatsEmbedCommand(githubStatsUpdateService);
                removeGitHubStatsEmbedCommand = new RemoveGitHubStatsEmbedCommand(githubStatsUpdateService);
            }

            this.commands.set(serverInfoCommand.data.name, serverInfoCommand);

            if (githubStatsEmbedCommand) {
                this.commands.set(githubStatsEmbedCommand.data.name, githubStatsEmbedCommand);
            }
            if (removeGitHubStatsEmbedCommand) {
                this.commands.set(removeGitHubStatsEmbedCommand.data.name, removeGitHubStatsEmbedCommand);
            }

            this.logger.info(`${this.commands.size} commands loaded`);
        } catch (error) {
            this.logger.error('Error loading commands:', error);
            throw error;
        }
    }

    public async registerCommands(_client: Client): Promise<void> {
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

    public getCommand(name: string): Command | undefined {
        return this.commands.get(name);
    }

    public getAllCommands(): Command[] {
        return Array.from(this.commands.values());
    }
}