export interface ApplicationData {
    id: string;
    userId: string;
    category: string;
    subject: string;
    description: string;
    name: string;
    age: string;
    experience: string;
    motivation: string;
    additionalInfo: string;
    status: 'pending' | 'approved' | 'rejected';
    createdAt: number;
    reviewedBy?: string;
    reviewedAt?: number;
    reviewReason?: string;
} 