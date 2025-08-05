import {
    TextChannel,
    CategoryChannel,
    PermissionFlagsBits,
    ChannelType,
    Colors,
    ActionRowBuilder,
    ButtonBuilder,
    ButtonStyle,
    ButtonInteraction,
    StringSelectMenuBuilder,
    StringSelectMenuOptionBuilder,
    ModalBuilder,
    TextInputBuilder,
    TextInputStyle,
    ContainerBuilder,
    MessageFlags
} from 'discord.js';
import { Logger} from "../../utils/Logger";
import { TICKET_CONFIG, BOT_CONFIG } from '../../config/constants';

interface TicketData {
    id: string;
    userId: string;
    channelId: string;
    category: string;
    createdAt: Date;
    status: 'open' | 'closed';
}

export class TicketService {
    private logger: Logger;
    private tickets: Map<string, TicketData> = new Map();

    constructor() {
        this.logger = new Logger('TicketService');
    }

    public async createTicketContainer(channel: TextChannel): Promise<void> {
        try {
            const container = new ContainerBuilder()
                .setAccentColor(Colors.Blue);
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`# 🎫 Support Tickets\n\nSelect a category below and click the button. A team member will take care of you right away.`)
            );

            container.addSeparatorComponents(
                separator => separator
            );
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
            );

            const categorySelect = new ActionRowBuilder<StringSelectMenuBuilder>()
                .addComponents(
                    new StringSelectMenuBuilder()
                        .setCustomId('ticket_category_select')
                        .setPlaceholder('🎫 Select category...')
                        .addOptions(
                            TICKET_CONFIG.CATEGORIES.map(cat =>
                                new StringSelectMenuOptionBuilder()
                                    .setLabel(cat.label)
                                    .setValue(cat.value)
                                    .setDescription(cat.description)
                                    .setEmoji(cat.emoji)
                            )
                        )
                );

            await channel.send({
                components: [container, categorySelect],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Ticket container created in channel #${channel.name}`);
        } catch (error) {
            this.logger.error('Error creating ticket container:', error);
        }
    }

    public async handleCategorySelect(interaction: any): Promise<void> {
        try {
            const userTickets = Array.from(this.tickets.values()).filter(
                ticket => ticket.userId === interaction.user.id && ticket.status === 'open'
            );

            if (userTickets.length >= TICKET_CONFIG.MAX_TICKETS_PER_USER) {
                await interaction.reply({
                    content: `You already have ${TICKET_CONFIG.MAX_TICKETS_PER_USER} open tickets. Please close one before creating a new one.`,
                    ephemeral: true
                });
                return;
            }

            const category = interaction.values[0]; 
            
            const validCategory = TICKET_CONFIG.CATEGORIES.find(cat => cat.value === category);
            if (!validCategory) {
                await interaction.reply({
                    content: `Invalid category selected.`,
                    ephemeral: true
                });
                return;
            }
            
            const modal = new ModalBuilder()
                .setCustomId(`ticket_modal_${category}`)
                .setTitle(`Create ${validCategory.label} Ticket`);

            const subjectInput = new TextInputBuilder()
                .setCustomId('ticket_subject')
                .setLabel('Subject')
                .setStyle(TextInputStyle.Short)
                .setPlaceholder('Brief description of your issue')
                .setRequired(true)
                .setMaxLength(100);

            const descriptionInput = new TextInputBuilder()
                .setCustomId('ticket_description')
                .setLabel('Description')
                .setStyle(TextInputStyle.Paragraph)
                .setPlaceholder('Please provide detailed information about your issue...')
                .setRequired(true)
                .setMaxLength(1000);

            const firstActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(subjectInput);
            const secondActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(descriptionInput);

            modal.addComponents(firstActionRow, secondActionRow);
            await interaction.showModal(modal);

        } catch (error) {
            this.logger.error('Error handling category select:', error);
            await interaction.reply({
                content: 'An error occurred while processing your selection. Please try again.',
                ephemeral: true
            });
        }
    }

    public async handleTicketModal(interaction: any): Promise<void> {
        try {
            const category = interaction.customId.replace('ticket_modal_', '');
            const subject = interaction.fields.getTextInputValue('ticket_subject');
            const description = interaction.fields.getTextInputValue('ticket_description');
            
            const validCategory = TICKET_CONFIG.CATEGORIES.find(cat => cat.value === category);
            if (!validCategory) {
                await interaction.reply({
                    content: `Invalid category. Please use one of: ${TICKET_CONFIG.CATEGORIES.map(cat => cat.value).join(', ')}`,
                    ephemeral: true
                });
                return;
            }
            
            const guild = interaction.guild;
            const categoryChannel = guild.channels.cache.get(TICKET_CONFIG.CATEGORY_ID) as CategoryChannel;

            if (!categoryChannel) {
                await interaction.reply({
                    content: 'Ticket category not found. Please contact an administrator.',
                    ephemeral: true
                });
                return;
            }

            const ticketId = `${TICKET_CONFIG.TICKET_PREFIX}${Math.floor(Math.random() * 9000) + 1000}`;
            const channelName = `${ticketId}-${interaction.user.username}`;

            const ticketChannel = await guild.channels.create({
                name: channelName,
                type: ChannelType.GuildText,
                parent: categoryChannel,
                permissionOverwrites: [
                    {
                        id: guild.id,
                        deny: [PermissionFlagsBits.ViewChannel]
                    },
                    {
                        id: interaction.user.id,
                        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory]
                    }
                ]
            });
            
            if (TICKET_CONFIG.SUPPORT_ROLE_ID) {
                await ticketChannel.permissionOverwrites.create(TICKET_CONFIG.SUPPORT_ROLE_ID, {
                    ViewChannel: true,
                    SendMessages: true,
                    ReadMessageHistory: true,
                    ManageMessages: true
                });
            }
            
            const ticketData: TicketData = {
                id: ticketId,
                userId: interaction.user.id,
                channelId: ticketChannel.id,
                category: category,
                createdAt: new Date(),
                status: 'open'
            };
            this.tickets.set(ticketId, ticketData);
            
            const ticketContainer = this.createTicketDisplayContainer(ticketId, validCategory, subject, description, interaction.user.id);

            const closeButton = new ActionRowBuilder<ButtonBuilder>()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId(`close_ticket_${ticketId}`)
                        .setLabel('Close Ticket')
                        .setEmoji('🔒')
                        .setStyle(ButtonStyle.Danger)
                );

            await ticketChannel.send({
                components: [ticketContainer, closeButton],
                flags: MessageFlags.IsComponentsV2
            });

            await interaction.reply({
                content: `✅ Ticket created successfully! Please check <#${ticketChannel.id}>`,
                ephemeral: true
            });

            this.logger.info(`Ticket ${ticketId} created by ${interaction.user.tag}`);

        } catch (error) {
            this.logger.error('Error handling ticket modal:', error);
            await interaction.reply({
                content: 'An error occurred while creating the ticket. Please try again.',
                ephemeral: true
            });
        }
    }

    private createTicketDisplayContainer(ticketId: string, category: any, subject: string, description: string, userId: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Green);
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 🎫 Ticket #${ticketId}\n\n**Category:** ${category.emoji} ${category.label}\n**Subject:** ${subject}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📝 Description\n\n${description}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        const detailsText = `## 📊 Details\n\n**Created by:** <@${userId}>\n**Created:** <t:${Math.floor(Date.now() / 1000)}:R>\n`;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(detailsText)
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

    public async handleCloseTicket(interaction: ButtonInteraction): Promise<void> {
        try {
            const ticketId = interaction.customId.replace('close_ticket_', '');
            const ticketData = this.tickets.get(ticketId);

            if (!ticketData) {
                await interaction.reply({
                    content: 'Ticket not found.',
                    ephemeral: true
                });
                return;
            }

            ticketData.status = 'closed';
            this.tickets.set(ticketId, ticketData);

            const guild = interaction.guild;
            if (!guild) {
                await interaction.reply({
                    content: 'Guild not found.',
                    ephemeral: true
                });
                return;
            }

            const archiveCategory = guild.channels.cache.get(TICKET_CONFIG.ARCHIVE_CATEGORY_ID) as CategoryChannel;

            if (!archiveCategory) {
                await interaction.reply({
                    content: 'Archive category not found. Please contact an administrator.',
                    ephemeral: true
                });
                return;
            }

            const channel = interaction.channel as TextChannel;
            await channel.setName(`${channel.name}-archived`);
            await channel.setParent(archiveCategory.id);

            await channel.permissionOverwrites.set([
                {
                    id: guild.id,
                    deny: [PermissionFlagsBits.ViewChannel]
                },
                {
                    id: TICKET_CONFIG.SUPPORT_ROLE_ID,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory]
                }
            ]);

            const closedContainer = this.createTicketClosedContainer(interaction.user.tag);

            await interaction.reply({
                components: [closedContainer],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Ticket ${ticketId} closed and moved to archive by ${interaction.user.tag}`);

            setTimeout(async () => {
                try {
                    const archivedChannel = guild.channels.cache.get(channel.id) as TextChannel;
                    if (archivedChannel && archivedChannel.name.includes('-archived')) {
                        await archivedChannel.delete();
                        this.logger.info(`Archived ticket channel ${ticketId} automatically deleted after 14 days`);
                    }
                } catch (error) {
                    this.logger.error(`Error deleting archived channel ${ticketId}:`, error);
                }
            }, 14 * 24 * 60 * 60 * 1000);

        } catch (error) {
            this.logger.error('Error closing ticket:', error);
            await interaction.reply({
                content: 'An error occurred while closing the ticket.',
                ephemeral: true
            });
        }
    }

    private createTicketClosedContainer(closedBy: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 🔒 Ticket Closed\n\nThis ticket has been closed by **${closedBy}**`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📁 Archive Status\n\nChannel has been moved to archive and will be automatically deleted in **14 days**.`)
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

    public getTicketStats(): { total: number; open: number; closed: number } {
        const tickets = Array.from(this.tickets.values());
        return {
            total: tickets.length,
            open: tickets.filter(t => t.status === 'open').length,
            closed: tickets.filter(t => t.status === 'closed').length
        };
    }
}