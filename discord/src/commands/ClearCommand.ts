import {
    ChatInputCommandInteraction,
    SlashCommandBuilder,
    PermissionFlagsBits,
    TextChannel,
    ContainerBuilder,
    MessageFlags,
    Colors
} from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { BOT_CONFIG } from '../config/constants';

export class ClearCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('clear')
        .setDescription('Delete a specified number of messages in the current channel')
        .addIntegerOption(option =>
            option
                .setName('amount')
                .setDescription('Number of messages to delete (1-100)')
                .setRequired(true)
                .setMinValue(1)
                .setMaxValue(100)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('ClearCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const amount = interaction.options.getInteger('amount', true);

            if (!interaction.memberPermissions?.has(PermissionFlagsBits.ManageMessages)) {
                await interaction.editReply('You do not have permission to delete messages.');
                return;
            }

            if (!channel.permissionsFor(interaction.client.user!)?.has(PermissionFlagsBits.ManageMessages)) {
                await interaction.editReply('I do not have permission to delete messages in this channel.');
                return;
            }


            const messages = await channel.messages.fetch({ limit: amount + 1 }); //


            const deletableMessages = messages.filter(msg => {
                const messageAge = Date.now() - msg.createdTimestamp;
                return messageAge < 14 * 24 * 60 * 60 * 1000;
            });

            if (deletableMessages.size === 0) {
                await interaction.editReply('No deletable messages found. (Messages older than 14 days cannot be deleted)');
                return;
            }

            const deletedMessages = await channel.bulkDelete(deletableMessages, true);

            const deletedCount = deletedMessages.size;
            const tooOldCount = messages.size - deletedCount;

            const container = this.createSuccessContainer(deletedCount, tooOldCount);

            await interaction.editReply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`User ${interaction.user.tag} deleted ${deletedCount} messages in channel #${channel.name} (${channel.id})`);

        } catch (error) {
            this.logger.error('Error executing Clear command:', error);

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

    private createSuccessContainer(deletedCount: number, tooOldCount: number): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Green);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 🧹 Messages Deleted\n\nSuccessfully cleared messages from this channel`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        const successText = `## ✅ Successfully Deleted\n\n\`${deletedCount} ${deletedCount === 1 ? 'message' : 'messages'}\``;
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(successText)
        );

        if (tooOldCount > 0) {
            container.addSeparatorComponents(
                separator => separator
            );

            const warningText = `## ⚠️ Not Deleted\n\n\`${tooOldCount} ${tooOldCount === 1 ? 'message' : 'messages'} (older than 14 days)\``;
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(warningText)
            );
        }

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
                .setContent(`# ❌ Error\n\nAn error occurred while deleting messages. Please try again later.`)
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