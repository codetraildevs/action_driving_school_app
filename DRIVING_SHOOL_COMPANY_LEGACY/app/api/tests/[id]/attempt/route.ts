import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { PrismaClient } from '@/lib/generated/prisma';
import { verifyToken } from '@/lib/auth/jwt';

import { prisma } from "@/lib/prismaDB";

interface Params {
  params: {
    id: string;
  };
}

export async function POST(request: NextRequest, { params }: Params) {
  try {
    const authHeader = request.headers.get('authorization');
     if (!authHeader || !authHeader.startsWith('Bearer ')) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Missing or malformed token' }, { status: 401 });
     }
     const token = authHeader.substring(7);
 
     const payload = await verifyToken(token);
     if (!payload || !payload.userId) {
       return NextResponse.json({ success: false, error: 'Unauthorized: Invalid or expired token' }, { status: 401 });
     }
const userId = payload.userId;
    const testId = parseInt(params.id);
    const body = await request.json();
    const { answers } = body; // Array of { questionId, optionId }

    // Verify test access and create attempt
    const result = await prisma.$transaction(async (tx) => {
      // Check user subscription
      const userSubscription = await tx.userSubscription.findUnique({
        where: { userId: userId }
      });

      const test = await tx.test.findUnique({
        where: { id: testId }
      });

      if (!test) {
        throw new Error('Test not found');
      }

      if (test.subscriptionId > (userSubscription?.subscriptionPlanId || 1)) {
        throw new Error('Subscription required');
      }

      // Create test attempt
      const attempt = await tx.testAttempt.create({
        data: {
          testId,
          userId: userId,
          startTime: new Date(),
          endTime: new Date(),
          status: 'submitted'
        }
      });

      // Record answers and calculate score
      let correctAnswers = 0;
      const answerPromises = answers.map(async (answer: any) => {
        const option = await tx.questionOption.findUnique({
          where: { id: answer.optionId },
          include: { question: true }
        });

        if (option && option.isCorrect) {
          correctAnswers++;
        }

        return tx.testAnswer.create({
          data: {
            attemptId: attempt.id,
            questionId: answer.questionId,
            optionId: answer.optionId
          }
        });
      });

      await Promise.all(answerPromises);

      // Calculate score
      const totalQuestions = await tx.testQuestion.count({
        where: { testId }
      });

      const score = (correctAnswers / totalQuestions) * test.totalMarks;
      const passed = score >= test.passMarks;

      // Update attempt with score
      const updatedAttempt = await tx.testAttempt.update({
        where: { id: attempt.id },
        data: {
          totalScore: score,
          passed
        }
      });

      // Create test result
      const testResult = await tx.testResult.create({
        data: {
          attemptId: attempt.id,
          userId: userId,
          testId,
          totalScore: score,
          passed
        }
      });

      // Log activity
      await tx.userActivity.create({
        data: {
          userId: userId,
          activityType: 'TEST_ATTEMPT',
          description: `Completed test: ${test.title} with score ${score.toFixed(1)}/${test.totalMarks}`
        }
      });

      return {
        attempt: updatedAttempt,
        result: testResult,
        score: score.toFixed(1),
        passed,
        correctAnswers,
        totalQuestions
      };
    });

    return NextResponse.json({ success: true, data: result });
  } catch (error) {
    console.error('Test attempt error:', error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : 'Internal server error' },
      { status: 500 }
    );
  }
}