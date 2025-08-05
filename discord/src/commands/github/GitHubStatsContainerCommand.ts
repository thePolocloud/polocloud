import { ChatInputCommandInteraction, SlashCommandBuilder, TextChannel, PermissionFlagsBits, MessageFlags } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { GitHubStatsUpdateService } from '../../services/github/GitHubStatsUpdateService';
import { GitHubStatsService } from '../../services/github/GitHubStatsService';
import { GitHubContainerBuilder } from '../../utils/GitHubContainerBuilder';

export class GitHubStatsContainerCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('githubstatscontainer')
        .setDescription('Creates a permanent GitHub stats container in the current channel')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator);

    private logger: Logger;
    private updateService: GitHubStatsUpdateService;
    private statsService: GitHubStatsService;

    constructor(updateService: GitHubStatsUpdateService) {
        this.logger = new Logger('GitHubStatsContainerCommand');
        this.updateService = updateService;
        this.statsService = new GitHubStatsService();
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const channel = interaction.channel as TextChannel;
            if (!channel) {
                await interaction.editReply('This command can only be used in a text channel.');
                return;
            }

            const stats = await this.statsService.fetchGitHubStats();
            const container = GitHubContainerBuilder.createGitHubStatsContainer(stats);
            
            const message = await channel.send({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            await this.updateService.addContainer(interaction.guildId!, channel.id, message.id);

            await interaction.editReply(`GitHub stats container created successfully! [View Message](${message.url})\n\nThe container will automatically update every 10 minutes.`);

        } catch (error) {
            this.logger.error('Error executing GitHubStatsContainer command:', error);
            await interaction.editReply('Error creating GitHub stats container. Please try again later.');
        }
    }
}