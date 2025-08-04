import { ChatInputCommandInteraction, SlashCommandBuilder, PermissionFlagsBits, TextChannel } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { GitHubStatsUpdateService } from '../../services/github/GitHubStatsUpdateService';

export class RemoveGitHubStatsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('removegithubstatsembed')
        .setDescription('Removes and deletes the GitHub stats embed from the current channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: GitHubStatsUpdateService;

    constructor(updateService: GitHubStatsUpdateService) {
        this.logger = new Logger('RemoveGitHubStatsEmbedCommand');
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

            try {
                const messages = await channel.messages.fetch({ limit: 100 });
                const githubStatsMessage = messages.find(msg =>
                    msg.embeds.length > 0 &&
                    msg.embeds[0]?.title === '☁️ PoloCloud GitHub Statistics'
                );

                if (githubStatsMessage) {
                    await githubStatsMessage.delete();
                    this.logger.info(`Deleted GitHub stats embed message in channel ${channel.id}`);
                } else {
                    this.logger.warn(`No GitHub stats embed found in channel ${channel.id}`);
                }
            } catch (deleteError) {
                this.logger.error('Error deleting GitHub stats embed message:', deleteError);
            }

            await this.updateService.removeEmbed(interaction.guildId!, channel.id);

            await interaction.editReply('GitHub stats embed removed and deleted from the channel. The embed will no longer be updated automatically.');

        } catch (error) {
            this.logger.error('Error executing RemoveGitHubStatsEmbed command:', error);
            await interaction.editReply('Error removing GitHub stats embed. Please try again later.');
        }
    }
}