import { ChatInputCommandInteraction, SlashCommandBuilder, PermissionFlagsBits, TextChannel } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { GitHubStatsUpdateService } from '../../services/github/GitHubStatsUpdateService';

export class RemoveGitHubStatsContainerCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('removegithubstatscontainer')
        .setDescription('Removes the GitHub stats container from the current channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: GitHubStatsUpdateService;

    constructor(updateService: GitHubStatsUpdateService) {
        this.logger = new Logger('RemoveGitHubStatsContainerCommand');
        this.updateService = updateService;
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const guild = interaction.guild;
            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const messageId = await this.updateService.removeEmbed(guild.id, channel.id);

            if (messageId) {
                try {
                    const message = await channel.messages.fetch(messageId);
                    await message.delete();
                    await interaction.editReply('GitHub stats container removed successfully!');
                } catch (error) {
                    this.logger.error('Error deleting GitHub stats message:', error);
                    await interaction.editReply('GitHub stats container removed from tracking. (Could not delete message)');
                }
            } else {
                await interaction.editReply('No GitHub stats container found in this channel.');
            }

        } catch (error) {
            this.logger.error('Error executing RemoveGitHubStatsContainer command:', error);
            await interaction.editReply('Error removing GitHub stats container. Please try again later.');
        }
    }
}