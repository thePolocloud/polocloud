import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BStatsUpdateService } from '../../services/bstats/BStatsUpdateService';

export class RemoveBStatsContainerCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('removebstatscontainer')
        .setDescription('Removes the bStats container from the current channel')
        .addStringOption(option =>
            option
                .setName('platform')
                .setDescription('Choose the platform to remove')
                .setRequired(true)
                .addChoices(
                    { name: 'Velocity', value: 'velocity' },
                    { name: 'BungeeCord', value: 'bungeecord' },
                    { name: 'All (Combined)', value: 'all' }
                )
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: BStatsUpdateService;

    constructor(updateService: BStatsUpdateService) {
        this.logger = new Logger('RemoveBStatsContainerCommand');
        this.updateService = updateService;
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const platform = interaction.options.getString('platform', true);

            const messageId = await this.updateService.removeContainer(interaction.guildId!, channel.id, platform);

            if (messageId) {
                try {
                    const message = await channel.messages.fetch(messageId);
                    await message.delete();
                    await interaction.editReply(`${platform} bStats container removed successfully!`);
                } catch (error) {
                    this.logger.error('Error deleting bStats message:', error);
                    await interaction.editReply(`${platform} bStats container removed from tracking. (Could not delete message)`);
                }
            } else {
                await interaction.editReply(`No ${platform} bStats container found in this channel.`);
            }

        } catch (error) {
            this.logger.error('Error executing RemoveBStatsContainer command:', error);
            await interaction.editReply('Error removing bStats container. Please try again later.');
        }
    }
}