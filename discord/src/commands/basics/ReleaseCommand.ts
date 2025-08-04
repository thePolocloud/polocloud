import { ChatInputCommandInteraction, SlashCommandBuilder, Colors, PermissionFlagsBits, TextChannel, ModalBuilder, TextInputBuilder, TextInputStyle, ActionRowBuilder, ContainerBuilder, MessageFlags } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';

export class ReleaseCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('release')
        .setDescription('Creates a formatted release announcement')
        .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('ReleaseCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            const modal = new ModalBuilder()
                .setCustomId('release_modal')
                .setTitle('📢 Create Release Announcement');

            const versionInput = new TextInputBuilder()
                .setCustomId('version')
                .setLabel('Release Version')
                .setPlaceholder('e.g., v3 Pre-4, v2.1.0')
                .setStyle(TextInputStyle.Short)
                .setRequired(true)
                .setMaxLength(50);

            const urlInput = new TextInputBuilder()
                .setCustomId('release_url')
                .setLabel('GitHub Release URL')
                .setPlaceholder('https://github.com/HttpMarco/polocloud/releases/tag/...')
                .setStyle(TextInputStyle.Short)
                .setRequired(true)
                .setMaxLength(500);

            const fixesInput = new TextInputBuilder()
                .setCustomId('fixes')
                .setLabel('Bug Fixes (optional)')
                .setPlaceholder('No template created during setup, Command parser disallowed single-command names')
                .setStyle(TextInputStyle.Paragraph)
                .setRequired(false)
                .setMaxLength(1000);

            const improvementsInput = new TextInputBuilder()
                .setCustomId('improvements')
                .setLabel('Improvements & New Features (optional)')
                .setPlaceholder('Support for Pumpkin, Reading CPU and RAM usage of services')
                .setStyle(TextInputStyle.Paragraph)
                .setRequired(false)
                .setMaxLength(1000);

            const customMessageInput = new TextInputBuilder()
                .setCustomId('custom_message')
                .setLabel('Additional Notes (optional)')
                .setPlaceholder('Any additional information you want to include')
                .setStyle(TextInputStyle.Paragraph)
                .setRequired(false)
                .setMaxLength(500);

            const firstActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(versionInput);
            const secondActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(urlInput);
            const thirdActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(fixesInput);
            const fourthActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(improvementsInput);
            const fifthActionRow = new ActionRowBuilder<TextInputBuilder>().addComponents(customMessageInput);

            modal.addComponents(firstActionRow, secondActionRow, thirdActionRow, fourthActionRow, fifthActionRow);

            await interaction.showModal(modal);

        } catch (error) {
            this.logger.error('Error executing Release command:', error);
            await interaction.reply({
                content: ' An error occurred while opening the release form. Please try again later.',
                ephemeral: true
            });
        }
    }

    public static async handleModalSubmit(interaction: any): Promise<void> {
        try {
            const version = interaction.fields.getTextInputValue('version');
            const releaseUrl = interaction.fields.getTextInputValue('release_url');
            const fixes = interaction.fields.getTextInputValue('fixes');
            const improvements = interaction.fields.getTextInputValue('improvements');
            const customMessage = interaction.fields.getTextInputValue('custom_message');

            const container = new ContainerBuilder()
                .setAccentColor(Colors.Blue);

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent('@everyone')
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`# 📢 Polocloud ${version} Release - Now Available!\n\nHey everyone! The ${version} release of Polocloud is now live - packed with bug fixes, new features, and enhanced platform support! Test it out, help us translate, and make Polocloud more stable than ever!`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 📎 Check out the release here:\n\n${releaseUrl}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent('## We\'re looking for:\n\n• Beta testers to try out new features in real environments\n• Translators who want to bring Polocloud to their native language (Crowdin ready!)')
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent('## How to join:\n\n➡ Open a ticket in # support\n➡ Briefly tell us how you\'d like to help (e.g. language or testing environment)')
            );

            let whatsNewText = '';

            if (fixes) {
                whatsNewText += '🔧 **Fixes:**\n';
                const fixesList = fixes.split(',').map((fix: string) => fix.trim());
                fixesList.forEach((fix: string) => {
                    whatsNewText += `• ${fix}\n`;
                });
                whatsNewText += '\n';
            }

            if (improvements) {
                whatsNewText += '⚡ **Improvements & New Features:**\n';
                const improvementsList = improvements.split(',').map((improvement: string) => improvement.trim());
                improvementsList.forEach((improvement: string) => {
                    whatsNewText += `• ${improvement}\n`;
                });
                whatsNewText += '\n';
            }

            if (!whatsNewText) {
                whatsNewText = '🔧 **Fixes:**\n• Various bug fixes and improvements';
            }

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## What's new in the ${version} Release?\n\n${whatsNewText}`)
            );
            if (customMessage) {
                container.addSeparatorComponents(
                    separator => separator
                );

                container.addTextDisplayComponents(
                    textDisplay => textDisplay
                        .setContent(`## Additional Notes\n\n${customMessage}`)
                );
            }

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent('💬 Your feedback is crucial – every contribution brings us closer to a final release.\nJoin in – be part of the development!\n\nWe\'re excited for your feedback and ideas.\n— The Polocloud Team ☁️')
            );

            const channel = interaction.channel as TextChannel;
            if (channel) {
                await channel.send({
                    components: [container],
                    flags: MessageFlags.IsComponentsV2
                });
            } else {
                await interaction.reply({
                    content: ' This command can only be used in a text channel.',
                    ephemeral: true
                });
                return;
            }

            await interaction.reply({
                content: '✅ Release announcement has been posted successfully!',
                ephemeral: true
            });

            const logger = new Logger('ReleaseCommand');
            logger.info(`Release announcement posted by ${interaction.user.tag} for version ${version}`);

        } catch (error) {
            const logger = new Logger('ReleaseCommand');
            logger.error('Error handling release modal submit:', error);
            await interaction.reply({
                content: ' An error occurred while creating the release announcement. Please try again later.',
                ephemeral: true
            });
        }
    }
}