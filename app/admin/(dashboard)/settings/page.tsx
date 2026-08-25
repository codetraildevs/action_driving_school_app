 
"use client";
import ProfileHeader from "@/components/profile-page/components/profile-header";
import ProfileContent from "@/components/profile-page/components/profile-content";
import { apiClient } from "@/lib/api-client";
import { useEffect, useState } from "react";
export interface UserProfile {
  id: string;
  firstName: string;
  middleName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  profilePicture: string;
  role: string;
  language: string;
  timezone: string;
  createdAt: string;
  userTestAccess: any;
  roleName: string;
}

export default function Page() {
  const[user, setUser]=useState<UserProfile | null>(null);


    const fetchUser = async () => {
    try {
      const resp = (await apiClient.get("/api/users/profile")) as any;
      const userData: UserProfile = resp.data;
      setUser(userData);
    } catch (error) {}
  };

    useEffect(() => {
    fetchUser();
  }, []);
  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-10">
      <ProfileHeader user={user} onUserChange={fetchUser} />
      <ProfileContent user={user}  />
    </div>
  );
}