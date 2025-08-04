import { ChatInputCommandInteraction, SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';
import axios from 'axios';
import { GITHUB_CONFIG, BOT_CONFIG } from '../config/constants';

interface GitHubContributor {
    login: string;
    id: number;
    avatar_url: string;
    contributions: number;
    html_url: string;
    commits?: number;
}

export class ContributorsCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('contributors')
        .setDescription('Shows all contributors from the PoloCloud GitHub repository');

    private logger: Logger;

    constructor() {
        this.logger = new Logger('ContributorsCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply();

            const contributors = await this.fetchContributorsWithCommits();

            if (contributors.length === 0) {
                await interaction.editReply(' No contributors found or unable to fetch data.');
                return;
            }

            const embed = this.createContributorsEmbed(contributors);

            await interaction.editReply({ embeds: [embed] });
        } catch (error) {
            this.logger.error('Error executing Contributors command:', error);
            await interaction.editReply(' Error fetching contributors. Please try again later.');
        }
    }

    private async fetchContributorsWithCommits(): Promise<GitHubContributor[]> {
        try {
            const headers: any = {
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'PoloCloud-DiscordBot/1.0.0'
            };

            if (process.env['GITHUB_TOKEN']) {
                headers['Authorization'] = `token ${process.env['GITHUB_TOKEN']}`;
            }

            const contributorsResponse = await axios.get(`${GITHUB_CONFIG.REPO_URL}/contributors?per_page=100`, { headers });
            const contributors = contributorsResponse.data;

            const contributorsWithCommits = await Promise.all(
                contributors.map(async (contributor: GitHubContributor) => {
                    try {
                        const commitsResponse = await axios.get(
                            `${GITHUB_CONFIG.REPO_URL}/commits?author=${contributor.login}&per_page=1`,
                            { headers }
                        );

                        if (commitsResponse.data.length === 0) {
                            return {
                                ...contributor,
                                commits: 0
                            };
                        }

                        const linkHeader = commitsResponse.headers['link'];
                        let totalCommits = contributor.contributions;

                        if (linkHeader) {
                            const links = linkHeader.split(',');
                            const lastLink = links.find((link: string) => link.includes('rel="last"'));
                            if (lastLink) {
                                const match = lastLink.match(/page=(\d+)>/);
                                if (match) {
                                    totalCommits = parseInt(match[1]);
                                }
                            }
                        }

                        if (totalCommits === contributor.contributions) {
                            let manualCount = 0;
                            let page = 1;
                            const perPage = 100;

                            while (true) {
                                const pageResponse = await axios.get(
                                    `${GITHUB_CONFIG.REPO_URL}/commits?author=${contributor.login}&per_page=${perPage}&page=${page}`,
                                    { headers }
                                );

                                const pageCommits = pageResponse.data;
                                if (pageCommits.length === 0) break;

                                manualCount += pageCommits.length;

                                if (pageCommits.length < perPage) break;
                                page++;

                                if (page > 20) break;
                            }

                            totalCommits = manualCount;
                        }

                        return {
                            ...contributor,
                            commits: totalCommits
                        };
                    } catch (error) {
                        this.logger.warn(`Could not fetch commit count for ${contributor.login}, using fallback`);
                        return {
                            ...contributor,
                            commits: contributor.contributions
                        };
                    }
                })
            );

            return contributorsWithCommits;
        } catch (error) {
            this.logger.error('Error fetching contributors:', error);
            throw new Error('Failed to fetch contributors');
        }
    }

    private createContributorsEmbed(contributors: GitHubContributor[]): EmbedBuilder {
        const embed = new EmbedBuilder()
            .setTitle('👥 PoloCloud Contributors')
            .setDescription(`All contributors to the **PoloCloud** GitHub repository`)
            .setColor(Colors.Blue)
            .setTimestamp()
            .setFooter({ text: BOT_CONFIG.NAME, iconURL: GITHUB_CONFIG.AVATAR_URL });

        const sortedContributors = contributors.sort((a, b) => {
            const aCommits = a.commits || a.contributions;
            const bCommits = b.commits || b.contributions;
            return bCommits - aCommits;
        });

        const contributorsList = sortedContributors.map((contributor, index) => {
            const rank = index + 1;
            const rankEmoji = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : `${rank}.`;
            const commitCount = contributor.commits || contributor.contributions;
            return `${rankEmoji} **${contributor.login}** - \`${commitCount} commits\``;
        }).join('\n');

        embed.addFields({
            name: `📊 Contributors (${contributors.length})`,
            value: contributorsList || 'No contributors found',
            inline: false
        });

        embed.addFields({
            name: '🔗 Repository',
            value: `[View on GitHub](${GITHUB_CONFIG.REPO_URL})`,
            inline: false
        });

        return embed;
    }
}