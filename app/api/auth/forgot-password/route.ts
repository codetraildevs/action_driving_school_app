import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
 ;
import { generateResetToken } from "@/lib/auth/jwt";
import { sendPasswordResetEmail } from "@/lib/email"; 

import { prisma } from "@/lib/prismaDB";

const forgotPasswordSchema = z.object({
  email: z.string().email(),
});

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { email } = forgotPasswordSchema.parse(body);

    
    const user = await prisma.user.findUnique({
      where: { email },
    });

    
    if (!user) {
      return NextResponse.json(
        {
          success: true,
          message: "If an account with that email exists, a password reset link has been sent.",
        },
        { status: 200 }
      );
    }

    
    if (!user.isActive) {
      return NextResponse.json(
        {
          success: true,
          message: "If an account with that email exists, a password reset link has been sent.",
        },
        { status: 200 }
      );
    }

    const resetToken = generateResetToken({
      userId: user.id,
      email: user.email,
    });

 
    const currentRequest= await prisma.forgetPasswordRequest.findFirst({where:{email}})
    if (currentRequest && currentRequest.active){
        return NextResponse.json(
        {
          success: false,
          error: "Invalid Request",
          details:  "Unknown error occured",
        },
        { status: 400 }
      );
    }
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000);
    if(currentRequest)
      await prisma.forgetPasswordRequest.delete({where:{id:currentRequest?.id}})
    await prisma.forgetPasswordRequest.create({data:{
      email:email,
      token:resetToken,
      expiresAt
    }})
     

 
    await sendPasswordResetEmail(user.email,  resetToken, user.firstName);
    
 
    console.log("Password reset URL:",  resetToken);

    return NextResponse.json(
      {
        success: true,
        message: "If an account with that email exists, a password reset link has been sent.",
       
      },
      { status: 200 }
    );
  } catch (error) {
    if (error instanceof z.ZodError) {
      return NextResponse.json(
        {
          success: false,
          error: "Invalid input",
          details: error.issues,
        },
        { status: 400 }
      );
    }

    console.error("Forgot password error:", error);
    return NextResponse.json(
      {
        success: false,
        error: "Internal server error",
      },
      { status: 500 }
    );
  }
}
