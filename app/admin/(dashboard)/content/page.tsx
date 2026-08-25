"use client"
import { Loader2 } from "lucide-react";
import { redirect } from "next/navigation";
import { useEffect, useState } from "react";

export default function RedirectPage() {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false);
      redirect("/admin/content/materials");
    }, 500);

    return () => clearTimeout(timer);
  }, []);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="animate-spin" />
      </div>
    );
  }

  return null;
}
