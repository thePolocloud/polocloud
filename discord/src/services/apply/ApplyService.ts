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
import { APPLY_CONFIG, BOT_CONFIG } from '../../config/constants';

interface ApplicationData {
    id: string;
    userId: string;
    channelId: string;
    category: string;
    createdAt: Date;
    status: 'pending' | 'approved' | 'rejected';
    name: string;
    age: string;
    experience: string;
    motivation: string;
    additionalInfo: string;
}

export class ApplyService {
    private logger: Logger;
    private applications: Map<string, ApplicationData> = new Map();

    constructor() {
        this.logger = new Logger('ApplyService');
    }

    public async createApplyContainer(channel: TextChannel): Promise<void> {
        try {
            const container = new ContainerBuilder()
                .setAccentColor(Colors.Purple);
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`# 📝 Application System\n\nSelect an application type below and click the button to submit your application. Our team will review it and get back to you.`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            this.addPartnerSection(container);

            container.addSeparatorComponents(
                separator => separator
            );

            this.addTranslatorSection(container);

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
                        .setCustomId('apply_category_select')
                        .setPlaceholder('📝 Select application type...')
                        .addOptions(
                            APPLY_CONFIG.CATEGORIES.map(cat => {
                                const emoji = cat.value === 'partner' ? APPLY_CONFIG.PARTNER_EMOJI_ID : APPLY_CONFIG.TRANSLATOR_EMOJI_ID;
                                return new StringSelectMenuOptionBuilder()
                                    .setLabel(cat.label)
                                    .setValue(cat.value)
                                    .setDescription(cat.description)
                                    .setEmoji(emoji);
                            })
                        )
                );

            await channel.send({
                components: [container, categorySelect],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`Apply container created in channel #${channel.name}`);
        } catch (error) {
            this.logger.error('Error creating apply container:', error);
        }
    }

