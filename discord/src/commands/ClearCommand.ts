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
import { BOT_CONFIG, GITHUB_CONFIG } from '../config/constants';

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

            const messages = await channel.messages.fetch({ limit: amount + 1 });

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

            const embed = new EmbedBuilder()
                .setTitle('🧹 Messages deleted')
                .setColor(Colors.Green)
                .addFields(
                    {
                        name: '✅ Successfully deleted',
                        value: `${deletedCount} ${deletedCount === 1 ? 'message' : 'messages'}`,
                        inline: true
                    }
                )
                .setTimestamp()
                .setFooter({
                    text: BOT_CONFIG.NAME,
                    iconURL: GITHUB_CONFIG.AVATAR_URL
                });

            if (tooOldCount > 0) {
                embed.addFields({
                    name: '⚠️ Not deleted',
                    value: `${tooOldCount} ${tooOldCount === 1 ? 'message' : 'messages'} (older than 14 days)`,
                    inline: true
                });
            }

            await interaction.editReply({ embeds: [embed] });

            this.logger.info(`User ${interaction.user.tag} deleted ${deletedCount} messages in channel #${channel.name} (${channel.id})`);

        } catch (error) {
            this.logger.error('Error executing Clear command:', error);

            const errorEmbed = new EmbedBuilder()
                .setTitle('Error')
                .setDescription('Error deleting messages. Please try again later.')
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