import { EmbedBuilder, Colors } from 'discord.js';
import { GitHubStats } from '../services/GitHubStatsService';
import { GITHUB_CONFIG, BOT_CONFIG } from '../config/constants';

export class GitHubEmbedBuilder {
  public static createGitHubStatsEmbed(stats: GitHubStats): EmbedBuilder {
    const embed = new EmbedBuilder()
      .setTitle('☁️ PoloCloud GitHub Statistics')
      .setDescription('Live statistics from the PoloCloud GitHub repository')
      .setColor(Colors.Blue)
      .setTimestamp()
      .setFooter({
        text: BOT_CONFIG.NAME,
        iconURL: GITHUB_CONFIG.AVATAR_URL
      });

    // Repository statistics
    embed.addFields({
      name: '📊 Repository Statistics',
      value: `\`Stars: ${stats.stars.toLocaleString('de-DE')}\`\n\`Forks: ${stats.forks.toLocaleString('de-DE')}\`\n\`Watchers: ${stats.watchers.toLocaleString('de-DE')}\`\n\`Branches: ${stats.branches.length}\``,
      inline: false
    });

    // Activity metrics
    embed.addFields({
      name: '📈 Activity Metrics',
      value: `\`Open Issues: ${stats.openIssues}\`\n\`Pull Requests: ${stats.openPullRequests}\`\n\`Contributors: ${stats.contributors}\``,
      inline: false
    });

    // Branches
    const branchesText = stats.branches
      .map(branch => `\`${branch}\``)
      .join('\n');

    embed.addFields({
      name: '🌿 Branches',
      value: branchesText || '`No branches available`',
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
      value: `[GitHub Repository](${GITHUB_CONFIG.REPO_URL.replace('/api.github.com', '/github.com')}) | [Website](${GITHUB_CONFIG.WEBSITE_URL}) | [Issues](${GITHUB_CONFIG.ISSUES_URL})`,
      inline: false
    });

    return embed;
  }
}