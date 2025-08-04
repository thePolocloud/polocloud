import { ChatInputCommandInteraction, SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import axios from 'axios';

interface GitHubStats {
    stars: number;
    forks: number;
    watchers: number;
    openIssues: number;
    openPullRequests: number;
    contributors: number;
    languages: Array<{
        name: string;
        percentage: number;
    }>;
    lastCommit: string;
    license: string;
    description: string;
}

export class GitHubStatsCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('githubstats')
        .setDescription('Shows detailed GitHub statistics for PoloCloud');

    private logger: Logger;

    constructor() {
        this.logger = new Logger('GitHubStatsCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply();

            const stats = await this.fetchGitHubStats();
            const embed = this.createGitHubStatsEmbed(stats);

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            this.logger.error('Error executing GitHubStats command:', error);
            await interaction.editReply('Error loading GitHub statistics. Please try again later.');
        }
    }

    private async fetchGitHubStats(): Promise<GitHubStats> {
        try {
            // GitHub API v3 for repository information
            const repoResponse = await axios.get('https://api.github.com/repos/HttpMarco/polocloud', {
                headers: {
                    'Accept': 'application/vnd.github.v3+json',
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
                }
            });

            const repoData = repoResponse.data;

            // GitHub API for Languages
            const languagesResponse = await axios.get('https://api.github.com/repos/HttpMarco/polocloud/languages', {
                headers: {
                    'Accept': 'application/vnd.github.v3+json',
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
                }
            });

            // GitHub API for Pull Requests (open)
            const pullRequestsResponse = await axios.get('https://api.github.com/repos/HttpMarco/polocloud/pulls?state=open', {
                headers: {
                    'Accept': 'application/vnd.github.v3+json',
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
                }
            });

            // GitHub API for Contributors
            const contributorsResponse = await axios.get('https://api.github.com/repos/HttpMarco/polocloud/contributors', {
                headers: {
                    'Accept': 'application/vnd.github.v3+json',
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
                }
            });

            // GitHub API for latest commit
            const commitsResponse = await axios.get('https://api.github.com/repos/HttpMarco/polocloud/commits?per_page=1', {
                headers: {
                    'Accept': 'application/vnd.github.v3+json',
                    'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
                }
            });

            const languagesData = languagesResponse.data;
            const totalBytes = Object.values(languagesData).reduce((sum: number, bytes: any) => sum + bytes, 0);

            const languages = Object.entries(languagesData)
                .map(([name, bytes]: [string, any]) => ({
                    name,
                    percentage: Math.round((bytes / totalBytes) * 100)
                }))
                .sort((a, b) => b.percentage - a.percentage)
                .slice(0, 5); // Top 5 languages

            const lastCommitDate = commitsResponse.data.length > 0
                ? new Date(commitsResponse.data[0].commit.author.date).toLocaleDateString('de-DE')
                : new Date(repoData.updated_at).toLocaleDateString('de-DE');

            return {
                stars: repoData.stargazers_count,
                forks: repoData.forks_count,
                watchers: repoData.watchers_count,
                openIssues: repoData.open_issues_count,
                openPullRequests: pullRequestsResponse.data.length,
                contributors: contributorsResponse.data.length,
                languages,
                lastCommit: lastCommitDate,
                license: repoData.license?.name,
                description: repoData.description
            };

        } catch (error) {
            this.logger.error('Error loading GitHub statistics:', error);
            throw new Error('Failed to fetch GitHub statistics');
        }
    }

    private createGitHubStatsEmbed(stats: GitHubStats): EmbedBuilder {
        const embed = new EmbedBuilder()
            .setTitle('☁️ PoloCloud GitHub Statistics')
            .setDescription('Live statistics from the PoloCloud GitHub repository')
            .setColor(Colors.Blue)
            .setTimestamp()
            .setFooter({
                text: 'PoloCloud Discord Bot',
                iconURL: 'https://github.com/HttpMarco/polocloud/blob/master/.img/img.png?raw=true'
            });

        // Repository statistics
        embed.addFields({
            name: '📊 Repository Statistics',
            value: `\`Stars: ${stats.stars.toLocaleString('de-DE')}\`\n\`Forks: ${stats.forks.toLocaleString('de-DE')}\`\n\`Watchers: ${stats.watchers.toLocaleString('de-DE')}\``,
            inline: false
        });

        // Activity metrics
        embed.addFields({
            name: '📈 Activity Metrics',
            value: `\`Open Issues: ${stats.openIssues}\`\n\`Pull Requests: ${stats.openPullRequests}\`\n\`Contributors: ${stats.contributors}\``,
            inline: false
        });

        // Technology stack
        const languagesText = stats.languages
            .map(lang => `\`${lang.name}: ${lang.percentage}%\``)
            .join('\n');

        embed.addFields({
            name: '💻 Technology Stack',
            value: languagesText || '`No language data available`',
            inline: false
        });

        // Repository details
        embed.addFields({
            name: '📋 Repository Details',
            value: `\`License: ${stats.license || 'Nicht angegeben'}\`\n\`Last Commit: ${stats.lastCommit}\`\n\`Description: ${stats.description || 'Keine Beschreibung verfügbar'}\``,
            inline: false
        });

        // Links
        embed.addFields({
            name: '🔗 Links',
            value: '[GitHub Repository](https://github.com/HttpMarco/polocloud) | [Website](https://polocloud.de) | [Issues](https://github.com/HttpMarco/polocloud/issues)',
            inline: false
        });

        return embed;
    }
}