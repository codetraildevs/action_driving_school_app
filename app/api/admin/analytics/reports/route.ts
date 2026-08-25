import { NextRequest, NextResponse } from 'next/server';
import { withPermission } from '@/lib/middleware/withPermission';
import { PERMISSIONS } from '@/lib/auth/permissions';
import { PrismaClient } from '@/lib/generated/prisma';

import { prisma } from "@/lib/prismaDB";

const getReportsHandler = withPermission(PERMISSIONS.REPORTS_VIEW)(
  async (req, { user }) => {
    try {
      const { searchParams } = new URL(req.url);
      const reportType = searchParams.get('type') || 'users';
      const startDate = searchParams.get('startDate');
      const endDate = searchParams.get('endDate');

      // Bound report queries even when no date range is supplied (a bare
      // call used to dump the whole table). Defaults to the last 90 days.
      let dateFilter;
      if (startDate && endDate) {
        dateFilter = {
          createdAt: {
            gte: new Date(startDate),
            lte: new Date(endDate),
          },
        };
      } else {
        dateFilter = {
          createdAt: {
            gte: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
          },
        };
      }

      let reportData;

      switch (reportType) {
        case 'users':
          reportData = await prisma.user.findMany({
            where: dateFilter,
            select: {
              id: true,
              firstName: true,
              lastName: true,
              email: true,
              phoneNumber: true,
              isActive: true,
              createdAt: true,
              lastLogin: true,
              role: {
                select: { roleName: true }
              },
              userSubscription: {
                select: {
                  subscriptionPlan: {
                    select: { planName: true }
                  }
                }
              },
              _count: {
                select: {
                  testAttempts: true,
                  readingSessions: true,
                }
              }
            },
            orderBy: { createdAt: 'desc' }
          });
          break;

        case 'subscriptions':
          reportData = await prisma.transaction.findMany({
            where: dateFilter,
            include: {
              user: {
                select: {
                  id: true,
                  firstName: true,
                  lastName: true,
                  email: true,
                }
              },
              subscription: {
                select: {
                  planName: true,
                  amount: true,
                }
              }
            },
            orderBy: { createdAt: 'desc' }
          });
          break;

        case 'tests':
          reportData = await prisma.testResult.findMany({
            where: dateFilter,
            include: {
              user: {
                select: {
                  id: true,
                  firstName: true,
                  lastName: true,
                  email: true,
                }
              },
              test: {
                select: {
                  title: true,
                  totalMarks: true,
                  passMarks: true,
                }
              }
            },
            orderBy: { createdAt: 'desc' }
          });
          break;

        case 'pdfs':
          reportData = await prisma.pdfFile.findMany({
            where: dateFilter,
            select: {
              id: true,
              title: true,
              author: true,
              uploadedAt: true,
              isPublic: true,
              uploader: {
                select: {
                  firstName: true,
                  lastName: true,
                }
              },
              _count: {
                select: {
                  readingSessions: true,
                  questions: true,
                  bookmarks: true,
                  ratings: true,
                }
              }
            },
            orderBy: { uploadedAt: 'desc' }
          });
          break;

        default:
          return NextResponse.json(
            { error: 'Invalid report type' },
            { status: 400 }
          );
      }

      return NextResponse.json({
        success: true,
        data: {
          reportType,
          dateRange: { startDate, endDate },
          results: reportData,
          count: reportData.length,
        }
      });
    } catch (error) {
      console.error('Generate report error:', error);
      return NextResponse.json(
        { error: 'Internal server error' },
        { status: 500 }
      );
    }
  }
);

export const GET = getReportsHandler;
