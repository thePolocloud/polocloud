import { ChatInputCommandInteraction, SlashCommandBuilder, Colors, PermissionFlagsBits, GuildMember, ContainerBuilder, MessageFlags } from 'discord.js';
import { Command } from '../../interfaces/Command';
import { Logger } from '../../utils/Logger';

export class MuteCommand implements Command {
    public data = new SlashCommandBuilder()
        .setName('mute')
        .setDescription('Mutes a user temporarily')
        .addUserOption(option =>
            option
                .setName('user')
                .setDescription('The user to mute')
                .setRequired(true)
        )
        .addStringOption(option =>
            option
                .setName('duration')
                .setDescription('Duration of the mute (e.g., 5m, 1h, 7d). Leave empty for permanent mute')
                .setRequired(false)
        )
        .addStringOption(option =>
            option
                .setName('reason')
                .setDescription('Reason for muting the user')
                .setRequired(false)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers);

    private logger: Logger;

    constructor() {
        this.logger = new Logger('MuteCommand');
    }

    public async execute(interaction: ChatInputCommandInteraction): Promise<void> {
        try {
            await interaction.deferReply({ ephemeral: true });

            const targetUser = interaction.options.getUser('user');
            const duration = interaction.options.getString('duration');
            const reason = interaction.options.getString('reason') || 'No reason provided';
            const guild = interaction.guild;

            if (!guild) {
                await interaction.editReply('This command can only be used in a server.');
                return;
            }

            if (!targetUser) {
                await interaction.editReply('Please specify a valid user to mute.');
                return;
            }

            let muteDuration: number | null = null;
            let durationText = 'Permanent';

            if (duration) {
                const parsedDuration = this.parseDuration(duration);
                if (parsedDuration === null) {
                    await interaction.editReply('Invalid duration format. Use formats like: 5m, 1h, 7d, 1w, 2mo, 1y');
                    return;
                }
                muteDuration = parsedDuration;
                durationText = this.formatDuration(parsedDuration);
            }

            if (targetUser.id === interaction.user.id) {
                await interaction.editReply('You cannot mute yourself.');
                return;
            }

            if (targetUser.id === interaction.client.user?.id) {
                await interaction.editReply('I cannot mute myself.');
                return;
            }

            const targetMember = await guild.members.fetch(targetUser.id).catch(() => null);

            if (!targetMember) {
                await interaction.editReply('The specified user is not a member of this server.');
                return;
            }

            if (!targetMember.moderatable) {
                await interaction.editReply('I cannot mute this user. They may have higher permissions than me.');
                return;
            }

            const commandMember = interaction.member as GuildMember;
            if (targetMember.roles.highest.position >= commandMember.roles.highest.position) {
                await interaction.editReply('You cannot mute this user. They have equal or higher permissions than you.');
                return;
            }

            if (targetMember.isCommunicationDisabled()) {
                await interaction.editReply('This user is already muted.');
                return;
            }

            try {
                const notificationContainer = this.createMuteNotificationContainer(targetUser, interaction, reason, durationText, guild);

                await targetUser.send({
                    components: [notificationContainer],
                    flags: MessageFlags.IsComponentsV2
                }).catch(() => {
                    this.logger.info(`Could not send mute notification to ${targetUser.tag} - DMs likely disabled`);
                });
            } catch (error) {
                this.logger.warn(`Could not send mute notification to ${targetUser.tag}:`, error);
            }

            await targetMember.timeout(muteDuration, `${interaction.user.tag}: ${reason}${muteDuration ? ` (Duration: ${durationText})` : ''}`);

            const container = this.createMuteSuccessContainer(targetUser, interaction, reason, durationText);

            await interaction.editReply({
                components: [container],
                flags: MessageFlags.IsComponentsV2
            });

            this.logger.info(`User ${targetUser.tag} (${targetUser.id}) was ${muteDuration ? 'temporarily muted' : 'permanently muted'} by ${interaction.user.tag} (${interaction.user.id}) for reason: ${reason}${muteDuration ? ` (Duration: ${durationText})` : ''}`);

        } catch (error) {
            this.logger.error('Error executing Mute command:', error);

            if (error instanceof Error && error.message.includes('Missing Permissions')) {
                await interaction.editReply('I do not have permission to mute users in this server.');
            } else {
                await interaction.editReply('An error occurred while trying to mute the user. Please try again later.');
            }
        }
    }

    private createMuteSuccessContainer(targetUser: any, interaction: any, reason: string, durationText: string): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Orange);

        const isTemporary = durationText !== 'Permanent';
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ${isTemporary ? '⏰' : '🔇'} User Successfully ${isTemporary ? 'Temporarily ' : ''}Muted\n\n**${targetUser.tag}** has been ${isTemporary ? 'temporarily muted' : 'permanently muted'} from the server.`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 👤 Muted User\n\n**${targetUser.tag}**\n\`${targetUser.id}\``)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 🛡️ Moderator\n\n**${interaction.user.tag}**\n\`${interaction.user.id}\``)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📅 Date & Time\n\n<t:${Math.floor(Date.now() / 1000)}:F>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## ⏱️ Duration\n\n${durationText}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📝 Reason\n\n${reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*'}`)
        );

        return container;
    }

    private parseDuration(duration: string): number | null {
        const match = duration.match(/^(\d+)([dhmsyow])$/i);
        if (!match || !match[1] || !match[2]) return null;

        const value = parseInt(match[1]);
        const unit = match[2].toLowerCase();

        switch (unit) {
            case 's': return value * 1000;
            case 'm': return value * 60 * 1000;
            case 'h': return value * 60 * 60 * 1000;
            case 'd': return value * 24 * 60 * 60 * 1000;
            case 'w': return value * 7 * 24 * 60 * 60 * 1000;
            case 'o': return value * 30 * 24 * 60 * 60 * 1000;
            case 'y': return value * 365 * 24 * 60 * 60 * 1000;
            default: return value * 1000;
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

    private createMuteNotificationContainer(targetUser: any, interaction: any, reason: string, durationText: string, guild: any): ContainerBuilder {
        const container = new ContainerBuilder()
            .setAccentColor(Colors.Orange);

        const isTemporary = durationText !== 'Permanent';
        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`# ${isTemporary ? '⏰' : '🔇'} Mute Notification\n\n**${targetUser.tag}** has been ${isTemporary ? 'temporarily muted' : 'permanently muted'} in **${guild.name}**.`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 🛡️ Moderator\n\n**${interaction.user.tag}**\n\`${interaction.user.id}\``)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📅 Date & Time\n\n<t:${Math.floor(Date.now() / 1000)}:F>`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## ⏱️ Duration\n\n${durationText}`)
        );

        container.addSeparatorComponents(
            separator => separator
        );

        container.addTextDisplayComponents(
            textDisplay => textDisplay
                .setContent(`## 📝 Reason\n\n${reason.length > 0 ? `\`\`\`${reason}\`\`\`` : '*No reason provided*'}`)
        );

        return container;
    }
}