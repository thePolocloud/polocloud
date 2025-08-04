import { Client, GatewayIntentBits, Events, Interaction } from 'discord.js';
import { CommandManager } from '../managers/CommandManager';
import { GitHubStatsUpdateService } from '../services/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/BStatsUpdateService';
import { TicketService } from '../services/TicketService';
import { Logger } from '../utils/Logger';

export class Bot {
    public client: Client;
    private commandManager: CommandManager;
    private githubStatsUpdateService: GitHubStatsUpdateService;
    private bStatsUpdateService: BStatsUpdateService;
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
        this.ticketService = new TicketService();
        this.commandManager = new CommandManager(
            this.githubStatsUpdateService,
            this.bStatsUpdateService,
            this.ticketService
        );

        this.setupEventHandlers();
    }

    private setupEventHandlers(): void {
        this.client.on(Events.ClientReady, () => {
            this.logger.info(`Logged in as ${this.client.user?.tag}`);
            this.githubStatsUpdateService.start();
            this.bStatsUpdateService.start();
        });

        this.client.on(Events.InteractionCreate, async (interaction: Interaction) => {
            try {
                if (interaction.isChatInputCommand()) {
                    await this.commandManager.executeCommand(interaction);
                } else if (interaction.isButton()) {
                    await this.handleButtonInteraction(interaction);
                } else if (interaction.isStringSelectMenu()) {
                    await this.handleStringSelectMenu(interaction);
                } else if (interaction.isModalSubmit()) {
                    await this.handleModalSubmit(interaction);
                }
            } catch (error) {
                this.logger.error('Error handling interaction:', error);

                const errorMessage = 'An error occurred while processing your request. Please try again.';

                if (interaction.isRepliable()) {
                    if (interaction.replied || interaction.deferred) {
                        await interaction.editReply({ content: errorMessage });
                    } else {
                        await interaction.reply({ content: errorMessage, ephemeral: true });
                    }
                }
            }
        });
    }

    private async handleButtonInteraction(interaction: any): Promise<void> {
        if (interaction.customId.startsWith('close_ticket_')) {
            await this.ticketService.handleCloseTicket(interaction);
        }
    }

    private async handleStringSelectMenu(interaction: any): Promise<void> {
        if (interaction.customId === 'ticket_category_select') {
            await this.ticketService.handleCategorySelect(interaction);
        }
    }

    private async handleModalSubmit(interaction: any): Promise<void> {
        if (interaction.customId.startsWith('ticket_modal_')) {
            await this.ticketService.handleTicketModal(interaction);
        }
    }

    public async start(): Promise<void> {
        try {
            await this.client.login(process.env['DISCORD_TOKEN']);
            await this.commandManager.registerCommands();
        } catch (error) {
            this.logger.error('Error starting bot:', error);
            throw error;
        }
    }

    public async stop(): Promise<void> {
        this.githubStatsUpdateService.stop();
        this.bStatsUpdateService.stop();
        this.client.destroy();
    }
}