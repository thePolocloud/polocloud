import { ChatInputCommandInteraction, SlashCommandBuilder, PermissionFlagsBits, TextChannel, ContainerBuilder, MessageFlags, Colors, ButtonBuilder, ButtonStyle, ActionRowBuilder } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { ContributorsUpdateService } from '../../services/contributors/ContributorsUpdateService';
import { GITHUB_CONFIG } from '../../config/constants';
import axios from 'axios';

interface GitHubContributor {
    login: string;
    id: number;
    avatar_url: string;
    contributions: number;
    html_url: string;
    commits?: number;
}

export class ContributorsEmbedCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('contributorsembed')
        .setDescription('Creates a permanent, auto-updating contributors container')
        .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages);

    private logger: Logger;
    private contributorsUpdateService: ContributorsUpdateService;

    constructor(contributorsUpdateService: ContributorsUpdateService) {
        this.logger = new Logger('ContributorsEmbedCommand');
        this.contributorsUpdateService = contributorsUpdateService;
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

            const existingMessages = await channel.messages.fetch({ limit: 100 });
            const existingContributorsMessage = existingMessages.find(msg => {
                if (!msg.components.length || !msg.flags?.has(MessageFlags.IsComponentsV2)) {
                    return false;
                }

                const container = msg.components[0];
                if (!(container instanceof ContainerBuilder)) {
                    return false;
                }

                try {
                    const containerData = container.toJSON();
                    const content = JSON.stringify(containerData).toLowerCase();
                    return content.includes('contributors') || content.includes('polocloud contributors');
                } catch (error) {
                    return true;
                }
            });

            if (existingContributorsMessage) {
                await interaction.editReply('There is already a contributors container in this channel. Please remove it first using `/removecontributorsembed`.');
                return;
            }

            const contributors = await this.fetchContributorsWithCommits();

            if (contributors.length === 0) {
                await interaction.editReply('No contributors found or unable to fetch data.');
                return;
            }

            const container = this.createContributorsContainer(contributors);

            const githubButton = new ActionRowBuilder<ButtonBuilder>()
                .addComponents(
                    new ButtonBuilder()
                        .setLabel('GitHub Repository')
                        .setURL('https://github.com/HttpMarco/polocloud')
                        .setEmoji('🔗')
                        .setStyle(ButtonStyle.Link)
                );

            const message = await channel.send({
                components: [container, githubButton],
                flags: MessageFlags.IsComponentsV2
            });

            await this.contributorsUpdateService.addContainer(guild.id, channel.id, message.id);

            await interaction.editReply('✅ Contributors container created successfully! It will update automatically every hour.');

            this.logger.info(`Created contributors container in guild ${guild.id}, channel ${channel.id}`);

        } catch (error) {
            this.logger.error('Error executing ContributorsEmbed command:', error);
            await interaction.editReply('An error occurred while creating the contributors container. Please try again later.');
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

    private createContributorsContainer(contributors: GitHubContributor[]): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Blue);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 👥 PoloCloud Contributors\n\nAll contributors to the **PoloCloud** GitHub repository`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

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

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📊 Contributors (${contributors.length})\n\n${contributorsList || 'No contributors found'}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );


        return container;
    }
}