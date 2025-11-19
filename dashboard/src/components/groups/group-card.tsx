'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { motion } from 'framer-motion'
import Image from 'next/image';
import {
    HardDrive,
    Activity,
    Cpu,
    Users,
    Settings
} from 'lucide-react';
import { Group } from '@/types/groups';
import { getPlatformIcon } from '@/lib/platform-icons';

interface GroupCardProps {
    group: Group;
    index: number;
    onClick: () => void;
}

const getPlatformName = (group: Group): string => {
    if (typeof group.platform === 'string') {
        return group.platform;
    } else if (group.platform && typeof group.platform === 'object' && 'name' in group.platform) {
        return group.platform.name;
    }
    return 'default';
};

const getPlatformVersion = (group: Group): string => {
    if (typeof group.platform === 'string') {
        return 'Unknown';
    } else if (group.platform && typeof group.platform === 'object' && 'version' in group.platform) {
        return group.platform.version;
    }
    return 'Unknown';
};

export function GroupCard({ group, index, onClick }: GroupCardProps) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 40, scale: 0.85 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{
                duration: 0.8,
                delay: index * 0.1,
                ease: [0.34, 1.56, 0.64, 1]
            }}
        >
            <Card 
                className="border-border/40 flex flex-col relative bg-gradient-to-br from-card/80 via-card/60 to-card/80 shadow-lg transition-all duration-300 group cursor-pointer w-80 h-auto min-h-[420px]" 
                onClick={onClick}
            >
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(75.54%,15.34%,0.03)_0%,transparent_50%)] opacity-60 rounded-t-lg"/>
                
                <CardHeader className="pb-4 flex-shrink-0 relative z-10">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center gap-3">
                            <div className="p-2 rounded-xl bg-background/50 border border-border/30 transition-colors duration-200">
                                <Image
                                    src={getPlatformIcon(getPlatformName(group))}
                                    alt={getPlatformName(group)}
                                    width={32}
                                    height={32}
                                    className="w-8 h-8"
                                    onError={(e) => {
                                        e.currentTarget.src = '/placeholder.png';
                                    }}
                                />
                            </div>
                            <div className="flex-1 min-w-0">
                                <CardTitle className="text-lg font-bold text-foreground truncate transition-colors duration-200">
                                    {group.name}
                                </CardTitle>
                                <div className="flex items-center gap-2 mt-1">
                                    <p className="text-sm text-muted-foreground">
                                        {getPlatformName(group)}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <Badge
                        className="absolute top-4 right-4 text-xs h-6 px-3 transition-all duration-200 rounded-full bg-white/10 border border-white/20 text-white"
                    >
                        {getPlatformVersion(group)}
                    </Badge>
                </CardHeader>

                <CardContent className="flex-1 flex flex-col space-y-3 pt-0 relative z-10">
                    {}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="bg-gradient-to-r from-[oklch(75.54%_0.1534_231.639,0.1)] to-[oklch(75.54%_0.1534_231.639,0.05)] rounded-lg p-2.5 border border-[oklch(75.54%_0.1534_231.639,0.3)]">
                            <div className="flex items-center gap-2 mb-1.5">
                                <HardDrive className="w-3.5 h-3.5 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Memory</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground truncate" title={`${group.minMemory} - ${group.maxMemory} MB`}>
                                {group.minMemory} - {group.maxMemory} MB
                            </div>
                        </div>

                        <div className="bg-gradient-to-r from-[oklch(75.54%_0.1534_231.639,0.1)] to-[oklch(75.54%_0.1534_231.639,0.05)] rounded-lg p-2.5 border border-[oklch(75.54%_0.1534_231.639,0.3)]">
                            <div className="flex items-center gap-2 mb-1.5">
                                <Activity className="w-3.5 h-3.5 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Services</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground truncate" title={`${group.minOnlineService} - ${group.maxOnlineService}`}>
                                {group.minOnlineService} - {group.maxOnlineService}
                            </div>
                        </div>
                    </div>

                    {}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="bg-gradient-to-r from-[oklch(75.54%_0.1534_231.639,0.1)] to-[oklch(75.54%_0.1534_231.639,0.05)] rounded-lg p-2.5 border border-[oklch(75.54%_0.1534_231.639,0.3)]">
                            <div className="flex items-center gap-2 mb-1.5">
                                <Cpu className="w-3.5 h-3.5 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Start %</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground truncate" title={`${group.percentageToStartNewService}%`}>
                                {group.percentageToStartNewService}%
                            </div>
                        </div>

                        <div className="bg-gradient-to-r from-[oklch(75.54%_0.1534_231.639,0.1)] to-[oklch(75.54%_0.1534_231.639,0.05)] rounded-lg p-2.5 border border-[oklch(75.54%_0.1534_231.639,0.3)]">
                            <div className="flex items-center gap-2 mb-1.5">
                                <Users className="w-3.5 h-3.5 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Templates</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground">
                                {group.templates?.length || 0}
                            </div>
                        </div>
                    </div>

                    {}
                    <div className="bg-gradient-to-r from-[oklch(75.54%_0.1534_231.639,0.1)] to-[oklch(75.54%_0.1534_231.639,0.05)] rounded-lg p-2.5 border border-[oklch(75.54%_0.1534_231.639,0.3)]">
                        <div className="flex items-center gap-2 mb-1.5">
                            <Settings className="w-3.5 h-3.5 text-muted-foreground" />
                            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Templates</span>
                        </div>
                        <div className="flex flex-wrap gap-1">
                            {group.templates && group.templates.length > 0 ? (
                                group.templates
                                    .filter((template: any) => {
                                        if (typeof template === 'string') {
                                            return template.trim().length > 0;
                                        }
                                        if (typeof template === 'object' && template !== null) {
                                            return template.name && typeof template.name === 'string' && template.name.trim().length > 0;
                                        }
                                        return false;
                                    })
                                    .map((template: any, idx: number) => {
                                        const templateName = typeof template === 'string'
                                            ? template 
                                            : (template.name || '');
                                        
                                        return (
                                            <Badge 
                                                key={idx} 
                                                variant="secondary" 
                                                className="text-xs px-2 py-1 h-5 text-muted-foreground bg-background/50 border border-border/30"
                                            >
                                                {templateName}
                                            </Badge>
                                        );
                                    })
                            ) : (
                                <span className="text-xs text-muted-foreground">No templates</span>
                            )}
                        </div>
                    </div>

                    {}
                    <div className="flex items-center gap-3 pt-2 min-h-[32px]">
                        <div className="flex items-center gap-2">
                            <Settings className="w-4 h-4 text-muted-foreground" />
                            <span className="text-sm font-medium text-muted-foreground">Properties</span>
                        </div>
                        
                        {group.properties?.staticService && (
                            <Badge 
                                variant="outline" 
                                className="text-xs px-2 py-1 border-green-500/50 text-green-500 bg-green-500/10"
                            >
                                Static: Yes
                            </Badge>
                        )}
                        
                        {group.properties?.fallback && (
                            <Badge 
                                variant="outline" 
                                className="text-xs px-2 py-1 border-blue-500/50 text-blue-500 bg-blue-500/10"
                            >
                                Fallback: Yes
                            </Badge>
                        )}
                    </div>
                </CardContent>
            </Card>
        </motion.div>
    );
}
