import { ContainerBuilder, Colors } from 'discord.js';
import { BStatsData } from '../services/bstats/BStatsService';
import { BSTATS_CONFIG, BOT_CONFIG } from '../config/constants';

export class BStatsContainerBuilder {
    public static createBStatsContainer(stats: BStatsData, title: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Blue);
        
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ${title}\n\nLive statistics from bStats.org`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        const hasData = stats.servers.current >= 0 || stats.players.current >= 0;

        if (hasData) {
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 📊 Server Statistics\n\n**Current Servers:** ${stats.servers.current.toLocaleString('de-DE')}\n**Record Servers:** ${stats.servers.record.toLocaleString('de-DE')}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 👥 Player Statistics\n\n**Current Players:** ${stats.players.current.toLocaleString('de-DE')}\n**Record Players:** ${stats.players.record.toLocaleString('de-DE')}`)
            );

        } else {
            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## ❌ No Data Available\n\nNo statistics could be loaded from bStats.org`)
            );
        }

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 🔗 Links\n\n[Velocity bStats](${BSTATS_CONFIG.BASE_URL}/velocity/polocloud/${BSTATS_CONFIG.VELOCITY_PLUGIN_ID}) | [BungeeCord bStats](${BSTATS_CONFIG.BASE_URL}/bungeecord/polocloud/${BSTATS_CONFIG.BUNGEECORD_PLUGIN_ID}) | [GitHub](https://github.com/HttpMarco/polocloud)`)
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