'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { 
  ArrowLeft, 
  ArrowRight, 
  Check, 
  FileText, 
  Settings, 
  Edit3, 
  Sparkles,
  Calendar,
  User,
  Tags
} from 'lucide-react';
import { useRouter } from 'next/navigation';

const MDXEditor = dynamic(
  () => import('@uiw/react-md-editor').then(mod => mod.default),
  { ssr: false }
);

interface BlogFormData {
  title: string;
  description: string;
  author: string;
  tags: string;
  pinned: boolean;
  content: string;
}

export default function CreateBlogPage() {
  const router = useRouter();
  const [creating, setCreating] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);
  
  const [formData, setFormData] = useState<BlogFormData>({
    title: '',
    description: '',
    author: '',
    tags: '',
    pinned: false,
    content: '# Blog Post Title\n\nWrite your content here...',
  });

  const steps = [
    { id: 1, title: 'Basic Info', icon: FileText, description: 'Title and basic information' },
    { id: 2, title: 'Details', icon: Settings, description: 'Description and metadata' },
    { id: 3, title: 'Content', icon: Edit3, description: 'Write your blog content' },
    { id: 4, title: 'Review', icon: Check, description: 'Review and publish' },
  ];

  const nextStep = () => {
    if (currentStep < steps.length) {
      setCurrentStep(currentStep + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const canProceed = () => {
    switch (currentStep) {
      case 1:
        return formData.title.trim().length > 0;
      case 2:
        return formData.description.trim().length > 0;
      case 3:
        return formData.content.trim().length > 0;
      case 4:
        return true;
      default:
        return false;
    }
  };

  const handleSubmit = async () => {
    if (!formData.title.trim() || !formData.content.trim()) return;

    setCreating(true);
    
    try {
      const response = await fetch('/api/admin/blog/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          title: formData.title.trim(),
          description: formData.description.trim(),
          author: formData.author.trim() || 'PoloCloud Team',
          tags: formData.tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0),
          pinned: formData.pinned,
          content: formData.content.trim(),
        }),
      });

      if (response.ok) {
        router.push('/admin/blog');
      } else {
        const errorData = await response.json();
        alert('Failed to create blog post: ' + errorData.error);
      }
    } catch (error) {
      console.error('Error creating blog post:', error);
      alert('Failed to create blog post');
    } finally {
      setCreating(false);
    }
  };

  const renderStepContent = () => {
    const stepVariants = {
      initial: { opacity: 0, y: 20 },
      animate: { opacity: 1, y: 0 },
      exit: { opacity: 0, y: -20 }
    };

    switch (currentStep) {
      case 1:
        return (
          <motion.div
            key="step1"
            variants={stepVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.3 }}
            className="space-y-8"
          >
            <div className="text-center mb-8">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring" }}
              >
                <FileText className="w-20 h-20 mx-auto mb-6 text-primary" />
              </motion.div>
              <h3 className="text-3xl font-bold mb-3">Enter Blog Title</h3>
              <p className="text-muted-foreground text-lg">What should your new blog post be called?</p>
            </div>
            
            <div className="max-w-md mx-auto">
              <label className="block text-sm font-medium mb-3">Blog Title *</label>
              <Input
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="e.g. PoloCloud 2.0 Release"
                className="text-lg py-3 px-4"
                autoFocus
              />
            </div>
          </motion.div>
        );

      case 2:
        return (
          <motion.div
            key="step2"
            variants={stepVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.3 }}
            className="space-y-8"
          >
            <div className="text-center mb-8">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring" }}
              >
                <Settings className="w-20 h-20 mx-auto mb-6 text-primary" />
              </motion.div>
              <h3 className="text-3xl font-bold mb-3">Add Details</h3>
              <p className="text-muted-foreground text-lg">Description and additional information</p>
            </div>
            
            <div className="max-w-lg mx-auto space-y-6">
              <div>
                <label className="block text-sm font-medium mb-2">Description *</label>
                <Input
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Brief description of the blog post"
                  className="py-3"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Author</label>
                  <Input
                    value={formData.author}
                    onChange={(e) => setFormData({ ...formData, author: e.target.value })}
                    placeholder="PoloCloud Team"
                    className="py-3"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-2">Tags</label>
                  <Input
                    value={formData.tags}
                    onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                    placeholder="tag1, tag2, tag3"
                    className="py-3"
                  />
                </div>
              </div>

              <motion.div 
                className="flex items-center space-x-3 p-4 bg-muted/50 rounded-lg"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <input
                  type="checkbox"
                  id="pinned"
                  checked={formData.pinned}
                  onChange={(e) => setFormData({ ...formData, pinned: e.target.checked })}
                  className="rounded border-border w-5 h-5"
                />
                <label htmlFor="pinned" className="text-sm font-medium cursor-pointer">
                  Pin this post (appears at the top of the list)
                </label>
              </motion.div>
            </div>
          </motion.div>
        );

      case 3:
        return (
          <motion.div
            key="step3"
            variants={stepVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.3 }}
            className="space-y-8"
          >
            <div className="text-center mb-8">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring" }}
              >
                <Edit3 className="w-20 h-20 mx-auto mb-6 text-primary" />
              </motion.div>
              <h3 className="text-3xl font-bold mb-3">Create Content</h3>
              <p className="text-muted-foreground text-lg">Write the content of your blog post</p>
            </div>
            
            <div className="max-w-5xl mx-auto">
              <label className="block text-sm font-medium mb-3">Blog Content *</label>
              <div className="border rounded-lg overflow-hidden shadow-lg">
                <MDXEditor
                  value={formData.content}
                  onChange={(val) => setFormData({ ...formData, content: val || '' })}
                  height={500}
                  data-color-mode="light"
                />
              </div>
            </div>
          </motion.div>
        );

      case 4:
        const wordCount = formData.content.split(' ').filter(word => word.trim().length > 0).length;
        const tagsArray = formData.tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
        
        return (
          <motion.div
            key="step4"
            variants={stepVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.3 }}
            className="space-y-8"
          >
            <div className="text-center mb-8">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring" }}
              >
                <Sparkles className="w-20 h-20 mx-auto mb-6 text-primary" />
              </motion.div>
              <h3 className="text-3xl font-bold mb-3">Ready to Publish</h3>
              <p className="text-muted-foreground text-lg">Review your information and publish the blog post</p>
            </div>
            
            <div className="max-w-2xl mx-auto">
              <motion.div 
                className="bg-gradient-to-br from-muted/50 to-muted/20 rounded-xl p-8 space-y-6 border shadow-lg"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
              >
                <div>
                  <h4 className="font-bold text-2xl mb-2">{formData.title}</h4>
                  {formData.description && (
                    <p className="text-muted-foreground text-lg">{formData.description}</p>
                  )}
                </div>
                
                <div className="flex flex-wrap gap-3">
                  <Badge variant="outline" className="px-3 py-1">
                    <User className="w-3 h-3 mr-1" />
                    {formData.author || 'PoloCloud Team'}
                  </Badge>
                  {formData.pinned && (
                    <Badge variant="secondary" className="px-3 py-1">
                      <Sparkles className="w-3 h-3 mr-1" />
                      Pinned
                    </Badge>
                  )}
                  {tagsArray.map((tag) => (
                    <Badge key={tag} variant="outline" className="px-2 py-1">
                      <Tags className="w-3 h-3 mr-1" />
                      {tag}
                    </Badge>
                  ))}
                </div>
                
                <div className="text-sm text-muted-foreground border-t pt-4">
                  <div className="flex items-center gap-4">
                    <span className="flex items-center gap-1">
                      <FileText className="w-4 h-4" />
                      {wordCount} words
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-4 h-4" />
                      {new Date().toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </motion.div>
            </div>
          </motion.div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="relative min-h-screen">
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">

        <div className="text-center mb-16">
          <div className="flex items-center justify-center gap-3 mb-6">
            <div className="flex items-center justify-center w-10 h-10 bg-primary/10 text-primary rounded-full border border-primary/20">
              <Edit3 className="w-5 h-5" />
            </div>
          </div>
          <h1 className="text-3xl md:text-4xl lg:text-5xl font-black bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent mb-6 leading-tight">
            Create New Blog Post
          </h1>
          <p className="text-base md:text-lg text-muted-foreground max-w-3xl mx-auto leading-relaxed">
            Follow the steps to create and publish a new blog post for PoloCloud
          </p>
        </div>

        <div className="bg-card/50 backdrop-blur-sm border border-border/50 rounded-xl p-8 mb-8 shadow-lg">
          <div className="relative mb-8">
            <div className="absolute top-6 left-6 right-6 h-0.5 bg-border/40"></div>
            
            <motion.div 
              className="absolute top-6 left-6 h-0.5 bg-primary"
              initial={{ width: 0 }}
              animate={{ 
                width: ((currentStep - 1) / (steps.length - 1)) * 100 + '%'
              }}
              transition={{ duration: 0.5, ease: "easeInOut" }}
            />
            
            <div className="flex items-center justify-between relative">
              {steps.map((step, index) => (
                <div key={step.id} className="flex flex-col items-center">
                  <motion.div 
                    className={'w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all duration-300 z-10 ' + 
                      (currentStep >= step.id 
                        ? 'bg-primary border-primary text-primary-foreground shadow-lg' 
                        : currentStep === step.id
                        ? 'bg-background border-primary text-primary shadow-lg'
                        : 'bg-background border-border text-muted-foreground')}
                    whileHover={{ scale: 1.1 }}
                    animate={{ 
                      scale: currentStep === step.id ? 1.1 : 1,
                    }}
                  >
                    {currentStep > step.id ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", delay: 0.1 }}
                      >
                        <Check className="w-5 h-5" />
                      </motion.div>
                    ) : (
                      <step.icon className="w-5 h-5" />
                    )}
                  </motion.div>
                  
                  <div className="text-center mt-3">
                    <p className={'text-sm font-medium transition-colors ' + 
                      (currentStep >= step.id ? 'text-foreground' : 'text-muted-foreground')}>
                      {step.title}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="text-center">
            <p className="font-semibold text-lg">{steps[currentStep - 1]?.title}</p>
            <p className="text-sm text-muted-foreground">{steps[currentStep - 1]?.description}</p>
          </div>
        </div>

        <div className="bg-card/50 backdrop-blur-sm border border-border/50 rounded-xl p-8 mb-8 shadow-lg">
          <div className="min-h-[500px] flex items-center justify-center">
            <div className="w-full">
              <AnimatePresence mode="wait">
                {renderStepContent()}
              </AnimatePresence>
            </div>
          </div>
        </div>

        <div className="flex justify-between">
          <Button 
            variant="outline" 
            onClick={() => router.push('/admin/blog')}
            className="px-6"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Cancel
          </Button>
          
          <div className="flex gap-4">
            {currentStep > 1 && (
              <Button 
                variant="outline"
                onClick={prevStep}
                className="px-6"
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back
              </Button>
            )}
            
            {currentStep < steps.length ? (
              <Button 
                onClick={nextStep}
                disabled={!canProceed()}
                className="px-6"
              >
                Next
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            ) : (
              <motion.div
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                <Button 
                  onClick={handleSubmit}
                  disabled={creating || !canProceed()}
                  variant="secondary"
                  className="px-8"
                >
                  {creating ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Creating...
                    </>
                  ) : (
                    <>
                      Publish Blog Post
                      <Sparkles className="w-4 h-4 ml-2" />
                    </>
                  )}
                </Button>
              </motion.div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
