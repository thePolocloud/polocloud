import { ChatInputCommandInteraction, SlashCommandBuilder, EmbedBuilder, Colors } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';

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


            const embed = new EmbedBuilder()
                .setTitle(`📊 ${guild.name}`)
                .setDescription(guild.description || 'No description available')
                .setColor(Colors.Blue)
                .setThumbnail(guild.iconURL({ size: 256 }))
                .setTimestamp()
                .setFooter({
                    text: 'PoloCloud Discord Bot',
                    iconURL: 'https://github.com/HttpMarco/polocloud/blob/master/.img/img.png?raw=true'
                });

            embed.addFields({
                name: '📋 Server Information',
                value: `\`Owner: ${owner.user.tag}\`\n\`Server ID: ${guild.id}\`\n\`Created: <t:${Math.floor(guild.createdTimestamp / 1000)}:R>\``,
                inline: false
            });

            embed.addFields({
                name: '👥 Member Statistics',
                value: `\`Members: ${memberCount.toLocaleString('de-DE')}\`\n\`Channels: ${channelCount}\`\n\`Roles: ${roleCount}\`\n\`Emojis: ${emojiCount}\``,
                inline: false
            });

            embed.addFields({
                name: '🚀 Server Features',
                value: `\`Boosts: Level ${boostLevel}\`\n\`Boosts: ${boostCount}\`\n\`Verification: ${this.getVerificationLevel(guild.verificationLevel)}\``,
                inline: false
            });

            if (guild.banner) {
                embed.setImage(guild.bannerURL({ size: 1024 })!);
            }

            await interaction.reply({ embeds: [embed] });

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