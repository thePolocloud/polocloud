"use client";

import { useEffect, useMemo, useState } from 'react';
import { LogIn, LogOut, Shield, User, Star, Search, RefreshCcw, Check, X, Clock, AlertCircle, Edit3, FileText, Github, Users, Plus, Trash2, Crown, Handshake, ExternalLink, Image as ImageIcon } from 'lucide-react';
import { ToastContainer, showToast } from '@/components/ui/toast';

type MeResp = { 
  authenticated: boolean; 
  user?: { 
    id?: string;
    username: string;
    name?: string;
    avatar?: string;
    isGitHubAdmin?: boolean;
    loginTime?: string;
  } 
};

type Feedback = {
  id: string;
  userId: string;
  username: string;
  rating: number;
  description: string;
  createdAt: string;
  avatar: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvedBy?: string;
  approvedAt?: string;
  rejectedBy?: string;
  rejectedAt?: string;
};

export default function AdminPage() {
  const [loading, setLoading] = useState(true);
  const [auth, setAuth] = useState<MeResp | null>(null);

  const [feedbacks, setFeedbacks] = useState<Feedback[]>([]);
  const [loadingFeedbacks, setLoadingFeedbacks] = useState(false);
  const [query, setQuery] = useState('');
  const [ratingFilter, setRatingFilter] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'>('ALL');


  const [adminUsers, setAdminUsers] = useState<any[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [newUserGitHubUsername, setNewUserGitHubUsername] = useState('');
  const [addingUser, setAddingUser] = useState(false);

  const [partners, setPartners] = useState<any[]>([]);
  const [loadingPartners, setLoadingPartners] = useState(false);

  const [activeTab, setActiveTab] = useState<'feedback' | 'users' | 'partners' | 'blog'>('feedback');
  const [newPartner, setNewPartner] = useState({
    name: '',
    logo: '',
    website: '',
    description: ''
  });
  const [addingPartner, setAddingPartner] = useState(false);

  const filtered = useMemo(() => {
    return feedbacks.filter(fb => {
      const q = query.toLowerCase().trim();
      const matchesQuery = !q || fb.username.toLowerCase().includes(q) || fb.description.toLowerCase().includes(q);
      const matchesRating = ratingFilter == null || fb.rating === ratingFilter;
      const matchesStatus = statusFilter === 'ALL' || fb.status === statusFilter;
      return matchesQuery && matchesRating && matchesStatus;
    });
  }, [feedbacks, query, ratingFilter, statusFilter]);

  const statusCounts = useMemo(() => {
    return feedbacks.reduce((acc, fb) => {
      acc[fb.status] = (acc[fb.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
  }, [feedbacks]);

  const fetchMe = async () => {
    setLoading(true);
    try {
      const githubRes = await fetch('/api/auth/github-admin/me', { credentials: 'include' });
      if (githubRes.ok) {
        const githubData = await githubRes.json();
        if (githubData.authenticated) {
          setAuth(githubData);
          setLoading(false);
          return;
        }
      } else if (githubRes.status === 403) {

        const errorData = await githubRes.json();
        if (errorData.error === 'Access revoked') {
          showToast(errorData.message || 'Your admin access has been revoked.', 'error', 7000);
          setTimeout(() => {
            window.location.reload();
          }, 2000);
          setAuth({ authenticated: false });
          setLoading(false);
          return;
        }
      }


      const res = await fetch('/api/admin/me', { credentials: 'include' });
      const data = await res.json();
      setAuth(data);
    } catch (e) {
      setAuth({ authenticated: false });
    } finally {
      setLoading(false);
    }
  };

  const fetchFeedbacks = async () => {
    setLoadingFeedbacks(true);
    try {
      const res = await fetch('/api/admin/feedback', { credentials: 'include' });
      if (res.ok) {
        const data = await res.json();
        setFeedbacks(data.feedbacks || []);
      }
    } finally {
      setLoadingFeedbacks(false);
    }
  };

  useEffect(() => {
    (async () => {
      await fetchMe();
      await fetchFeedbacks();
      await fetchAdminUsers();
      await fetchPartners();
    })();


    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');
    if (error === 'unauthorized') {
      showToast('Access denied: You are not authorized to access this admin dashboard.', 'error', 7000);

      const url = new URL(window.location.href);
      url.searchParams.delete('error');
      window.history.replaceState({}, '', url.toString());
    }


    const sessionCheckInterval = setInterval(async () => {
      if (auth?.authenticated) {
        await fetchMe();
      }
    }, 30000);

    return () => clearInterval(sessionCheckInterval);
  }, [auth?.authenticated]);



  const handleLogout = async () => {
    if (auth?.user?.isGitHubAdmin) {
      await fetch('/api/auth/github-admin/logout', { method: 'POST', credentials: 'include' });
    } else {
      await fetch('/api/admin/logout', { method: 'POST', credentials: 'include' });
    }
    setFeedbacks([]);
    await fetchMe();
  };

  const handleGitHubLogin = () => {
    window.location.href = '/api/auth/github-admin';
  };

  const fetchAdminUsers = async () => {
    setLoadingUsers(true);
    try {
      const res = await fetch('/api/admin/github-users', { credentials: 'include' });
      if (res.ok) {
        const data = await res.json();
        setAdminUsers(data.users || []);
        setIsSuperAdmin(data.isSuperAdmin || false);
      }
    } catch (error) {
      console.error('Error fetching admin users:', error);
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleAddUser = async () => {
    if (!newUserGitHubUsername.trim()) return;
    
    setAddingUser(true);
    try {
      const res = await fetch('/api/admin/github-users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          githubUsername: newUserGitHubUsername.trim(),
        }),
      });

      if (res.ok) {
        setNewUserGitHubUsername('');
        await fetchAdminUsers();
        showToast('User added successfully!', 'success');
      } else {
        const error = await res.json();
        showToast(error.error || 'Failed to add user', 'error');
      }
    } catch (error) {
      console.error('Error adding user:', error);
      showToast('Failed to add user', 'error');
    } finally {
      setAddingUser(false);
    }
  };

  const handleRemoveUser = async (username: string) => {
    if (!confirm(`Remove ${username} from admin access?`)) return;

    try {
      const res = await fetch(`/api/admin/github-users?username=${username}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (res.ok) {
        await fetchAdminUsers();
        showToast(`User ${username} removed successfully! They will be logged out automatically.`, 'success');

        if (auth?.user?.username === username) {
          showToast('You removed yourself from admin access. Logging out...', 'warning', 5000);
          setTimeout(handleLogout, 2000);
        }
      } else {
        const error = await res.json();
        showToast(error.error || 'Failed to remove user', 'error');
      }
    } catch (error) {
      console.error('Error removing user:', error);
      showToast('Failed to remove user', 'error');
    }
  };

  const fetchPartners = async () => {
    setLoadingPartners(true);
    try {
      const res = await fetch('/api/admin/partners', {
        credentials: 'include',
      });
      if (res.ok) {
        const data = await res.json();
        setPartners(data.partners || []);
      } else {
        console.error('Failed to fetch partners');
      }
    } catch (error) {
      console.error('Error fetching partners:', error);
    } finally {
      setLoadingPartners(false);
    }
  };

  const handleAddPartner = async () => {
    if (!newPartner.name.trim() || !newPartner.logo.trim()) {
      showToast('Name and logo URL are required', 'error');
      return;
    }

    setAddingPartner(true);
    try {
      const res = await fetch('/api/admin/partners', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(newPartner),
      });

      if (res.ok) {
        const data = await res.json();
        showToast(data.message || 'Partner added successfully', 'success');
        setNewPartner({ name: '', logo: '', website: '', description: '' });
        await fetchPartners();
      } else {
        const error = await res.json();
        showToast(error.error || 'Failed to add partner', 'error');
      }
    } catch (error) {
      console.error('Error adding partner:', error);
      showToast('Failed to add partner', 'error');
    } finally {
      setAddingPartner(false);
    }
  };

  const handleRemovePartner = async (partnerId: string, partnerName: string) => {
    if (!confirm(`Are you sure you want to remove ${partnerName}?`)) return;

    try {
      const res = await fetch(`/api/admin/partners?id=${partnerId}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (res.ok) {
        const data = await res.json();
        showToast(data.message || 'Partner removed successfully', 'success');
        await fetchPartners();
      } else {
        const error = await res.json();
        showToast(error.error || 'Failed to remove partner', 'error');
      }
    } catch (error) {
      console.error('Error removing partner:', error);
      showToast('Failed to remove partner', 'error');
    }
  };

  const handleApprove = async (feedbackId: string) => {
    try {
      const res = await fetch('/api/admin/feedback/approve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ feedbackId }),
      });
      if (res.ok) {
        await fetchFeedbacks();
      }
    } catch (error) {
      console.error('Failed to approve feedback:', error);
    }
  };

  const handleReject = async (feedbackId: string) => {
    try {
      const res = await fetch('/api/admin/feedback/reject', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ feedbackId }),
      });
      if (res.ok) {
        await fetchFeedbacks();
      }
    } catch (error) {
      console.error('Failed to reject feedback:', error);
    }
  };

  const feedbackStatusCounts = useMemo(() => {
    return feedbacks.reduce((acc, feedback) => {
      acc[feedback.status] = (acc[feedback.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
  }, [feedbacks]);

  const filteredFeedbacks = useMemo(() => {
    return feedbacks.filter(feedback => {
      const matchesQuery = feedback.username.toLowerCase().includes(query.toLowerCase()) ||
                          feedback.description.toLowerCase().includes(query.toLowerCase());
      const matchesRating = ratingFilter === null || feedback.rating === ratingFilter;
      const matchesStatus = statusFilter === 'ALL' || feedback.status === statusFilter;
      
      return matchesQuery && matchesRating && matchesStatus;
    });
  }, [feedbacks, query, ratingFilter, statusFilter]);

  return (
    <div className="relative min-h-screen">
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">
      <div className="text-center mb-10">
        <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">Admin Dashboard</h1>
        <div className="w-20 h-1 bg-gradient-to-r from-transparent via-border/50 to-transparent rounded-full mx-auto mt-4" />
        

      </div>

      <div className="relative overflow-hidden rounded-xl border border-border/50 bg-gradient-to-b from-background/50 to-background/80 p-6">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5 opacity-20" />
        <div className="relative">
          {loading ? (
            <div className="text-center text-muted-foreground">Loading...</div>
          ) : auth?.authenticated ? (
            <div className="space-y-6">

              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  {auth.user?.isGitHubAdmin && auth.user?.avatar ? (
                    <img 
                      src={auth.user.avatar} 
                      alt={auth.user.username}
                      className="w-12 h-12 rounded-full border-2 border-primary/20"
                    />
                  ) : (
                    <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                      <Shield className="w-6 h-6 text-primary" />
                    </div>
                  )}
                  <div>
                    <div className="text-sm text-muted-foreground">
                      Signed in as {auth.user?.isGitHubAdmin && (
                        <span className="inline-flex items-center gap-1 text-primary">
                          <Github className="w-3 h-3" />
                          GitHub Admin
                        </span>
                      )}
                    </div>
                    <div className="text-foreground font-semibold">
                      {auth.user?.name || auth.user?.username}
                      {auth.user?.isGitHubAdmin && (
                        <span className="text-xs text-muted-foreground ml-2">
                          (@{auth.user.username})
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">

                  <button onClick={fetchFeedbacks} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-border/50 bg-background hover:bg-background/70 text-sm">
                    <RefreshCcw className="w-4 h-4" /> Refresh
                  </button>
                  <button onClick={handleLogout} className="inline-flex items-center gap-2 px-3 py-2 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 text-sm">
                    <LogOut className="w-4 h-4" /> Logout
                  </button>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 mb-8 p-1 bg-muted/30 rounded-lg border border-border/30">
                <button
                  onClick={() => setActiveTab('feedback')}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                    activeTab === 'feedback'
                      ? 'bg-primary text-primary-foreground shadow-lg'
                      : 'text-muted-foreground hover:text-foreground hover:bg-background/50'
                  }`}
                >
                  <Star className="w-4 h-4" />
                  Feedback ({feedbacks.length})
                </button>
                {isSuperAdmin && (
                  <button
                    onClick={() => setActiveTab('users')}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                      activeTab === 'users'
                        ? 'bg-primary text-primary-foreground shadow-lg'
                        : 'text-muted-foreground hover:text-foreground hover:bg-background/50'
                    }`}
                  >
                    <Users className="w-4 h-4" />
                    Users ({adminUsers.length})
                  </button>
                )}
                {isSuperAdmin && (
                  <button
                    onClick={() => setActiveTab('partners')}
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                      activeTab === 'partners'
                        ? 'bg-primary text-primary-foreground shadow-lg'
                        : 'text-muted-foreground hover:text-foreground hover:bg-background/50'
                    }`}
                  >
                    <Handshake className="w-4 h-4" />
                    Partners ({partners.length})
                  </button>
                )}
                <button
                  onClick={() => window.open('/admin/blog', '_blank')}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-muted-foreground hover:text-foreground hover:bg-background/50 transition-all duration-200"
                >
                  <FileText className="w-4 h-4" />
                  Blog Management
                </button>
              </div>

              {activeTab === 'feedback' && (
                <div>

                  <div className="grid md:grid-cols-3 gap-4 mb-6">
                    <div className="flex items-center gap-2 rounded-lg border border-border/50 bg-background/50 px-3 py-2">
                      <Search className="w-4 h-4 text-muted-foreground" />
                      <input
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="Search by user or description"
                        className="w-full bg-transparent outline-none text-foreground placeholder:text-muted-foreground"
                      />
                    </div>
                    <div className="flex gap-2 overflow-x-auto">
                      {[1,2,3,4,5].map(r => (
                        <button
                          key={r}
                          onClick={() => setRatingFilter(prev => prev === r ? null : r)}
                          className={`inline-flex items-center gap-1 px-3 py-2 rounded-lg border text-sm ${ratingFilter===r ? 'bg-primary text-primary-foreground border-primary' : 'bg-background/50 text-foreground border-border/50 hover:bg-background'}`}
                        >
                          <Star className="w-4 h-4 text-yellow-400" /> {r}
                        </button>
                      ))}
                    </div>
                    <div className="flex items-center">
                      <div className="text-sm text-muted-foreground">{filteredFeedbacks.length} results</div>
                    </div>
                  </div>

                  <div className="flex gap-2 mb-6 overflow-x-auto">
                {[
                  { key: 'ALL', label: 'All', icon: null, count: feedbacks.length },
                  { key: 'PENDING', label: 'Pending', icon: Clock, count: feedbackStatusCounts.PENDING || 0 },
                  { key: 'APPROVED', label: 'Approved', icon: Check, count: feedbackStatusCounts.APPROVED || 0 },
                  { key: 'REJECTED', label: 'Rejected', icon: X, count: feedbackStatusCounts.REJECTED || 0 },
                ].map(tab => {
                  const TabIcon = tab.icon;
                  return (
                    <button
                      key={tab.key}
                      onClick={() => setStatusFilter(tab.key as any)}
                      className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium ${
                        statusFilter === tab.key 
                          ? 'bg-primary text-primary-foreground border-primary' 
                          : 'bg-background/50 text-foreground border-border/50 hover:bg-background'
                      }`}
                    >
                      {TabIcon && <TabIcon className="w-4 h-4" />}
                      {tab.label}
                      <span className="bg-muted/50 text-xs px-2 py-0.5 rounded-full">{tab.count}</span>
                    </button>
                  );
                })}
              </div>
                </div>
              )}

              {activeTab === 'users' && isSuperAdmin && (
                <div className="mb-6 p-6 bg-gradient-to-br from-amber-500/5 via-transparent to-amber-500/5 border border-amber-500/20 rounded-xl">
                  <div className="flex items-center gap-3 mb-4">
                    <Crown className="w-5 h-5 text-amber-500" />
                    <h3 className="text-lg font-semibold text-foreground">GitHub User Management</h3>
                    <span className="text-xs text-amber-600 bg-amber-500/10 px-2 py-1 rounded-full">Admin Only</span>
                  </div>

                  <div className="mb-4 p-4 bg-background/50 rounded-lg border border-border/50">
                    <h4 className="text-sm font-medium text-foreground mb-3">Add New GitHub Admin User</h4>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={newUserGitHubUsername}
                        onChange={(e) => setNewUserGitHubUsername(e.target.value)}
                        placeholder="GitHub username (e.g., username123)"
                        className="flex-1 px-3 py-2 bg-background border border-border/50 rounded-lg text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
                      />
                      <button
                        onClick={handleAddUser}
                        disabled={addingUser || !newUserGitHubUsername.trim()}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm transition-all duration-300"
                      >
                        <Plus className="w-4 h-4" />
                        {addingUser ? 'Adding...' : 'Add User'}
                      </button>
                    </div>
                    <p className="text-xs text-muted-foreground mt-2">
                      Enter the GitHub username of the person you want to give admin access. We'll validate the user exists on GitHub.
                    </p>
                  </div>

                  <div>
                    <h4 className="text-sm font-medium text-foreground mb-3">Current Admin Users ({adminUsers.length})</h4>
                    {loadingUsers ? (
                      <div className="text-center py-4 text-muted-foreground">Loading users...</div>
                    ) : adminUsers.length === 0 ? (
                      <div className="text-center py-4 text-muted-foreground">No admin users found.</div>
                    ) : (
                      <div className="space-y-2">
                        {adminUsers.map((user) => (
                          <div key={user.username} className="flex items-center justify-between p-3 bg-background/30 rounded-lg border border-border/30">
                            <div className="flex items-center gap-3">
                              {user.avatar ? (
                                <img 
                                  src={user.avatar} 
                                  alt={user.username}
                                  className="w-8 h-8 rounded-full border border-border/50"
                                />
                              ) : (
                                <div className="w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center">
                                  {user.role === 'SUPER_ADMIN' ? (
                                    <Crown className="w-4 h-4 text-amber-500" />
                                  ) : (
                                    <User className="w-4 h-4 text-primary" />
                                  )}
                                </div>
                              )}
                              <div>
                                <div className="font-medium text-foreground flex items-center gap-2">
                                  {user.name || user.username}
                                  {user.role === 'SUPER_ADMIN' && (
                                    <span className="text-xs bg-amber-500/10 text-amber-600 px-2 py-0.5 rounded-full">Admin</span>
                                  )}
                                  {user.role !== 'SUPER_ADMIN' && (
                                    <span className="text-xs bg-blue-500/10 text-blue-600 px-2 py-0.5 rounded-full">Team</span>
                                  )}
                                </div>
                                <div className="text-xs text-muted-foreground">
                                  @{user.username} • Added by {user.addedBy}
                                </div>
                              </div>
                            </div>
                            {user.role !== 'SUPER_ADMIN' && (
                              <button
                                onClick={() => handleRemoveUser(user.username)}
                                className="inline-flex items-center gap-1 px-2 py-1 bg-red-600/10 text-red-600 rounded-lg hover:bg-red-600/20 text-xs transition-all duration-300"
                              >
                                <Trash2 className="w-3 h-3" />
                                Remove
                              </button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {activeTab === 'partners' && isSuperAdmin && (
                <div className="mb-6 p-6 bg-gradient-to-br from-green-500/5 via-transparent to-green-500/5 border border-green-500/20 rounded-xl">
                  <div className="flex items-center gap-3 mb-4">
                    <Handshake className="w-5 h-5 text-green-500" />
                    <h3 className="text-lg font-semibold text-foreground">Partner Management</h3>
                    <button
                      onClick={fetchPartners}
                      className="ml-auto inline-flex items-center gap-2 px-3 py-1 bg-green-600/10 text-green-600 rounded-lg hover:bg-green-600/20 text-sm transition-all duration-300"
                    >
                      <RefreshCcw className="w-4 h-4" />
                      Refresh
                    </button>
                  </div>

                  <div className="mb-6 p-4 bg-background/30 rounded-lg border border-border/30">
                    <h4 className="font-medium text-foreground mb-3">Add New Partner</h4>
                    <div className="grid md:grid-cols-2 gap-4 mb-3">
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-1">Partner Name *</label>
                        <input
                          type="text"
                          value={newPartner.name}
                          onChange={(e) => setNewPartner({...newPartner, name: e.target.value})}
                          placeholder="e.g. HGLabor"
                          className="w-full px-3 py-2 bg-background border border-border/50 rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-green-500/50"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-1">Logo URL *</label>
                        <input
                          type="url"
                          value={newPartner.logo}
                          onChange={(e) => setNewPartner({...newPartner, logo: e.target.value})}
                          placeholder="https://example.com/logo.png"
                          className="w-full px-3 py-2 bg-background border border-border/50 rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-green-500/50"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-1">Website URL</label>
                        <input
                          type="url"
                          value={newPartner.website}
                          onChange={(e) => setNewPartner({...newPartner, website: e.target.value})}
                          placeholder="https://partner-website.com"
                          className="w-full px-3 py-2 bg-background border border-border/50 rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-green-500/50"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-foreground mb-1">Description</label>
                        <input
                          type="text"
                          value={newPartner.description}
                          onChange={(e) => setNewPartner({...newPartner, description: e.target.value})}
                          placeholder="Brief description of the partner"
                          className="w-full px-3 py-2 bg-background border border-border/50 rounded-lg text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-green-500/50"
                        />
                      </div>
                    </div>
                    <button
                      onClick={handleAddPartner}
                      disabled={addingPartner || !newPartner.name.trim() || !newPartner.logo.trim()}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300"
                    >
                      {addingPartner ? (
                        <>
                          <RefreshCcw className="w-4 h-4 animate-spin" />
                          Adding...
                        </>
                      ) : (
                        <>
                          <Plus className="w-4 h-4" />
                          Add Partner
                        </>
                      )}
                    </button>
                  </div>

                  <div>
                    <h4 className="font-medium text-foreground mb-3">Current Partners ({partners.length})</h4>
                    {loadingPartners ? (
                      <div className="text-center text-muted-foreground py-4">Loading partners...</div>
                    ) : partners.length === 0 ? (
                      <div className="text-center text-muted-foreground py-4">No partners added yet.</div>
                    ) : (
                      <div className="grid gap-3">
                        {partners.map((partner) => (
                          <div key={partner.id} className="flex items-center gap-4 p-4 bg-background/30 rounded-lg border border-border/30">
                            <div className="flex-shrink-0">
                              {partner.logo ? (
                                <img 
                                  src={partner.logo} 
                                  alt={partner.name}
                                  className="w-12 h-12 rounded-lg object-contain bg-background border border-border/50"
                                  onError={(e) => {
                                    e.currentTarget.style.display = 'none';
                                    e.currentTarget.nextElementSibling!.style.display = 'flex';
                                  }}
                                />
                              ) : null}
                              <div className="w-12 h-12 rounded-lg bg-green-500/10 flex items-center justify-center" style={{display: partner.logo ? 'none' : 'flex'}}>
                                <ImageIcon className="w-6 h-6 text-green-500" />
                              </div>
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <h5 className="font-medium text-foreground truncate">{partner.name}</h5>
                                {partner.website && (
                                  <a
                                    href={partner.website}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="inline-flex items-center gap-1 text-green-600 hover:text-green-700 text-sm"
                                  >
                                    <ExternalLink className="w-3 h-3" />
                                  </a>
                                )}
                              </div>
                              {partner.description && (
                                <div className="text-sm text-muted-foreground truncate">{partner.description}</div>
                              )}
                              <div className="text-xs text-muted-foreground">
                                Added {new Date(partner.addedAt).toLocaleDateString()} by {partner.addedBy}
                              </div>
                            </div>
                            <button
                              onClick={() => handleRemovePartner(partner.id, partner.name)}
                              className="inline-flex items-center gap-1 px-2 py-1 bg-red-600/10 text-red-600 rounded-lg hover:bg-red-600/20 text-xs transition-all duration-300"
                            >
                              <Trash2 className="w-3 h-3" />
                              Remove
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    {loadingFeedbacks && (
                      <div className="text-center text-muted-foreground">Loading feedbacks...</div>
                    )}
                    {!loadingFeedbacks && filteredFeedbacks.map((fb) => (
                  <div key={`${fb.userId}-${fb.createdAt}`} className="relative overflow-hidden rounded-xl border border-border/50 bg-gradient-to-b from-background/50 to-background/80 p-5">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        {fb.avatar ? (
                          <img 
                            src={fb.avatar} 
                            alt={fb.username}
                            className="w-10 h-10 rounded-full ring-2 ring-primary/20"
                          />
                        ) : (
                          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                            <User className="w-5 h-5 text-primary" />
                          </div>
                        )}
                        <div>
                          <div className="font-semibold text-foreground">{fb.username}</div>
                          <div className="text-xs text-muted-foreground">{new Date(fb.createdAt).toLocaleString()}</div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="inline-flex items-center gap-1 px-2 py-1 rounded bg-muted/40 text-sm">
                          {Array.from({ length: 5 }).map((_, i) => (
                            <span key={i} className={i < fb.rating ? 'text-yellow-400' : 'text-gray-300'}>★</span>
                          ))}
                        </div>
                        <div className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${
                          fb.status === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400' :
                          fb.status === 'APPROVED' ? 'bg-green-500/20 text-green-400' :
                          'bg-red-500/20 text-red-400'
                        }`}>
                          {fb.status === 'PENDING' && <Clock className="w-3 h-3" />}
                          {fb.status === 'APPROVED' && <Check className="w-3 h-3" />}
                          {fb.status === 'REJECTED' && <X className="w-3 h-3" />}
                          {fb.status}
                        </div>
                      </div>
                    </div>
                    
                    <div className="text-sm text-foreground/90 leading-relaxed mb-4">
                      {fb.description}
                    </div>

                    {fb.status === 'PENDING' && (
                      <div className="flex gap-2 pt-3 border-t border-border/30">
                        <button
                          onClick={() => handleApprove(fb.id)}
                          className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm"
                        >
                          <Check className="w-4 h-4" /> Approve
                        </button>
                        <button
                          onClick={() => handleReject(fb.id)}
                          className="flex-1 inline-flex items-center justify-center gap-2 px-3 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 text-sm"
                        >
                          <X className="w-4 h-4" /> Reject
                        </button>
                      </div>
                    )}

                    {fb.status === 'APPROVED' && fb.approvedBy && (
                      <div className="pt-3 border-t border-border/30 text-xs text-muted-foreground">
                        Approved by {fb.approvedBy} on {fb.approvedAt ? new Date(fb.approvedAt).toLocaleString() : 'Unknown'}
                      </div>
                    )}

                    {fb.status === 'REJECTED' && fb.rejectedBy && (
                      <div className="pt-3 border-t border-border/30 text-xs text-muted-foreground">
                        Rejected by {fb.rejectedBy} on {fb.rejectedAt ? new Date(fb.rejectedAt).toLocaleString() : 'Unknown'}
                      </div>
                    )}
                  </div>
                ))}
                {!loadingFeedbacks && filteredFeedbacks.length === 0 && (
                  <div className="text-center text-muted-foreground py-10">No feedbacks found.</div>
                )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-6 max-w-md mx-auto text-center">
              <div className="space-y-3">
                <h3 className="text-xl font-semibold text-foreground">Admin Access Required</h3>
                <p className="text-muted-foreground text-sm">
                  This admin dashboard is restricted to authorized GitHub users only.
                </p>
              </div>
              
              <button
                type="button"
                onClick={handleGitHubLogin}
                className="w-full inline-flex items-center justify-center gap-3 px-8 py-4 bg-[#24292e] hover:bg-[#1c2025] text-white rounded-xl font-semibold text-lg transition-all duration-300 shadow-lg hover:shadow-xl border border-gray-700"
              >
                <Github className="w-5 h-5" /> 
                Sign in with GitHub
              </button>
              
              <p className="text-xs text-muted-foreground">
                Only authorized GitHub accounts can access this dashboard.
              </p>
            </div>
          )}
                  </div>
        </div>
      </div>
      <ToastContainer />
    </div>
  );
}
