const { PrismaClient } = require('./lib/generated/prisma');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  try {
    // Find user
    const user = await prisma.user.findFirst({
      where: { phoneNumber: '0732657995' },
      include: { role: true }
    });
    
    if (!user) {
      console.log('User not found');
      return;
    }
    
    console.log('User found:', {
      id: user.id,
      phone: user.phoneNumber,
      firstName: user.firstName,
      role: user.role?.roleName,
      passwordHash: user.password?.substring(0, 20) + '...',
      isActive: user.isActive
    });
    
    // Test password
    const valid = await bcrypt.compare('Password123', user.password);
    console.log('Password valid:', valid);
    
  } catch (error) {
    console.error('Error:', error.message);
  } finally {
    await prisma.$disconnect();
  }
}

main();
