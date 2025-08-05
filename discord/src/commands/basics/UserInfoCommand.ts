import { ChatInputCommandInteraction, SlashCommandBuilder, ContainerBuilder, Colors, MessageFlags, GuildMember } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';
import { BOT_CONFIG } from '../../config/constants';

export class UserInfoCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('userinfo')
        .setDescription('Show detailed information about a user')
        .addUserOption(option =>
            option
                .setName('user')
                .setDescription('The user to get information about')
                .setRequired(false)
        );

    private logger: Logger;

    constructor() {
        this.logger = new Logger('UserInfoCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const guild = interaction.guild;
            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            const targetUser = interaction.options.getUser('user') || interaction.user;
            const targetMember = await guild.members.fetch(targetUser.id).catch(() => null);

            const container = this.createUserInfoContainer(targetUser, targetMember, guild);

            await interaction.editReply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`User info displayed for ${targetUser.tag} by ${interaction.user.tag} in guild ${guild.name}`);

        } catch (error) {
            this.logger.error('Error executing UserInfo command:', error);
            await interaction.editReply('An error occurred while fetching user information. Please try again.');
        }
    }

    private createUserInfoContainer(user: any, member: GuildMember | null, guild: any): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Blue);

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# 👤 User Information\n\n**${user.tag}**`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📋 Basic Information\n\n**Username:** ${user.username}\n**Display Name:** ${user.displayName || user.username}\n**User ID:** \`${user.id}\`\n**Bot:** ${user.bot ? 'Yes' : 'No'}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📅 Account Information\n\n**Created:** <t:${Math.floor(user.createdTimestamp / 1000)}:F>\n**Account Age:** <t:${Math.floor(user.createdTimestamp / 1000)}:R>`)
        );

        if (member) {
            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🏠 Server Information\n\n**Joined:** <t:${Math.floor(member.joinedTimestamp! / 1000)}:F>\n**Member Since:** <t:${Math.floor(member.joinedTimestamp! / 1000)}:R>\n**Nickname:** ${member.nickname || 'None'}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            const roles = member.roles.cache
                .filter(role => role.id !== guild.id)
                .sort((a, b) => b.position - a.position);

            const rolesList = roles.map(role => `<@&${role.id}>`).join(' ');

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🏷️ Roles (${roles.size})\n\n${rolesList || 'No roles'}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            const permissions = member.permissions.toArray();
            const keyPermissions = permissions.filter(perm => 
                ['Administrator', 'ManageGuild', 'ManageChannels', 'ManageMessages', 'BanMembers', 'KickMembers', 'ManageRoles'].includes(perm)
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🔑 Key Permissions\n\n${keyPermissions.length > 0 ? keyPermissions.map(perm => `\`${perm}\``).join(', ') : 'No key permissions'}`)
            );

            container.addSeparatorComponents(
                separator => separator
            );

            const status = member.presence?.status || 'offline';
            const statusEmoji = this.getStatusEmoji(status);
            const activities = member.presence?.activities || [];

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🟢 Status & Activity\n\n**Status:** ${statusEmoji} ${status.charAt(0).toUpperCase() + status.slice(1)}\n**Activities:** ${activities.length > 0 ? activities.map(activity => `\`${activity.name}\``).join(', ') : 'None'}`)
            );
        } else {
            container.addSeparatorComponents(
                separator => separator
            );

            container.addTextDisplayComponents(
                textDisplay => textDisplay
                    .setContent(`## 🏠 Server Information\n\n**Status:** Not a member of this server`)
            );
        }

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`**${BOT_CONFIG.NAME}** • ${new Date().toLocaleString('de-DE')}`)
        );

        return container;
    }

    private getStatusEmoji(status: string): string {
        switch (status) {
            case 'online': return '🟢';
            case 'idle': return '🟡';
            case 'dnd': return '🔴';
            case 'offline': return '⚫';
            default: return '⚫';
        }
    }
} 