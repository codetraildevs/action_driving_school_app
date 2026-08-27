"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Users,
  FileText,
  CreditCard,
  Settings,
  LogOut,
  BookOpen,
  Logs,
  Text,
  ListChecks,
  FolderArchive,
  Menu,
  X,
  Activity,
  ShieldCheck,
  User2,
  Globe2,
  Group,
  Network,
} from "lucide-react";
import { useAuth } from "@/lib/auth/auth-context";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {  useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";

const navigation = [
  {
    name: "Dashboard",
    href: "/admin/dashboard",
    icon: LayoutDashboard,
  },
  {
    name: "Users",
    href: "/admin/users",
    icon: Users,
  },
  {
    name: "Content",
    href: "/admin/content",
    icon: FileText,
    submenu: [
      { name: "Learning Materials", href: "/admin/content/materials" },
      { name: "Tests", href: "/admin/content/tests" },
      { name: "Questions", href: "/admin/content/questions" },
    ],
  },
  {
    name: "File Manager",
    href: "/admin/file-manager",
    icon: FolderArchive,
  },
  // {
  //   name: "Subscriptions",
  //   href: "/admin/subscriptions",
  //   icon: CreditCard,
  // },
  {
    name: "Requests",
    href: "/admin/user-requests",
    icon: Logs,
  },
  {
    name: "Irembo Services",
    href: "/admin/requests",
    icon: Globe2,
  },
   {
    name: "Whatsapp  Groups",
    href: "/admin/whatsapp-groups",
    icon: Network,
  },
  {
    name: "Terms of Service",
    href: "/admin/terms-of-service",
    icon: ListChecks,
  },
  {
    name: "Privacy Policy",
    href: "/admin/privacy-policy",
    icon: ShieldCheck,
  },
  {
    name: "Profile",
    href: "/admin/settings",
    icon: User2,
  },
];
export function SidebarContent({ onLinkClick }: { onLinkClick?: () => void }) {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    onLinkClick?.();
  };

  return (
    <>
      <div className="flex h-14 items-center border-b px-4 lg:h-[60px] lg:px-6">
        <Link
          href="/admin/dashboard"
          className="flex items-center gap-2 font-semibold"
          onClick={onLinkClick}
        >
          <img src={"/logo.webp"} className="h-10 w-10" />
          <span>Action Driving School</span>
        </Link>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto p-4">
        {navigation.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <div key={item.name}>
              <Link
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
                onClick={onLinkClick}
              >
                <item.icon className="h-5 w-5" />
                {item.name}
              </Link>
              {item.submenu && isActive && (
                <div className="ml-8 mt-1 space-y-1">
                  {item.submenu.map((subitem) => {
                    const isSubActive = pathname === subitem.href;
                    return (
                      <Link
                        key={subitem.name}
                        href={subitem.href}
                        className={cn(
                          "block rounded-md px-3 py-1.5 text-sm transition-colors",
                          isSubActive
                            ? "text-primary font-medium"
                            : "text-muted-foreground hover:text-foreground"
                        )}
                        onClick={onLinkClick}
                      >
                        {subitem.name}
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* User Profile */}
      <div className="border-t p-4">
        <div className="flex items-center gap-3">
          <Avatar>
            <AvatarFallback>
              {user?.firstName?.[0]}
              {user?.lastName?.[0]}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
          </div>
        </div>
        <Button
          variant="ghost"
          className="w-full justify-start mt-2"
          onClick={handleLogout}
        >
          <LogOut className="mr-2 h-4 w-4" />
          Logout
        </Button>
      </div>
    </>
  );
}

export function Sidebar({open,onOpen}:{open:boolean,onOpen:(open:boolean)=>void}) {


  return (
    <div className="transition-all duration-300">
      {/* Mobile Menu Button */}
      <Sheet open={open} onOpenChange={onOpen}>
      <SheetContent side="left" className="p-0 w-72 transition-all duration-300">
        <div className="flex h-full flex-col">
        <SidebarContent onLinkClick={() => onOpen(!open)} />
        </div>
      </SheetContent>
      </Sheet>

      {/* Desktop Sidebar */}
      <div className="hidden border-r bg-muted/40 md:block transition-all duration-300">
      <div className="flex h-[99vh] max-h-screen flex-col gap-2">
        <SidebarContent />
      </div>
      </div>
    </div>
  );
}