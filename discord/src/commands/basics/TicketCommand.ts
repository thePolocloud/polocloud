import {
    ChatInputCommandInteraction,
    SlashCommandBuilder,
    PermissionFlagsBits,
    TextChannel,
    ContainerBuilder,
    MessageFlags,
    Colors
} from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BOT_CONFIG, TICKET_CONFIG } from '../../config/constants';
import { TicketService} from "../../services/ticket/TicketService";

export class TicketCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('ticket')
        .setDescription('Create the ticket system container in the configured channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private ticketService: TicketService;

    constructor(ticketService: TicketService) {
        this.logger = new Logger('TicketCommand');
        this.ticketService = ticketService;
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });
            
            if (!TICKET_CONFIG.CHANNEL_ID) {
                await interaction.editReply('Ticket channel ID is not configured. Please set TICKET_CHANNEL_ID in your environment variables.');
                return;
            }
            
            const ticketChannel = interaction.guild?.channels.cache.get(TICKET_CONFIG.CHANNEL_ID) as TextChannel;

            if (!ticketChannel) {
                await interaction.editReply('Ticket channel not found. Please check your TICKET_CHANNEL_ID configuration.');
                return;
            }
            
            if (!TICKET_CONFIG.CATEGORY_ID) {
                await interaction.editReply('Ticket category ID is not configured. Please set TICKET_CATEGORY_ID in your environment variables.');
                return;
            }
            
            const categoryChannel = interaction.guild?.channels.cache.get(TICKET_CONFIG.CATEGORY_ID);
            if (!categoryChannel) {
                await interaction.editReply('Ticket category not found. Please check your TICKET_CATEGORY_ID configuration.');
                return;
            }

            await this.ticketService.createTicketContainer(ticketChannel);

            const successContainer = this.createSuccessContainer(ticketChannel);

            await interaction.editReply({
                components: [successContainer],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Ticket system created by ${interaction.user.tag} in channel #${ticketChannel.name}`);

        } catch (error) {
            this.logger.error('Error executing Ticket command:', error);

            const errorContainer = this.createErrorContainer();

            if (!interaction.replied && !interaction.deferred) {
                await interaction.reply({
                    components: [errorContainer],
                    flags: MessageFlags.IsComponentsV2,
                    ephemeral: true
                });
            } else {
                await interaction.editReply({
                    components: [errorContainer],
                    flags: MessageFlags.IsComponentsV2
                });
            }
        }
    }

    private createSuccessContainer(ticketChannel: TextChannel): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Green);
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ✅ Ticket System Created\n\nTicket system has been set up in <#${ticketChannel.id}>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        const configText = `## 📋 Configuration\n\n**Channel:** <#${ticketChannel.id}>\n**Category:** <#${TICKET_CONFIG.CATEGORY_ID}>\n**Max Tickets per User:** \`${TICKET_CONFIG.MAX_TICKETS_PER_USER}\``;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(configText)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        const categoriesText = `## 🎫 Categories\n\n${TICKET_CONFIG.CATEGORIES.map(cat => `${cat.emoji} ${cat.label}`).join('\n')}`;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(categoriesText)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
        );

        return container;
    }

    private createErrorContainer(): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# Error\n\nAn error occurred while creating the ticket system. Please check your configuration and try again.`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
        );

        return container;
    }
}