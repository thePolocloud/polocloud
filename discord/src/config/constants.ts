export const GITHUB_CONFIG = {
    REPO_URL: 'https://api.github.com/repos/HttpMarco/polocloud',
    WEBSITE_URL: 'https://polocloud.de',
    ISSUES_URL: 'https://github.com/HttpMarco/polocloud/issues',
    AVATAR_URL: 'https://github.com/HttpMarco/polocloud/blob/master/.img/img.png?raw=true',
    UPDATE_INTERVAL: 10 * 60 * 1000, // 10 minutes in milliseconds
    TOP_LANGUAGES: 5
} as const;

export const BOT_CONFIG = {
    NAME: 'PoloCloud Discord Bot',
    STATUS: 'PoloCloud Stats'
} as const;