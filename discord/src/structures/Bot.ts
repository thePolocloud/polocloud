import { Client, GatewayIntentBits, Events, Interaction } from 'discord.js';
import { CommandManager } from '../managers/CommandManager';
import { GitHubStatsUpdateService } from '../services/github/GitHubStatsUpdateService';
import { BStatsUpdateService } from '../services/bstats/BStatsUpdateService';
import { TicketService } from '../services/ticket/TicketService';
import { ApplyService } from '../services/apply/ApplyService';
import { ContributorsUpdateService } from '../services/contributors/ContributorsUpdateService';
import { TempBanService } from '../services/moderation/TempBanService';
import { ReleaseCommand } from '../commands/basics/ReleaseCommand';
import { Logger } from '../utils/Logger';

export class Bot {
    public client: Client;
    private commandManager: CommandManager;
    private githubStatsUpdateService: GitHubStatsUpdateService;
    private bStatsUpdateService: BStatsUpdateService;
    private ticketService: TicketService;
    private applyService: ApplyService;
    private contributorsUpdateService: ContributorsUpdateService;
    private tempBanService: TempBanService;
    private logger: Logger;

    constructor() {
        this.client = new Client({
            intents: [
                GatewayIntentBits.Guilds,
                GatewayIntentBits.GuildMessages,
                GatewayIntentBits.MessageContent
            ]
        });

        this.githubStatsUpdateService = new GitHubStatsUpdateService(this.client);
        this.bStatsUpdateService = new BStatsUpdateService(this.client);
        this.ticketService = new TicketService();
        this.applyService = new ApplyService();
        this.contributorsUpdateService = new ContributorsUpdateService(this.client);
        this.tempBanService = new TempBanService(this.client);
        this.logger = new Logger('Bot');
        
        this.commandManager = new CommandManager(
            this.githubStatsUpdateService,
            this.bStatsUpdateService,
            this.contributorsUpdateService,
            this.ticketService,
            this.applyService,
            this.tempBanService
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
                    } else if (interaction.customId.startsWith('approve_application_')) {
                        await this.applyService.handleApproveApplication(interaction);
                    } else if (interaction.customId.startsWith('reject_application_')) {
                        await this.applyService.handleRejectApplication(interaction);
                    }
                } else if (interaction.isStringSelectMenu()) {
                    if (interaction.customId === 'ticket_category_select') {
                        await this.ticketService.handleCategorySelect(interaction);
                    } else if (interaction.customId === 'apply_category_select') {
                        await this.applyService.handleCategorySelect(interaction);
                    }
                } else if (interaction.isModalSubmit()) {
                    if (interaction.customId.startsWith('ticket_modal_')) {
                        await this.ticketService.handleTicketModal(interaction);
                    } else if (interaction.customId.startsWith('apply_modal_')) {
                        await this.applyService.handleApplicationModal(interaction);
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

    public stop(): void {
        this.bStatsUpdateService.stop();
        this.githubStatsUpdateService.stop();
        this.tempBanService.stop();
        this.logger.info('Bot stopped');
    }
}