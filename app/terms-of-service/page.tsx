// app/terms-of-service/page.tsx
"use client"

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Loader2, Calendar, Smartphone, Globe, FileText, Download, Scale } from "lucide-react";
import { toast } from "sonner";
import MDEditor from '@uiw/react-md-editor';
import { useSearchParams } from "next/navigation";

// Dynamically import the markdown renderer
// const MDEditor = dynamic(() => import('@uiw/react-md-editor'), { ssr: false });

interface TermsOfService {
  id: number;
  version: string;
  title: string;
  content: string;
  appVersion: string;
  language: string;
  createdAt: string;
  updatedAt: string;
}

export default function TermsOfServicePage() {
  const [terms, setTerms] = useState<TermsOfService | null>(null);
  const [availableVersions, setAvailableVersions] = useState<TermsOfService[]>([]);
  const [isLoading, setIsLoading] = useState(true);
    const searchParams = useSearchParams();
   const queryLanguage = searchParams.get("language") || "en";
  const [selectedLanguage, setSelectedLanguage] = useState<string>(queryLanguage);

  useEffect(() => {
    fetchTerms();
    fetchAvailableVersions();
  }, [selectedLanguage]);

  const fetchTerms = async () => {
    try {
      setIsLoading(true);
      const url = `/api/terms-of-service?language=${selectedLanguage}`;
      
      const data = await fetch(url).then(res => res.json());
      
      if (data.error) {
        toast.error(data.error);
        setTerms(null);
      } else {
        setTerms(data.data);
      }
    } catch (error) {
      toast.error("Failed to load terms of service");
      console.error("Error fetching terms of service:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchAvailableVersions = async () => {
    try {
      const data = await fetch(`/api/terms-of-service/versions?language=${selectedLanguage}`).then(res => res.json());
      if (data.data) {
        setAvailableVersions(data.data);
      }
    } catch (error) {
      console.error("Error fetching available versions:", error);
    }
  };

  const downloadAsPDF = () => {
    if (!terms) return;
    
    const element = document.createElement("a");
    const file = new Blob([`
      Terms of Service - ${terms.title}
      Version: ${terms.version}
      App Version: ${terms.appVersion}
      Last Updated: ${new Date(terms.updatedAt).toLocaleDateString()}
      
      ${terms.content.replace(/#/g, '').replace(/\*\*(.*?)\*\*/g, '$1').replace(/\*(.*?)\*/g, '$1')}
    `], { type: "text/plain" });
    
    element.href = URL.createObjectURL(file);
    element.download = `terms-of-service-v${terms.version}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    
    toast.success("Terms of service downloaded");
  };

  const getLanguageName = (code: string) => {
    const languages: { [key: string]: string } = {
      en: "English",
      fr: "French",
      es: "Spanish",
      de: "German",
      rw: "Kinyarwanda"
    };
    return languages[code] || code;
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading terms of service...</p>
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
              <h1 className="text-3xl font-bold tracking-tight">Terms of Service</h1>
              <p className="text-muted-foreground mt-2">
                Legal agreement between you and our service
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

         

              <Button variant="outline" onClick={downloadAsPDF} disabled={!terms}>
                <Download className="h-4 w-4 mr-2" />
                Download
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Terms Content */}
      <div className="container mx-auto px-4 py-8">
        {terms ? (
          <div className="max-w-4xl mx-auto">
            {/* Terms Header */}
            <Card className="mb-8">
              <CardHeader className="text-center">
                <CardTitle className="text-2xl flex items-center justify-center gap-2">
                  <Scale className="h-6 w-6" />
                  {terms.title}
                </CardTitle>
                <CardDescription className="flex flex-wrap items-center justify-center gap-4 mt-4">
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <FileText className="h-4 w-4" />
                    Version {terms.version}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Smartphone className="h-4 w-4" />
                    App {terms.appVersion}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Globe className="h-4 w-4" />
                    {getLanguageName(terms.language)}
                  </Badge>
                  <Badge variant="secondary" className="flex items-center gap-2">
                    <Calendar className="h-4 w-4" />
                    Updated {new Date(terms.updatedAt).toLocaleDateString()}
                  </Badge>
                </CardDescription>
              </CardHeader>
            </Card>

            {/* Important Notice */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
              <div className="flex items-start gap-3">
                <div className="bg-blue-100 p-2 rounded-full">
                  <Scale className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <h3 className="font-semibold text-blue-900 mb-1">Legal Notice</h3>
                  <p className="text-blue-800 text-sm">
                    Please read these terms carefully before using our service. By accessing or using our service, 
                    you agree to be bound by these terms. If you disagree with any part of the terms, you may not access the service.
                  </p>
                </div>
              </div>
            </div>

            {/* Terms Content */}
            <Card>
              <CardContent className="p-6">
                <div className="prose prose-lg max-w-none dark:prose-invert">
                  <MDEditor.Markdown source={terms.content} />
                </div>
              </CardContent>
            </Card>

            {/* Footer Note */}
            <div className="mt-8 text-center text-sm text-muted-foreground">
            
              <p className="mt-2">
                Last updated: {new Date(terms.updatedAt).toLocaleString()}
              </p>
            </div>
          </div>
        ) : (
          <div className="text-center py-16">
            <Scale className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
            <h2 className="text-2xl font-semibold mb-2">Terms of Service Not Available</h2>
            <p className="text-muted-foreground">
              The requested terms of service are not available in the selected language or version.
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