    private addPartnerSection(container: ContainerBuilder): void {
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## ${APPLY_CONFIG.PARTNER_EMOJI_ID} Partner Requirements\n\n**Requirements for Partner Applications:**\n• [PLACEHOLDER] Minimum age requirement\n• [PLACEHOLDER] Community size requirement\n• [PLACEHOLDER] Content quality standards\n• [PLACEHOLDER] Activity requirements\n• [PLACEHOLDER] Additional criteria`)
        );
    }

    private addTranslatorSection(container: ContainerBuilder): void {
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## ${APPLY_CONFIG.TRANSLATOR_EMOJI_ID} Translator Requirements\n\n**Requirements for Translator Applications:**\n• [PLACEHOLDER] Language proficiency requirements\n• [PLACEHOLDER] Translation experience needed\n• [PLACEHOLDER] Technical knowledge required\n• [PLACEHOLDER] Availability requirements\n• [PLACEHOLDER] Quality standards`)
        );
    }

    public async handleCategorySelect(interaction: any): Promise<void> {
        try {
            const userApplications = Array.from(this.applications.values()).filter(
                application => application.userId === interaction.user.id && application.status === 'pending'
            );

            if (userApplications.length >= APPLY_CONFIG.MAX_APPLICATIONS_PER_USER) {
                await interaction.reply({
                    content: `You already have ${APPLY_CONFIG.MAX_APPLICATIONS_PER_USER} pending application. Please wait for a response before submitting a new one.`,
                    ephemeral: true
                });
                return;
            }

            const category = interaction.values[0]; 
            
            const validCategory = APPLY_CONFIG.CATEGORIES.find(cat => cat.value === category);
            if (!validCategory) {
                await interaction.reply({
                    content: `Invalid category selected.`,
                    ephemeral: true
                });
                return;
            }
            
            const modal = new ModalBuilder()
                .setCustomId(`apply_modal_${category}`)
                .setTitle(`Submit ${validCategory.label}`);

            const nameInput = new TextInputBuilder()
                .setCustomId('apply_name')
                .setLabel('Full Name')
                .setStyle(TextInputStyle.Short)
                .setPlaceholder('Enter your full name')
                .setRequired(true)
                .setMaxLength(50);

            const ageInput = new TextInputBuilder()
                .setCustomId('apply_age')
                .setLabel('Age')
                .setStyle(TextInputStyle.Short)
                .setPlaceholder('Enter your age')
                .setRequired(true)
                .setMaxLength(3);

            const experienceInput = new TextInputBuilder()
                .setCustomId('apply_experience')
                .setLabel('Experience')
                .setStyle(TextInputStyle.Paragraph)
                .setPlaceholder('Describe your relevant experience...')
                .setRequired(true)
                .setMaxLength(500);

            const motivationInput = new TextInputBuilder()
                .setCustomId('apply_motivation')
                .setLabel('Motivation')
                .setStyle(TextInputStyle.Paragraph)
                .setPlaceholder('Why do you want to join our team?')
                .setRequired(true)
                .setMaxLength(500);

            const additionalInput = new TextInputBuilder()
                .setCustomId('apply_additional')
                .setLabel('Additional Information')
                .setStyle(TextInputStyle.Paragraph)
                .setPlaceholder('Any additional information you want to share...')
                .setRequired(false)
                .setMaxLength(300);

            const firstActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(nameInput);
            const secondActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(ageInput);
            const thirdActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(experienceInput);
            const fourthActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(motivationInput);
            const fifthActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(additionalInput);

            modal.addComponents(firstActionRow, secondActionRow, thirdActionRow, fourthActionRow, fifthActionRow);
            await interaction.showModal(modal);

        } catch (error) {
            this.logger.error('Error handling category select:', error);
            await interaction.reply({
                content: 'An error occurred while processing your selection. Please try again.',
                ephemeral: true
            });
        }
    }

    public async handleApplicationModal(interaction: any): Promise<void> {
        try {
            const category = interaction.customId.replace('apply_modal_', '');
            const name = interaction.fields.getTextInputValue('apply_name');
            const age = interaction.fields.getTextInputValue('apply_age');
            const experience = interaction.fields.getTextInputValue('apply_experience');
            const motivation = interaction.fields.getTextInputValue('apply_motivation');
            const additionalInfo = interaction.fields.getTextInputValue('apply_additional') || 'None provided';
            
            const validCategory = APPLY_CONFIG.CATEGORIES.find(cat => cat.value === category);
            if (!validCategory) {
                await interaction.reply({
                    content: `Invalid category. Please use one of: ${APPLY_CONFIG.CATEGORIES.map(cat => cat.value).join(', ')}`,
                    ephemeral: true
                });
                return;
            }
            
            const guild = interaction.guild;
            const categoryChannel = guild.channels.cache.get(APPLY_CONFIG.CATEGORY_ID) as CategoryChannel;

            if (!categoryChannel) {
                await interaction.reply({
                    content: 'Application category not found. Please contact an administrator.',
                    ephemeral: true
                });
                return;
            }

            const applicationId = `${APPLY_CONFIG.APPLICATION_PREFIX}${Math.floor(Math.random() * 9000) + 1000}`;
            const channelName = `${applicationId}-${interaction.user.username}`;

            const applicationChannel = await guild.channels.create({
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
            
            if (APPLY_CONFIG.STAFF_ROLE_ID) {
                await applicationChannel.permissionOverwrites.create(APPLY_CONFIG.STAFF_ROLE_ID, {
                    ViewChannel: true,
                    SendMessages: true,
                    ReadMessageHistory: true,
                    ManageMessages: true
                });
            }
            
            const applicationData: ApplicationData = {
                id: applicationId,
                userId: interaction.user.id,
                channelId: applicationChannel.id,
                category: category,
                createdAt: new Date(),
                status: 'pending',
                name: name,
                age: age,
                experience: experience,
                motivation: motivation,
                additionalInfo: additionalInfo
            };
            this.applications.set(applicationId, applicationData);
            
            const applicationContainer = this.createApplicationDisplayContainer(applicationId, validCategory, name, age, experience, motivation, additionalInfo, interaction.user.id);

            const actionButtons = new ActionRowBuilder<ButtonBuilder>()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId(`approve_application_${applicationId}`)
                        .setLabel('Approve')
                        .setEmoji('✅')
                        .setStyle(ButtonStyle.Success),
                    new ButtonBuilder()
                        .setCustomId(`reject_application_${applicationId}`)
                        .setLabel('Reject')
                        .setEmoji('❌')
                        .setStyle(ButtonStyle.Danger)
                );

            await applicationChannel.send({
                components: [applicationContainer, actionButtons],
                flags: MessageFlags.IsComponentsV2
            });

            await interaction.reply({
                content: `✅ Application submitted successfully! Please check <#${applicationChannel.id}>`,
                ephemeral: true
            });

            this.logger.info(`Application ${applicationId} submitted by ${interaction.user.tag}`);

        } catch (error) {
            this.logger.error('Error handling application modal:', error);
            await interaction.reply({
                content: 'An error occurred while submitting the application. Please try again.',
                ephemeral: true
            });
        }
    }

    private createApplicationDisplayContainer(applicationId: string, category: any, name: string, age: string, experience: string, motivation: string, additionalInfo: string, userId: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Purple);
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 📝 Application #${applicationId}\n\n**Type:** ${category.emoji} ${category.label}\n**Applicant:** ${name} (Age: ${age})`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 👤 Personal Information\n\n**Name:** ${name}\n**Age:** ${age}\n**Discord User:** <@${userId}>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 💼 Experience\n\n${experience}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 🎯 Motivation\n\n${motivation}`)
        );

        if (additionalInfo && additionalInfo !== 'None provided') {
            container.addSeparatorComponents(
                separator => separator
            );
            
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 📋 Additional Information\n\n${additionalInfo}`)
            );
        }

        container.addSeparatorComponents(
            separator => separator
        );
        
        const detailsText = `## 📊 Application Details\n\n**Status:** Pending Review\n**Submitted:** <t:${Math.floor(Date.now() / 1000)}:R>\n`;
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

    public async handleApproveApplication(interaction: ButtonInteraction): Promise<void> {
        try {
            const applicationId = interaction.customId.replace('approve_application_', '');
            const applicationData = this.applications.get(applicationId);

            if (!applicationData) {
                await interaction.reply({
                    content: 'Application not found.',
                    ephemeral: true
                });
                return;
            }

            applicationData.status = 'approved';
            this.applications.set(applicationId, applicationData);

            const guild = interaction.guild;
            if (!guild) {
                await interaction.reply({
                    content: 'Guild not found.',
                    ephemeral: true
                });
                return;
            }

            const archiveCategory = guild.channels.cache.get(APPLY_CONFIG.ARCHIVE_CATEGORY_ID) as CategoryChannel;

            if (!archiveCategory) {
                await interaction.reply({
                    content: 'Archive category not found. Please contact an administrator.',
                    ephemeral: true
                });
                return;
            }

            const channel = interaction.channel as TextChannel;
            await channel.setName(`${channel.name}-approved`);
            await channel.setParent(archiveCategory.id);

            await channel.permissionOverwrites.set([
                {
                    id: guild.id,
                    deny: [PermissionFlagsBits.ViewChannel]
                },
                {
                    id: APPLY_CONFIG.STAFF_ROLE_ID,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory]
                }
            ]);

            const approvedContainer = this.createApplicationApprovedContainer(interaction.user.tag);

            await interaction.reply({
                components: [approvedContainer],
                flags: MessageFlags.IsComponentsV2
            });

            try {
                const user = await guild.client.users.fetch(applicationData.userId);
                await user.send(`🎉 Congratulations! Your ${APPLY_CONFIG.CATEGORIES.find(cat => cat.value === applicationData.category)?.label} has been **approved**! A team member will contact you soon.`);
            } catch (error) {
                this.logger.error(`Could not notify user ${applicationData.userId} about approval:`, error);
            }

            this.logger.info(`Application ${applicationId} approved by ${interaction.user.tag}`);

            setTimeout(async () => {
                try {
                    const archivedChannel = guild.channels.cache.get(channel.id) as TextChannel;
                    if (archivedChannel && archivedChannel.name.includes('-approved')) {
                        await archivedChannel.delete();
                        this.logger.info(`Approved application channel ${applicationId} automatically deleted after 30 days`);
                    }
                } catch (error) {
                    this.logger.error(`Error deleting approved application channel ${applicationId}:`, error);
                }
            }, 30 * 24 * 60 * 60 * 1000);

        } catch (error) {
            this.logger.error('Error approving application:', error);
            await interaction.reply({
                content: 'An error occurred while approving the application.',
                ephemeral: true
            });
        }
    }

    public async handleRejectApplication(interaction: ButtonInteraction): Promise<void> {
        try {
            const applicationId = interaction.customId.replace('reject_application_', '');
            const applicationData = this.applications.get(applicationId);

            if (!applicationData) {
                await interaction.reply({
                    content: 'Application not found.',
                    ephemeral: true
                });
                return;
            }

            applicationData.status = 'rejected';
            this.applications.set(applicationId, applicationData);

            const guild = interaction.guild;
            if (!guild) {
                await interaction.reply({
                    content: 'Guild not found.',
                    ephemeral: true
                });
                return;
            }

            const archiveCategory = guild.channels.cache.get(APPLY_CONFIG.ARCHIVE_CATEGORY_ID) as CategoryChannel;

            if (!archiveCategory) {
                await interaction.reply({
                    content: 'Archive category not found. Please contact an administrator.',
                    ephemeral: true
                });
                return;
            }

            const channel = interaction.channel as TextChannel;
            await channel.setName(`${channel.name}-rejected`);
            await channel.setParent(archiveCategory.id);

            await channel.permissionOverwrites.set([
                {
                    id: guild.id,
                    deny: [PermissionFlagsBits.ViewChannel]
                },
                {
                    id: APPLY_CONFIG.STAFF_ROLE_ID,
                    allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory]
                }
            ]);

            const rejectedContainer = this.createApplicationRejectedContainer(interaction.user.tag);

            await interaction.reply({
                components: [rejectedContainer],
                flags: MessageFlags.IsComponentsV2
            });

            try {
                const user = await guild.client.users.fetch(applicationData.userId);
                await user.send(`❌ Unfortunately, your ${APPLY_CONFIG.CATEGORIES.find(cat => cat.value === applicationData.category)?.label} has been **rejected**. You can apply again in the future if you meet the requirements.`);
            } catch (error) {
                this.logger.error(`Could not notify user ${applicationData.userId} about rejection:`, error);
            }

            this.logger.info(`Application ${applicationId} rejected by ${interaction.user.tag}`);

            setTimeout(async () => {
                try {
                    const archivedChannel = guild.channels.cache.get(channel.id) as TextChannel;
                    if (archivedChannel && archivedChannel.name.includes('-rejected')) {
                        await archivedChannel.delete();
                        this.logger.info(`Rejected application channel ${applicationId} automatically deleted after 30 days`);
                    }
                } catch (error) {
                    this.logger.error(`Error deleting rejected application channel ${applicationId}:`, error);
                }
            }, 30 * 24 * 60 * 60 * 1000);

        } catch (error) {
            this.logger.error('Error rejecting application:', error);
            await interaction.reply({
                content: 'An error occurred while rejecting the application.',
                ephemeral: true
            });
        }
    }

    private createApplicationApprovedContainer(approvedBy: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Green);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ✅ Application Approved\n\nThis application has been **approved** by **${approvedBy}**`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📁 Archive Status\n\nChannel has been moved to archive and will be automatically deleted in **30 days**.`)
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

    private createApplicationRejectedContainer(rejectedBy: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Red);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ❌ Application Rejected\n\nThis application has been **rejected** by **${rejectedBy}**`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📁 Archive Status\n\nChannel has been moved to archive and will be automatically deleted in **30 days**.`)
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

    public getApplicationStats(): { total: number; pending: number; approved: number; rejected: number } {
        const applications = Array.from(this.applications.values());
        return {
            total: applications.length,
            pending: applications.filter(a => a.status === 'pending').length,
            approved: applications.filter(a => a.status === 'approved').length,
            rejected: applications.filter(a => a.status === 'rejected').length
        };
    }
}