import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prismaDB";
import { verifyToken } from "@/lib/auth/jwt";

// DELETE user (soft delete)
export async function DELETE(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Missing or malformed token" },
        { status: 401 }
      );
    }
    const token = authHeader.substring(7);

    const payload = await verifyToken(token);
    if (!payload || !payload.userId) {
      return NextResponse.json(
        { success: false, error: "Unauthorized: Invalid or expired token" },
        { status: 401 }
      );
    }

    const userId = payload.userId;

    // Log activity
    await prisma.$transaction(async (tx) => {
      await tx.user.delete({ where: { id: userId } });
 
    });

    return NextResponse.json({
      success: true,
      message: "User deleted successfully",
    });
  } catch (error) {
    console.error("Delete user error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
