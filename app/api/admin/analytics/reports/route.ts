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

      // Pagination: reports used to dump every matching row in one response
      // (the users report returned thousands). Clamp hard so a report can
      // never become an unbounded table dump.
      const page = Math.max(1, parseInt(searchParams.get('page') || '1', 10) || 1);
      const pageSize = Math.min(
        200,
        Math.max(5, parseInt(searchParams.get('pageSize') || '25', 10) || 25),
      );
      const skip = (page - 1) * pageSize;

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
      let reportTotal = 0;

      switch (reportType) {
        case 'users': {
          const [rows, total] = await Promise.all([
            prisma.user.findMany({
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
              orderBy: { createdAt: 'desc' },
              skip,
              take: pageSize,
            }),
            prisma.user.count({ where: dateFilter }),
          ]);
          reportData = rows;
          reportTotal = total;
          break;
        }

        case 'subscriptions': {
          const [rows, total] = await Promise.all([
            prisma.transaction.findMany({
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
              orderBy: { createdAt: 'desc' },
              skip,
              take: pageSize,
            }),
            prisma.transaction.count({ where: dateFilter }),
          ]);
          reportData = rows;
          reportTotal = total;
          break;
        }

        case 'tests': {
          const [rows, total] = await Promise.all([
            prisma.testResult.findMany({
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
              orderBy: { createdAt: 'desc' },
              skip,
              take: pageSize,
            }),
            prisma.testResult.count({ where: dateFilter }),
          ]);
          reportData = rows;
          reportTotal = total;
          break;
        }

        case 'requests': {
          const [rows, total] = await Promise.all([
            prisma.userSubscriptionRequest.findMany({
              where: dateFilter,
              select: {
                id: true,
                userId: true,
                requestedTests: true,
                requestedDays: true,
                requestedExpiresAt: true,
                status: true,
                createdAt: true,
                user: {
                  select: {
                    id: true,
                    firstName: true,
                    lastName: true,
                    phoneNumber: true,
                    email: true,
                  }
                },
              },
              orderBy: { createdAt: 'desc' },
              skip,
              take: pageSize,
            }),
            prisma.userSubscriptionRequest.count({ where: dateFilter }),
          ]);
          reportData = rows;
          reportTotal = total;
          break;
        }

        case 'pdfs': {
          const [rows, total] = await Promise.all([
            prisma.pdfFile.findMany({
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
                    // NOTE: 'questions' used to be selected here, but PdfFile
                    // has no questions relation — Prisma rejected the select
                    // at runtime and the whole PDF report 500'd.
                    bookmarks: true,
                    ratings: true,
                  }
                }
              },
              orderBy: { uploadedAt: 'desc' },
              skip,
              take: pageSize,
            }),
            prisma.pdfFile.count({ where: dateFilter }),
          ]);
          reportData = rows;
          reportTotal = total;
          break;
        }

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
          total: reportTotal,
          pagination: {
            page,
            pageSize,
            total: reportTotal,
            pages: Math.ceil(reportTotal / pageSize) || 1,
          },
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
