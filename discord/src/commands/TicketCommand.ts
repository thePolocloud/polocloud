import {
    ChatInputCommandInteraction,
    SlashCommandBuilder,
    PermissionFlagsBits,
    TextChannel,
    EmbedBuilder,
    Colors
} from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { BOT_CONFIG, GITHUB_CONFIG, TICKET_CONFIG } from '../config/constants';
import { TicketService } from '../services/TicketService';

export class TicketCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('ticket')
        .setDescription('Create the ticket system embed in the configured channel')
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

            await this.ticketService.createTicketEmbed(ticketChannel);

            const successEmbed = new EmbedBuilder()
                .setTitle('✅ Ticket System Created')
                .setDescription(`Ticket system has been set up in <#${ticketChannel.id}>`)
                .setColor(Colors.Green)
                .addFields(
                    {
                        name: '📋 Configuration',
                        value: `**Channel:** <#${ticketChannel.id}>\n**Category:** <#${TICKET_CONFIG.CATEGORY_ID}>\n**Max Tickets per User:** ${TICKET_CONFIG.MAX_TICKETS_PER_USER}`,
                        inline: false
                    },
                    {
                        name: '🎫 Categories',
                        value: TICKET_CONFIG.CATEGORIES.map(cat => `${cat.emoji} ${cat.label}`).join('\n'),
                        inline: false
                    }
                )
                .setTimestamp()
                .setFooter({
                    text: BOT_CONFIG.NAME,
                    iconURL: GITHUB_CONFIG.AVATAR_URL
                });

            await interaction.editReply({ embeds: [successEmbed] });
            this.logger.info(`Ticket system created by ${interaction.user.tag} in channel #${ticketChannel.name}`);

        } catch (error) {
            this.logger.error('Error executing Ticket command:', error);

            const errorEmbed = new EmbedBuilder()
                .setTitle('Error')
                .setDescription('An error occurred while creating the ticket system. Please check your configuration and try again.')
                .setColor(Colors.Red)
                .setTimestamp()
                .setFooter({
                    text: BOT_CONFIG.NAME,
                    iconURL: GITHUB_CONFIG.AVATAR_URL
                });

            if (!interaction.replied && !interaction.deferred) {
                await interaction.reply({
                    embeds: [errorEmbed],
                    ephemeral: true
                });
            } else {
                await interaction.editReply({ embeds: [errorEmbed] });
            }
        }
    }
}