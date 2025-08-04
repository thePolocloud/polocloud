import { Client, GatewayIntentBits, Events, Interaction } from 'discord.js';
import { CommandManager } from '../managers/CommandManager';
import { GitHubStatsUpdateService } from '../services/github/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/bstats/BStatsUpdateService';
import { TicketService } from '../services/ticket/TicketService';
import { ContributorsUpdateService } from "../services/contributors/ContributorsUpdateService";
import { ReleaseCommand } from '../commands/basics/ReleaseCommand';
import { Logger } from '../utils/Logger';

export class Bot {
    public client: Client;
    private commandManager: CommandManager;
    private githubStatsUpdateService: GitHubStatsUpdateService;
    private bStatsUpdateService: BStatsUpdateService;
    private contributorsUpdateService: ContributorsUpdateService;
    private ticketService: TicketService;
    private logger: Logger;

    constructor() {
        this.client = new Client({
            intents: [
                GatewayIntentBits.Guilds,
                GatewayIntentBits.GuildMessages,
                GatewayIntentBits.MessageContent
            ]
        });

        this.logger = new Logger('Bot');
        this.githubStatsUpdateService = new GitHubStatsUpdateService(this.client);
        this.bStatsUpdateService = new BStatsUpdateService(this.client);
        this.contributorsUpdateService = new ContributorsUpdateService(this.client);
        this.ticketService = new TicketService();
        this.commandManager = new CommandManager(
            this.githubStatsUpdateService,
            this.bStatsUpdateService,
            this.contributorsUpdateService,
            this.ticketService
        );

        this.setupEventHandlers();
    }

    private setupEventHandlers(): void {
        this.client.on(Events.InteractionCreate, async (interaction: Interaction) => {
            try {
                if (interaction.isChatInputCommand()) {
                    await this.commandManager.handleCommand(interaction);
                } else if (interaction.isButton()) {
                    if (interaction.customId.startsWith('close_ticket_')) {
                        await this.ticketService.handleCloseTicket(interaction);
                    }
                } else if (interaction.isStringSelectMenu()) {
                    if (interaction.customId === 'ticket_category_select') {
                        await this.ticketService.handleCategorySelect(interaction);
                    }
                } else if (interaction.isModalSubmit()) {
                    if (interaction.customId.startsWith('ticket_modal_')) {
                        await this.ticketService.handleTicketModal(interaction);
                    } else if (interaction.customId === 'release_modal') {
                        await ReleaseCommand.handleModalSubmit(interaction);
                    }
                }
            } catch (error) {
                this.logger.error('Error handling interaction:', error);

                try {
                    if (interaction.isRepliable()) {
                        const reply = interaction.replied ? interaction.followUp : interaction.reply;
                        await reply({
                            content: 'An error occurred while processing your request. Please try again later.',
                            ephemeral: true
                        });
                    }
                } catch (replyError) {
                    this.logger.error('Error sending error reply:', replyError);
                }
            }
        });

        this.client.on(Events.ClientReady, () => {
            this.logger.info(`Logged in as ${this.client.user?.tag}`);

            this.githubStatsUpdateService.start();
            this.bStatsUpdateService.start();
            this.contributorsUpdateService.start();
        });
    }

    public async start(): Promise<void> {
        try {
            this.logger.info('Starting PoloCloud Discord Bot...');

            await this.commandManager.registerCommands();

            await this.client.login(process.env['DISCORD_TOKEN']);
        } catch (error) {
            this.logger.error('Error starting bot:', error);
            throw error;
        }
    }

    public async stop(): Promise<void> {
        try {
            this.githubStatsUpdateService.stop();
            this.bStatsUpdateService.stop();
            this.contributorsUpdateService.stop();
            await this.client.destroy();
            this.logger.info('Bot stopped successfully');
        } catch (error) {
            this.logger.error('Error stopping bot:', error);
            throw error;
        }
    }
}