import { NextResponse } from "next/server";
import { withPermission } from "@/lib/middleware/withPermission";
import { PERMISSIONS } from "@/lib/auth/permissions";
import { prisma } from "@/lib/prismaDB";

const translateQuestionHandler = withPermission(PERMISSIONS.TEST_UPDATE)(
  async (
    req: Request,
    { params }: { params: { id: string } }
  ) => {
  try {
    const { questionTranslations, optionTranslations } = await req.json();

    if (!Array.isArray(questionTranslations) || !Array.isArray(optionTranslations)) {
      return NextResponse.json(
        { success: false, error: "Invalid translation data format" },
        { status: 400 }
      );
    }

    const questionId = Number(params.id);

    // Verify question exists
    const question = await prisma.question.findUnique({
      where: { id: questionId },
    });

    if (!question) {
      return NextResponse.json(
        { success: false, error: "Question not found" },
        { status: 404 }
      );
    }

    // Process translations in a transaction
    const translatedQuestion = await prisma.$transaction(
      async (tx) => {
        // Handle question translations
        for (const qt of questionTranslations) {
          if (qt.questionText && qt.questionText.trim() !== "") {
            await tx.questionTranslation.upsert({
              where: {
                questionId_languageId: {
                  questionId,
                  languageId: qt.languageId,
                },
              },
              create: {
                questionId,
                languageId: qt.languageId,
                questionText: qt.questionText,
                imageUrl: qt.imageUrl || null,
              },
              update: {
                questionText: qt.questionText,
                imageUrl: qt.imageUrl || null,
              },
            });
          }
        }

        // Handle option translations
        for (const optionTranslation of optionTranslations) {
          const { optionId, translations } = optionTranslation;

          if (!optionId || !Array.isArray(translations)) continue;

          for (const translation of translations) {
            if (translation.text && translation.text.trim() !== "") {
              await tx.questionOptionTranslation.upsert({
                where: {
                  optionId_languageId: {
                    optionId: optionId,
                    languageId: translation.languageId,
                  },
                },
                create: {
                  optionId: optionId,
                  languageId: translation.languageId,
                  text: translation.text,
                },
                update: {
                  text: translation.text,
                },
              });
            }
          }
        }

        // Fetch and return updated question with all translations
        const updatedQuestion = await tx.question.findUnique({
          where: { id: questionId },
          include: {
            options: {
              include: {
                questionOptionTranslations: true,
              },
            },
            questionTranslations: true,
            testQuestions: {
              include: {
                test: {
                  select: {
                    id: true,
                    title: true,
                  },
                },
              },
            },
          },
        });

        return updatedQuestion;
      },
      { maxWait: 60000, timeout: 60000 }
    );

    return NextResponse.json(
      { success: true, data: translatedQuestion },
      { status: 200 }
    );
  } catch (error) {
    console.error("PUT translate error:", error);
    return NextResponse.json(
      {
        success: false,
        error: "Failed to update translations",
        details: error instanceof Error ? error.message : "Unknown error",
      },
      { status: 500 }
    );
  }
  }
);

export const PUT = translateQuestionHandler;