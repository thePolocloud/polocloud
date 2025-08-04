import { Client, REST, Routes } from 'discord.js';
import { Logger } from '../utils/Logger';
import { Command } from '../interfaces/Command';
import { GitHubStatsCommand } from "../commands/GitHubStatsCommand";

export class CommandManager {
    private commands: Map<string, Command> = new Map();
    private logger: Logger;

    constructor() {
        this.logger = new Logger("CommandManager");
    }

    public async loadCommands(): Promise<void> {
        try {
            const githubStatsCommand = new GitHubStatsCommand();

            this.commands.set(githubStatsCommand.data.name, githubStatsCommand);


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