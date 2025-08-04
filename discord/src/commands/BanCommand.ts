import { ChatInputCommandInteraction, SlashCommandBuilder, EmbedBuilder, Colors, PermissionFlagsBits, GuildMember } from 'discord.js';
import { Command } from '../interfaces/Command';
import { Logger } from '../utils/Logger';

export class BanCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('ban')
        .setDescription('Bans a user from the server')
        .addUserOption(option =>
            option
                .setName('user')
                .setDescription('The user to ban')
                .setRequired(true)
        )
        .addStringOption(option =>
            option
                .setName('reason')
                .setDescription('Reason for banning the user')
                .setRequired(false)
        )
        .addStringOption(option =>
            option
                .setName('duration')
                .setDescription('Duration of the ban (e.g., 7d, 24h, 30m, 1w). Leave empty for permanent ban')
                .setRequired(false)
        )
        .addIntegerOption(option =>
            option
                .setName('delete_messages')
                .setDescription('Number of days of message history to delete (0-7)')
                .setRequired(false)
                .setMinValue(0)
                .setMaxValue(7)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.BanMembers);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('BanCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const targetUser = interaction.options.getUser('user');
            const reason = interaction.options.getString('reason') || 'No reason provided';
            const duration = interaction.options.getString('duration');
            const deleteMessageDays = interaction.options.getInteger('delete_messages') || 0;
            const guild = interaction.guild;

            if (!guild) {
                await interaction.editReply(' This command can only be used in a server.');
                return;
            }

            if (!targetUser) {
                await interaction.editReply(' Please specify a valid user to ban.');
                return;
            }

            let banDuration: number | null = null;
            let durationText = 'Permanent';

            if (duration) {
                const parsedDuration = this.parseDuration(duration);
                if (parsedDuration === null) {
                    await interaction.editReply(' Invalid duration format. Use formats like: 7d, 24h, 30m, 1w, 2mo, 1y');
                    return;
                }
                banDuration = parsedDuration;
                durationText = this.formatDuration(parsedDuration);
            }

            if (targetUser.id === interaction.user.id) {
                await interaction.editReply(' You cannot ban yourself.');
                return;
            }

            if (targetUser.id === interaction.client.user?.id) {
                await interaction.editReply(' I cannot ban myself.');
                return;
            }

            const targetMember = await guild.members.fetch(targetUser.id).catch(() => null);

            if (!targetMember) {
                await this.banUserNotInServer(interaction, targetUser, reason, deleteMessageDays, banDuration, durationText);
                return;
            }

            if (!targetMember.bannable) {
                await interaction.editReply(' I cannot ban this user. They may have higher permissions than me.');
                return;
            }

            const commandMember = interaction.member as GuildMember;
            if (targetMember.roles.highest.position >= commandMember.roles.highest.position) {
                await interaction.editReply(' You cannot ban this user. They have equal or higher permissions than you.');
                return;
            }

            try {
                const notificationEmbed = new EmbedBuilder()
                    .setTitle(banDuration ? '⏰ You have been temporarily banned' : '🔨 You have been banned')
                    .setDescription(`You have been ${banDuration ? 'temporarily banned' : 'banned'} from **${guild.name}**.`)
                    .setColor(Colors.DarkRed)
                    .setThumbnail(guild.iconURL({ size: 256 }))
                    .addFields(
                        {
                            name: '🛡️ Moderator',
                            value: `**${interaction.user.tag}**`,
                            inline: true
                        },
                        {
                            name: '📅 Date & Time',
                            value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                            inline: true
                        },
                        {
                            name: '⏱️ Duration',
                            value: durationText,
                            inline: true
                        },
                        {
                            name: '📝 Reason',
                            value: reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*',
                            inline: false
                        }
                    )
                    .setTimestamp()
                    .setFooter({
                        text: `PoloCloud Discord Bot • Ban Notification`,
                        iconURL: interaction.client.user?.displayAvatarURL()
                    });

                await targetUser.send({ embeds: [notificationEmbed] }).catch(() => {
                    this.logger.info(`Could not send ban notification to ${targetUser.tag} - DMs likely disabled`);
                });
            } catch (error) {
                this.logger.warn(`Could not send ban notification to ${targetUser.tag}:`, error);
            }

            await guild.members.ban(targetUser, {
                reason: `${interaction.user.tag}: ${reason}${banDuration ? ` (Duration: ${durationText})` : ''}`,
                deleteMessageDays: deleteMessageDays
            });

            if (banDuration) {
                setTimeout(async () => {
                    try {
                        await guild.members.unban(targetUser, `Automatic unban after ${durationText}`);
                        this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was automatically unbanned after ${durationText}`);
                    } catch (error) {
                        this.logger.error(`Failed to automatically unban ${targetUser.tag}:`, error);
                    }
                }, banDuration);
            }

            const embed = new EmbedBuilder()
                .setTitle(banDuration ? '⏰ User Successfully Temporarily Banned' : '🔨 User Successfully Banned')
                .setDescription(`**${targetUser.tag}** has been ${banDuration ? 'temporarily banned' : 'banned'} from the server.`)
                .setColor(Colors.DarkRed)
                .setThumbnail(targetUser.displayAvatarURL({ size: 256 }))
                .addFields(
                    {
                        name: '👤 Banned User',
                        value: `**${targetUser.tag}**\n\`${targetUser.id}\``,
                        inline: true
                    },
                    {
                        name: '🛡️ Moderator',
                        value: `**${interaction.user.tag}**\n\`${interaction.user.id}\``,
                        inline: true
                    },
                    {
                        name: '📅 Date & Time',
                        value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                        inline: true
                    },
                    {
                        name: '⏱️ Duration',
                        value: durationText,
                        inline: true
                    },
                    {
                        name: '🗑️ Message Deletion',
                        value: deleteMessageDays > 0 ? `\`${deleteMessageDays} days\`` : '`No messages deleted`',
                        inline: true
                    },
                    {
                        name: '📝 Reason',
                        value: reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*',
                        inline: false
                    }
                )
                .setTimestamp()
                .setFooter({
                    text: `PoloCloud Discord Bot • Ban Action`,
                    iconURL: interaction.client.user?.displayAvatarURL()
                });

            await interaction.editReply({ embeds: [embed] });

            this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was ${banDuration ? 'temporarily banned' : 'banned'} by ${interaction.user.tag} (${interaction.user.id}) for reason: ${reason}${banDuration ? ` (Duration: ${durationText})` : ''}`);

        } catch (error) {
            this.logger.error('Error executing Ban command:', error);

            if (error instanceof Error && error.message.includes('Missing Permissions')) {
                await interaction.editReply(' I do not have permission to ban users in this server.');
            } else {
                await interaction.editReply(' An error occurred while trying to ban the user. Please try again later.');
            }
        }
    }

    private parseDuration(duration: string): number | null {
        const match = duration.match(/^(\d+)([dhmsyow])$/i);
        if (!match || !match[1] || !match[2]) return null;

        const value = parseInt(match[1]);
        const unit = match[2].toLowerCase();

        switch (unit) {
            case 's': return value * 1000; // seconds
            case 'm': return value * 60 * 1000; // minutes
            case 'h': return value * 60 * 60 * 1000; // hours
            case 'd': return value * 24 * 60 * 60 * 1000; // days
            case 'w': return value * 7 * 24 * 60 * 60 * 1000; // weeks
            case 'o': return value * 30 * 24 * 60 * 60 * 1000; // months (30 days)
            case 'y': return value * 365 * 24 * 60 * 60 * 1000; // years
            default: return null;
        }
    }

    private formatDuration(milliseconds: number): string {
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        const weeks = Math.floor(days / 7);
        const months = Math.floor(days / 30);
        const years = Math.floor(days / 365);

        if (years > 0) return `${years} year${years > 1 ? 's' : ''}`;
        if (months > 0) return `${months} month${months > 1 ? 's' : ''}`;
        if (weeks > 0) return `${weeks} week${weeks > 1 ? 's' : ''}`;
        if (days > 0) return `${days} day${days > 1 ? 's' : ''}`;
        if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''}`;
        if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''}`;
        return `${seconds} second${seconds > 1 ? 's' : ''}`;
    }

    private async banUserNotInServer(interaction: ChatInputCommandInteraction, targetUser: any, reason: string, deleteMessageDays: number, banDuration: number | null, durationText: string): Promise<void> {
        try {
            const guild = interaction.guild;
            if (!guild) {
                await interaction.editReply(' This command can only be used in a server.');
                return;
            }

            await guild.members.ban(targetUser, {
                reason: `${interaction.user.tag}: ${reason}${banDuration ? ` (Duration: ${durationText})` : ''}`,
                deleteMessageDays: deleteMessageDays
            });

            if (banDuration) {
                setTimeout(async () => {
                    try {
                        await guild.members.unban(targetUser, `Automatic unban after ${durationText}`);
                        this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was automatically unbanned after ${durationText}`);
                    } catch (error) {
                        this.logger.error(`Failed to automatically unban ${targetUser.tag}:`, error);
                    }
                }, banDuration);
            }

            const embed = new EmbedBuilder()
                .setTitle(banDuration ? '⏰ User Successfully Temporarily Banned' : '🔨 User Successfully Banned')
                .setDescription(`**${targetUser.tag}** has been ${banDuration ? 'temporarily banned' : 'banned'} from the server.`)
                .setColor(Colors.DarkRed)
                .setThumbnail(targetUser.displayAvatarURL({ size: 256 }))
                .addFields(
                    {
                        name: '👤 Banned User',
                        value: `**${targetUser.tag}**\n\`${targetUser.id}\``,
                        inline: true
                    },
                    {
                        name: '🛡️ Moderator',
                        value: `**${interaction.user.tag}**\n\`${interaction.user.id}\``,
                        inline: true
                    },
                    {
                        name: '📅 Date & Time',
                        value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                        inline: true
                    },
                    {
                        name: '⏱️ Duration',
                        value: durationText,
                        inline: true
                    },
                    {
                        name: 'ℹ️ Status',
                        value: '`User was not in server`',
                        inline: true
                    },
                    {
                        name: '📝 Reason',
                        value: reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*',
                        inline: false
                    }
                )
                .setTimestamp()
                .setFooter({
                    text: `PoloCloud Discord Bot • Ban Action`,
                    iconURL: interaction.client.user?.displayAvatarURL()
                });

            await interaction.editReply({ embeds: [embed] });

            this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was ${banDuration ? 'temporarily banned' : 'banned'} by ${interaction.user.tag} (${interaction.user.id}) for reason: ${reason} (User was not in server)${banDuration ? ` (Duration: ${durationText})` : ''}`);

        } catch (error) {
            this.logger.error('Error banning user not in server:', error);
            await interaction.editReply(' An error occurred while trying to ban the user. Please try again later.');
        }
    }
}