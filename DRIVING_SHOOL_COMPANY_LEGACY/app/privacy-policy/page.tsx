// app/privacy-policy/page.tsx
"use client"

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Loader2, Calendar, Smartphone, Globe, FileText, Download } from "lucide-react";
import { toast } from "sonner";
import MDEditor from '@uiw/react-md-editor';
import { useSearchParams } from "next/navigation";

interface PrivacyPolicy {
  id: number;
  version: string;
  title: string;
  content: string;
  appVersion: string;
  language: string;
  createdAt: string;
  updatedAt: string;
}

export default function PrivacyPolicyPage() {
  const [policy, setPolicy] = useState<PrivacyPolicy | null>(null);
  const [availableVersions, setAvailableVersions] = useState<PrivacyPolicy[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const searchParams = useSearchParams();
  const queryLanguage = searchParams.get("language") || "en";
  const [selectedLanguage, setSelectedLanguage] = useState<string>(queryLanguage);

  useEffect(() => {
    fetchPolicy();
    fetchAvailableVersions();
  }, [selectedLanguage]);

  const fetchPolicy = async () => {
    try {
      setIsLoading(true);
      const url = `/api/privacy-policy?language=${selectedLanguage}`;
      
      const data = await fetch(url).then(res => res.json());
      
      if (data.error) {
        toast.error(data.error);
        setPolicy(null);
      } else {
        setPolicy(data.data);
      }
    } catch (error) {
      toast.error("Failed to load privacy policy");
      console.error("Error fetching privacy policy:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchAvailableVersions = async () => {
    try {
      // This would be a new endpoint to get all available versions
      const data = await fetch(`/api/privacy-policy/versions?language=${selectedLanguage}`).then(res => res.json());
      if (data.data) {
        setAvailableVersions(data.data);
      }
    } catch (error) {
      console.error("Error fetching available versions:", error);
    }
  };

  const downloadAsPDF = () => {
    if (!policy) return;
    
    const element = document.createElement("a");
    const file = new Blob([`
      Privacy Policy - ${policy.title}
      Version: ${policy.version}
      App Version: ${policy.appVersion}
      Last Updated: ${new Date(policy.updatedAt).toLocaleDateString()}
      
      ${policy.content.replace(/#/g, '').replace(/\*\*(.*?)\*\*/g, '$1').replace(/\*(.*?)\*/g, '$1')}
    `], { type: "text/plain" });
    
    element.href = URL.createObjectURL(file);
    element.download = `privacy-policy-v${policy.version}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    
    toast.success("Privacy policy downloaded");
  };

  const getLanguageName = (code: string) => {
    const languages: { [key: string]: string } = {
      en: "English",
      fr: "French",
      rw: "Kinyarwanda"
    };
    return languages[code] || code;
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading privacy policy...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <div className="border-b">
        <div className="container mx-auto px-4 py-6">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Privacy Policy</h1>
              <p className="text-muted-foreground mt-2">
                Understand how we collect, use, and protect your personal information
              </p>
            </div>
            
            <div className="flex flex-col sm:flex-row gap-3">
              <Select value={selectedLanguage} onValueChange={setSelectedLanguage}>
                <SelectTrigger className="w-full sm:w-[180px]">
                  <div className="flex items-center gap-2">
                    <Globe className="h-4 w-4" />
                    <SelectValue placeholder="Language" />
                  </div>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="en">English</SelectItem>
                  <SelectItem value="fr">Français</SelectItem>
                  <SelectItem value="rw">Kinyarwanda</SelectItem>
                </SelectContent>
              </Select>
 

              <Button variant="outline" onClick={downloadAsPDF} disabled={!policy}>
                <Download className="h-4 w-4 mr-2" />
                Download
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Policy Content */}
      <div className="container mx-auto px-4 py-8">
        {policy ? (
          <div className="max-w-4xl mx-auto">
            {/* Policy Header */}
            <Card className="mb-8">
              <CardHeader className="text-center">
                <CardTitle className="text-2xl">{policy.title}</CardTitle>
                <CardDescription className="flex flex-wrap items-center justify-center gap-4 mt-4">
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <FileText className="h-4 w-4" />
                    Version {policy.version}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Smartphone className="h-4 w-4" />
                    App {policy.appVersion}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Globe className="h-4 w-4" />
                    {getLanguageName(policy.language)}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Calendar className="h-4 w-4" />
                    Updated {new Date(policy.updatedAt).toLocaleDateString()}
                  </Badge>
                </CardDescription>
              </CardHeader>
            </Card>

            {/* Policy Content */}
            <Card>
              <CardContent className="p-6">
                <div className="prose prose-lg max-w-none dark:prose-invert">
                  <MDEditor.Markdown source={policy.content} />
                </div>
              </CardContent>
            </Card>

            {/* Footer Note */}
            <div className="mt-8 text-center text-sm text-muted-foreground">
              <p>
                If you have any questions about our privacy policy, please contact us at{" "}
                <a href="mailto:privacy@yourapp.com" className="text-primary hover:underline">
                  privacy@yourapp.com
                </a>
              </p>
              <p className="mt-2">
                Last updated: {new Date(policy.updatedAt).toLocaleString()}
              </p>
            </div>
          </div>
        ) : (
          <div className="text-center py-16">
            <FileText className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
            <h2 className="text-2xl font-semibold mb-2">Privacy Policy Not Available</h2>
            <p className="text-muted-foreground">
              The requested privacy policy is not available in the selected language or version.
            </p>
            <Button 
              onClick={() => {
                setSelectedLanguage("en");
              }}
              className="mt-4"
            >
              Load Latest English Version
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}