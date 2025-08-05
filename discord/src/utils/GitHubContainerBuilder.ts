import { ContainerBuilder, Colors } from 'discord.js';
import { GitHubStats } from '../services/github/GitHubStatsService';
import { GITHUB_CONFIG, BOT_CONFIG } from '../config/constants';

export class GitHubContainerBuilder {
  public static createGitHubStatsContainer(stats: GitHubStats): ContainerBuilder {
    const container = new ContainerBuilder()
      .setAccentColor(Colors.Blue);

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`# ☁️ PoloCloud GitHub Statistics\n\nLive statistics from the PoloCloud GitHub repository`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 📊 Repository Statistics\n\n**Stars:** ${stats.stars.toLocaleString('de-DE')}\n**Forks:** ${stats.forks.toLocaleString('de-DE')}\n**Watchers:** ${stats.watchers.toLocaleString('de-DE')}\n**Branches:** ${stats.branches.length}`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 📈 Activity Metrics\n\n**Open Issues:** ${stats.openIssues}\n**Pull Requests:** ${stats.openPullRequests}\n**Contributors:** ${stats.contributors}`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    const branchesText = stats.branches
      .map(branch => `\`${branch}\``)
      .join('\n');

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 🌿 Branches\n\n${branchesText || 'No branches available'}`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    const languagesText = stats.languages
      .map(lang => `\`${lang.name}: ${lang.percentage}%\``)
      .join('\n');

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 💻 Technology Stack\n\n${languagesText || 'No language data available'}`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 📋 Repository Details\n\n**License:** ${stats.license || 'Nicht angegeben'}\n**Last Commit:** ${stats.lastCommit}\n**Description:** ${stats.description || 'Keine Beschreibung verfügbar'}`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`## 🔗 Links\n\n[GitHub Repository](${GITHUB_CONFIG.REPO_URL.replace('/api.github.com', '/github.com')}) | [Website](${GITHUB_CONFIG.WEBSITE_URL}) | [Issues](${GITHUB_CONFIG.ISSUES_URL})`)
    );

    container.addSeparatorComponents(
      separator => separator
    );

    container.addTextDisplayComponents(
      textDisplay => textDisplay
        .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
    );

    return container;
  }
}