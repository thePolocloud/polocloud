import { Client, GatewayIntentBits, ActivityType } from 'discord.js';
import { CommandManager } from '../managers/CommandManager';
import { Logger } from '../utils/Logger';
import { GitHubStatsUpdateService } from '../services/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/BStatsUpdateService';

export class Bot {
    public client: Client;
    private commandManager: CommandManager;
    private logger: Logger;
    private githubStatsUpdateService: GitHubStatsUpdateService;
    private bStatsUpdateService: BStatsUpdateService;

    constructor() {
        this.client = new Client({
            intents: [
                GatewayIntentBits.Guilds,
                GatewayIntentBits.GuildMessages,
                GatewayIntentBits.MessageContent,
            ],
        });

        this.logger = new Logger('Bot');
        this.githubStatsUpdateService = new GitHubStatsUpdateService(this.client);
        this.bStatsUpdateService = new BStatsUpdateService(this.client);
        this.commandManager = new CommandManager(this.githubStatsUpdateService, this.bStatsUpdateService);

        this.setupEventHandlers();
    }

    private setupEventHandlers(): void {
        this.client.on('ready', () => {
            this.logger.info(`Logged in as ${this.client.user?.tag}`);
            this.client.user?.setActivity('PoloCloud Stats', { type: ActivityType.Watching });
        });

        this.client.on('interactionCreate', async (interaction) => {
            if (!interaction.isChatInputCommand()) return;

            try {
                await this.commandManager.executeCommand(interaction);
            } catch (error) {
                this.logger.error('Error handling command:', error);
                const reply = {
                    content: 'An error occurred while executing this command.',
                    ephemeral: true,
                };

                if (interaction.replied || interaction.deferred) {
                    await interaction.editReply(reply);
                } else {
                    await interaction.reply(reply);
                }
            }
        });
    }

    public async start(): Promise<void> {
        try {
            await this.client.login(process.env['DISCORD_TOKEN']!);
            await this.commandManager.registerCommands();
            this.githubStatsUpdateService.start();
            this.bStatsUpdateService.start();
            this.logger.info('Bot started successfully');
        } catch (error) {
            this.logger.error('Failed to start bot:', error);
            throw error;
        }
    }

    public async stop(): Promise<void> {
        try {
            this.githubStatsUpdateService.stop();
            this.bStatsUpdateService.stop();
            await this.client.destroy();
            this.logger.info('Bot stopped successfully');
        } catch (error) {
            this.logger.error('Error stopping bot:', error);
            throw error;
        }
    }
}