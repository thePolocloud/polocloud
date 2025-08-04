import { Client, Interaction, ChatInputCommandInteraction } from 'discord.js';
import { CommandManager } from '../managers/CommandManager';
import { GitHubStatsUpdateService } from '../services/GitHubStatsUpdateService';
import { Logger } from '../utils/Logger';

export class Bot {
    private client: Client;
    private commandManager: CommandManager;
    private githubStatsUpdateService: GitHubStatsUpdateService;
    private logger: Logger;

    constructor(client: Client) {
        this.client = client;
        this.commandManager = new CommandManager();
        this.githubStatsUpdateService = new GitHubStatsUpdateService(client);
        this.logger = new Logger('Bot');

        this.initializeCommands();
    }

    private async initializeCommands(): Promise<void> {
        try {
            await this.commandManager.loadCommands(this.githubStatsUpdateService);
            await this.commandManager.registerCommands(this.client);
            this.logger.info('Commands loaded and registered successfully');
        } catch (error) {
            this.logger.error('Error loading commands:', error);
        }
    }

    public async start(): Promise<void> {
        try {
            // Start the GitHub stats update service
            this.githubStatsUpdateService.start();
            this.logger.info('GitHub Stats Update Service started');
        } catch (error) {
            this.logger.error('Error starting services:', error);
        }
    }

    public async stop(): Promise<void> {
        try {
            // Stop the GitHub stats update service
            this.githubStatsUpdateService.stop();
            this.logger.info('GitHub Stats Update Service stopped');
        } catch (error) {
            this.logger.error('Error stopping services:', error);
        }
    }

    public async handleInteraction(interaction: Interaction): Promise<void> {
        try {
            if (interaction.isChatInputCommand()) {
                await this.handleCommand(interaction);
            }
        } catch (error) {
            this.logger.error('Error processing interaction:', error);

            if (interaction.isRepliable()) {
                await interaction.reply({
                    content: 'An error occurred. Please try again later.',
                    ephemeral: true,
                });
            }
        }
    }

    private async handleCommand(interaction: ChatInputCommandInteraction): Promise<void> {
        const commandName = interaction.commandName;
        const command = this.commandManager.getCommand(commandName);

        if (!command) {
            this.logger.warn(`Unknown command: ${commandName}`);
            await interaction.reply({
                content: 'This command is not available.',
                ephemeral: true,
            });
            return;
        }

        try {
            await command.execute(interaction);
        } catch (error) {
            this.logger.error(`Error executing command ${commandName}:`, error);

            if (!interaction.replied && !interaction.deferred) {
                await interaction.reply({
                    content: 'An error occurred while executing the command.',
                    ephemeral: true,
                });
            } else {
                await interaction.followUp({
                    content: 'An error occurred while executing the command.',
                    ephemeral: true,
                });
            }
        }
    }
}