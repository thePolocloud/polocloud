export const GITHUB_CONFIG = {
    REPO_URL: 'https://api.github.com/repos/HttpMarco/polocloud',
    WEBSITE_URL: 'https://polocloud.de',
    ISSUES_URL: 'https://github.com/HttpMarco/polocloud/issues',
    AVATAR_URL: 'https://github.com/HttpMarco/polocloud/blob/master/.img/img.png?raw=true',
    UPDATE_INTERVAL: 10 * 60 * 1000,
    TOP_LANGUAGES: 5
} as const;

export const BSTATS_CONFIG = {
    VELOCITY_PLUGIN_ID: process.env['BSTATS_VELOCITY_PLUGIN_ID'] || '26763',
    BUNGEECORD_PLUGIN_ID: process.env['BSTATS_BUNGEECORD_PLUGIN_ID'] || '26764',
    BASE_URL: 'https://bstats.org/plugin',
    UPDATE_INTERVAL: 15 * 60 * 1000
} as const;

export const TICKET_CONFIG = {
    get CHANNEL_ID() { return process.env['TICKET_CHANNEL_ID'] || ''; },
    get CATEGORY_ID() { return process.env['TICKET_CATEGORY_ID'] || ''; },
    get ARCHIVE_CATEGORY_ID() { return process.env['TICKET_ARCHIVE_CATEGORY_ID'] || ''; },
    get SUPPORT_ROLE_ID() { return process.env['TICKET_SUPPORT_ROLE_ID'] || ''; },
    MAX_TICKETS_PER_USER: 3,
    TICKET_PREFIX: 'ticket-',
    CATEGORIES: [
        { label: 'General Support', value: 'general', emoji: '❓', description: 'General questions and support' },
        { label: 'Technical Issues', value: 'technical', emoji: '🔧', description: 'Technical problems and bugs' },
        { label: 'Bug Report', value: 'bug', emoji: '🐛', description: 'Report bugs and issues' }
    ]
} as const;

export const APPLY_CONFIG = {
    get CHANNEL_ID() { return process.env['APPLY_CHANNEL_ID'] || ''; },
    get CATEGORY_ID() { return process.env['APPLY_CATEGORY_ID'] || ''; },
    get ARCHIVE_CATEGORY_ID() { return process.env['APPLY_ARCHIVE_CATEGORY_ID'] || ''; },
    get STAFF_ROLE_ID() { return process.env['APPLY_STAFF_ROLE_ID'] || ''; },
    get PARTNER_EMOJI_ID() { return process.env['APPLY_PARTNER_EMOJI_ID'] || '🤝'; },
    get TRANSLATOR_EMOJI_ID() { return process.env['APPLY_TRANSLATOR_EMOJI_ID'] || '🌐'; },
    MAX_APPLICATIONS_PER_USER: 1,
    APPLICATION_PREFIX: 'apply-',
    CATEGORIES: [
        { label: 'Partner Application', value: 'partner', emoji: '🤝', description: 'Apply to become a partner' },
        { label: 'Translator Application', value: 'translator', emoji: '🌐', description: 'Apply to become a translator' }
    ]
} as const;


export const CONTRIBUTORS_CONFIG = {
    UPDATE_INTERVAL: 60 * 60 * 1000
} as const;

export const BOT_CONFIG = {
    NAME: 'PoloCloud Discord Bot',
    STATUS: 'PoloCloud Stats'
} as const;