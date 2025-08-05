import { ChatInputCommandInteraction, SlashCommandBuilder, ContainerBuilder, Colors, MessageFlags } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BOT_CONFIG } from '../../config/constants';

export class ServerInfoCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('serverinfo')
        .setDescription('Show information about this server');

    private logger: Logger;

    constructor() {
        this.logger = new Logger('ServerInfoCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            const guild = interaction.guild;

            if (!guild) {
                await interaction.reply('This command can only be used in a server.');
                return;
            }

            const owner = await guild.fetchOwner();
            const memberCount = guild.memberCount;
            const channelCount = guild.channels.cache.size;
            const roleCount = guild.roles.cache.size;
            const emojiCount = guild.emojis.cache.size;
            const boostLevel = guild.premiumTier;
            const boostCount = guild.premiumSubscriptionCount || 0;

            const container = new ContainerBuilder()
                .setAccentColor(Colors.Blue);

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`# 📊 ${guild.name}\n\n${guild.description || 'No description available'}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 📋 Server Information\n\n**Owner:** ${owner.user.tag}\n**Server ID:** ${guild.id}\n**Created:** <t:${Math.floor(guild.createdTimestamp / 1000)}:R>`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 👥 Member Statistics\n\n**Members:** ${memberCount.toLocaleString('de-DE')}\n**Channels:** ${channelCount}\n**Roles:** ${roleCount}\n**Emojis:** ${emojiCount}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🚀 Server Features\n\n**Boosts:** Level ${boostLevel}\n**Boosts:** ${boostCount}\n**Verification:** ${this.getVerificationLevel(guild.verificationLevel)}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
            );

            await interaction.reply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

        } catch (error) {
            this.logger.error('Error executing ServerInfo command:', error);
            await interaction.reply('Error loading server information. Please try again.');
        }
    }

    private getVerificationLevel(level: number): string {
        switch (level) {
            case 0: return 'None';
            case 1: return 'Low';
            case 2: return 'Medium';
            case 3: return 'High';
            case 4: return 'Very High';
            default: return 'Unknown';
        }
    }
}