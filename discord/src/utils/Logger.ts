export class Logger {
    private context: string;

    constructor(context: string) {
        this.context = context;
    }

    private formatMessage(level: string, message: string): string {
        const timestamp = new Date().toISOString();
        return `[${timestamp}] [${level}] [${this.context}] ${message}`;
    }

    public info(message: string, ...args: unknown[]): void {
        console.log(this.formatMessage('INFO', message), ...args);
    }

    public warn(message: string, ...args: unknown[]): void {
        console.log(this.formatMessage('WARN', message), ...args);
    }

    public error(message: string, ...args: unknown[]): void {
        console.log(this.formatMessage('ERROR', message), ...args);
    }
}