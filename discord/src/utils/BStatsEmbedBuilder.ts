import { EmbedBuilder, Colors } from 'discord.js';
import { BStatsData } from '../services/BStatsService';
import { BSTATS_CONFIG, BOT_CONFIG, GITHUB_CONFIG } from '../config/constants';

export class BStatsEmbedBuilder {
    public static createBStatsEmbed(stats: BStatsData, title: string): EmbedBuilder {
        const embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription('Live statistics from bStats.org')
            .setColor(Colors.Blue)
            .setTimestamp()
            .setFooter({
                text: BOT_CONFIG.NAME,
                iconURL: GITHUB_CONFIG.AVATAR_URL
            });

        const hasData = stats.servers.current >= 0 || stats.players.current >= 0;

        if (hasData) {
            embed.addFields({
                name: '📊 Server Statistics',
                value: `\`Current Servers: ${stats.servers.current.toLocaleString('de-DE')}\`\n\`Record Servers: ${stats.servers.record.toLocaleString('de-DE')}\``,
                inline: false
            });

            embed.addFields({
                name: '👥 Player Statistics',
                value: `\`Current Players: ${stats.players.current.toLocaleString('de-DE')}\`\n\`Record Players: ${stats.players.record.toLocaleString('de-DE')}\``,
                inline: false
            });

        } else {
            embed.addFields({
                name: '❌ No Data Available',
                value: '`No statistics could be loaded from bStats.org`',
                inline: false
            });
        }

        embed.addFields({
            name: '🔗 Links',
            value: `[Velocity bStats](${BSTATS_CONFIG.BASE_URL}/velocity/polocloud/${BSTATS_CONFIG.VELOCITY_PLUGIN_ID}) | [BungeeCord bStats](${BSTATS_CONFIG.BASE_URL}/bungeecord/polocloud/${BSTATS_CONFIG.BUNGEECORD_PLUGIN_ID}) | [GitHub](${GITHUB_CONFIG.REPO_URL.replace('/api.github.com', '/github.com')})`,
            inline: false
        });

        return embed;
    }
}