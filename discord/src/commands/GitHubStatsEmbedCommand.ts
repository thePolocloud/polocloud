import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import { GitHubStatsUpdateService } from '../services/github/GitHubStatsUpdateService';
import { GitHubStatsService } from '../services/github/GitHubStatsService';
import { GitHubEmbedBuilder } from '../utils/GitHubEmbedBuilder';

export class GitHubStatsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('githubstatsembed')
        .setDescription('Creates a permanent GitHub stats embed in the current channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: GitHubStatsUpdateService;
    private githubStatsService: GitHubStatsService;

    constructor(updateService: GitHubStatsUpdateService) {
        this.logger = new Logger('GitHubStatsEmbedCommand');
        this.updateService = updateService;
        this.githubStatsService = new GitHubStatsService();
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const stats = await this.githubStatsService.fetchGitHubStats();
            const embed = GitHubEmbedBuilder.createGitHubStatsEmbed(stats);

            const message = await channel.send({ embeds: [embed] });

            await this.updateService.addEmbed(interaction.guildId!, channel.id, message.id);

            await interaction.editReply(`GitHub stats embed created successfully! [View Message](${message.url})\n\nThe embed will automatically update every 10 minutes.`);

        } catch (error) {
            this.logger.error('Error executing GitHubStatsEmbed command:', error);
            await interaction.editReply('Error creating GitHub stats embed. Please try again later.');
        }
    }
}