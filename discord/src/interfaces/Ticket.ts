export interface TicketData {
    id: string;
    userId: string;
    category: string;
    subject: string;
    description: string;
    status: 'open' | 'closed';
    createdAt: number;
    closedAt?: number;
    closedBy?: string;
} 