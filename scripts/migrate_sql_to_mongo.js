const fs = require('fs');
const readline = require('readline');
const path = require('path');
const mongoose = require('mongoose');

// Determine which URI to use
const args = process.argv.slice(2);
const useDevDb = args.includes('--dev');

// Load environment variables if not dev
if (!useDevDb) {
  require('dotenv').config();
}

const MONGO_URI = useDevDb 
  ? 'mongodb://localhost:27017/astroeleven_dev'
  : (process.env.MONGODB_URI || 'mongodb://localhost:27017/astroeleven');

const sqlFilePath = path.join(__dirname, '../u453854592_astro.sql');

// Load Mongoose models
const User = require('../models/User');
const CallRequest = require('../models/CallRequest');
const Payment = require('../models/Payment');
const Withdrawal = require('../models/Withdrawal');
const Banner = require('../models/Banner');
const Feedback = require('../models/Feedback');

// Helper function to parse SQL values from line
function parseSqlValues(valuesStr) {
  const result = [];
  let currentToken = '';
  let inString = false;
  let stringChar = null;
  let isEscaped = false;

  for (let i = 0; i < valuesStr.length; i++) {
    const char = valuesStr[i];

    if (isEscaped) {
      currentToken += char;
      isEscaped = false;
      continue;
    }

    if (char === '\\') {
      isEscaped = true;
      continue;
    }

    if (inString) {
      if (char === stringChar) {
        inString = false;
        stringChar = null;
      } else {
        currentToken += char;
      }
    } else {
      if (char === "'" || char === '"') {
        inString = true;
        stringChar = char;
      } else if (char === ',') {
        result.push(convertToken(currentToken));
        currentToken = '';
      } else {
        currentToken += char;
      }
    }
  }
  result.push(convertToken(currentToken));
  return result;
}

function convertToken(token) {
  token = token.trim();
  if (token.toUpperCase() === 'NULL') {
    return null;
  }
  if ((token.startsWith("'") && token.endsWith("'")) || (token.startsWith('"') && token.endsWith('"'))) {
    return token.slice(1, -1);
  }
  if (!isNaN(token) && token !== '') {
    return Number(token);
  }
  return token;
}

async function migrate() {
  console.log('📖 Starting SQL parser...');
  console.log('Reading from:', sqlFilePath);

  const fileStream = fs.createReadStream(sqlFilePath);
  const rl = readline.createInterface({
    input: fileStream,
    crlfDelay: Infinity
  });

  // Maps to store SQL data in memory
  const usersMap = new Map();         // id -> userObj
  const astrologersMap = new Map();   // userId -> astrologerObj
  const walletsMap = new Map();       // userId -> amount
  
  // Arrays for other tables
  const callRequests = [];
  const payments = [];
  const withdrawals = [];
  const banners = [];
  const feedbacks = [];

  let currentTable = null;
  let currentCols = [];

  for await (const line of rl) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    // Detect INSERT INTO statement
    if (trimmed.startsWith('INSERT INTO')) {
      const match = trimmed.match(/INSERT INTO\s+`([^`]+)`\s*\(([^)]+)\)\s*VALUES/i);
      if (match) {
        currentTable = match[1];
        currentCols = match[2].split(',').map(c => c.replace(/`/g, '').trim());
      }
      continue;
    }

    // Process data rows
    if (currentTable && trimmed.startsWith('(')) {
      // Find where values row ends (either with '),' or ');')
      const endsWithComma = trimmed.endsWith('),');
      const endsWithSemicolon = trimmed.endsWith(');');
      
      if (endsWithComma || endsWithSemicolon) {
        const valuesStr = trimmed.slice(1, -2);
        try {
          const parsedValues = parseSqlValues(valuesStr);
          
          // Construct row object
          const row = {};
          currentCols.forEach((col, idx) => {
            row[col] = parsedValues[idx];
          });

          // Store in corresponding data structures
          if (currentTable === 'users') {
            usersMap.set(row.id, row);
          } else if (currentTable === 'astrologers') {
            astrologersMap.set(row.userId, row);
          } else if (currentTable === 'user_wallets') {
            walletsMap.set(row.userId, row.amount);
          } else if (currentTable === 'callrequest') {
            callRequests.push(row);
          } else if (currentTable === 'payment') {
            payments.push(row);
          } else if (currentTable === 'withdrawrequest') {
            withdrawals.push(row);
          } else if (currentTable === 'banners') {
            banners.push(row);
          } else if (currentTable === 'user_reviews') {
            feedbacks.push(row);
          }
        } catch (e) {
          console.error(`Failed to parse line in table ${currentTable}: ${e.message}`);
        }

        if (endsWithSemicolon) {
          currentTable = null;
        }
      }
    }
  }

  console.log('\n📊 SQL Parsing Complete!');
  console.log(`- Users in SQL: ${usersMap.size}`);
  console.log(`- Astrologers in SQL: ${astrologersMap.size}`);
  console.log(`- Wallets in SQL: ${walletsMap.size}`);
  console.log(`- Call Requests: ${callRequests.length}`);
  console.log(`- Payments: ${payments.length}`);
  console.log(`- Withdrawals: ${withdrawals.length}`);
  console.log(`- Banners: ${banners.length}`);
  console.log(`- Feedbacks: ${feedbacks.length}`);

  // Connect to MongoDB
  console.log(`\n📡 Connecting to MongoDB: ${MONGO_URI.split('@').pop().split('?')[0]}`);
  await mongoose.connect(MONGO_URI);
  console.log('✅ Connected to MongoDB!');

  // Clear existing collections
  console.log('\n🧹 Cleaning existing collections...');
  await User.deleteMany({});
  await CallRequest.deleteMany({});
  await Payment.deleteMany({});
  await Withdrawal.deleteMany({});
  await Banner.deleteMany({});
  await Feedback.deleteMany({});
  console.log('✅ Collections cleaned!');

  // 1. Process & Insert Users
  console.log('\n👤 Processing and importing Users & Astrologers...');
  const mongoUsers = [];
  const seenPhones = new Set();
  let skippedDuplicates = 0;
  let skippedNoPhone = 0;

  for (const [id, user] of usersMap.entries()) {
    let phone = user.contactNo ? user.contactNo.toString().trim() : '';
    
    if (!phone) {
      skippedNoPhone++;
      continue;
    }

    // Filter duplicates for MongoDB unique constraints
    if (seenPhones.has(phone)) {
      skippedDuplicates++;
      continue;
    }
    seenPhones.add(phone);

    const astro = astrologersMap.get(id);
    const walletBalance = walletsMap.get(id) || 0;

    const userDoc = {
      userId: id.toString(),
      phone: phone,
      name: user.name || 'User_' + id,
      email: user.email || '',
      gender: user.gender || '',
      dob: user.birthDate ? user.birthDate.toString().split(' ')[0] : '',
      tob: user.birthTime || '',
      pob: user.birthPlace || '',
      fcmToken: user.fcm_token || '',
      walletBalance: Number(walletBalance),
      isBanned: user.isActive === 0,
      createdAt: user.created_at ? new Date(user.created_at) : new Date()
    };

    if (astro) {
      userDoc.role = 'astrologer';
      userDoc.realName = astro.name || user.name;
      userDoc.isOnline = astro.chatStatus === 'online' || astro.callStatus === 'online';
      userDoc.isChatOnline = astro.chatStatus === 'online';
      userDoc.isAudioOnline = astro.callStatus === 'online';
      userDoc.skills = (astro.allSkill && typeof astro.allSkill === 'string') ? astro.allSkill.split(',').map(s => s.trim()) : [];
      userDoc.price = Number(astro.charge) || 20;
      userDoc.ratePerMinute = Number(astro.charge) || 10;
      userDoc.experience = Number(astro.experienceInYears) || 0;
      userDoc.isVerified = astro.isVerified === 1;
      userDoc.approvalStatus = astro.isVerified === 1 ? 'approved' : 'pending';
      userDoc.image = astro.profileImage || '';
    } else {
      userDoc.role = 'client';
    }

    mongoUsers.push(userDoc);
  }

  // Batch insert users
  if (mongoUsers.length > 0) {
    const batchSize = 500;
    for (let i = 0; i < mongoUsers.length; i += batchSize) {
      const batch = mongoUsers.slice(i, i + batchSize);
      await User.insertMany(batch);
    }
  }
  console.log(`✅ Users imported successfully! Total: ${mongoUsers.length} (Skipped no phone: ${skippedNoPhone}, Skipped duplicate phone: ${skippedDuplicates})`);

  // 2. Process & Insert Call Requests
  console.log('\n📞 Importing Call Requests...');
  const mongoCallRequests = callRequests.map(call => {
    let status = 'initiated';
    if (call.callStatus === 'success' || call.callStatus === 'completed') status = 'accepted';
    else if (call.callStatus === 'rejected') status = 'rejected';
    else if (call.callStatus === 'missed') status = 'missed';

    return {
      callId: call.id.toString(),
      callerId: call.userId ? call.userId.toString() : '',
      receiverId: call.astrologerId ? call.astrologerId.toString() : '',
      status: status,
      createdAt: call.created_at ? new Date(call.created_at) : new Date()
    };
  }).filter(c => c.callerId && c.receiverId);

  if (mongoCallRequests.length > 0) {
    const batchSize = 500;
    for (let i = 0; i < mongoCallRequests.length; i += batchSize) {
      const batch = mongoCallRequests.slice(i, i + batchSize);
      await CallRequest.insertMany(batch);
    }
  }
  console.log(`✅ Call Requests imported: ${mongoCallRequests.length}`);

  // 3. Process & Insert Payments
  console.log('\n💳 Importing Payments...');
  const mongoPayments = payments.map(pay => {
    let status = 'pending';
    if (pay.paymentStatus === 'success' || pay.paymentStatus === 'completed') status = 'success';
    else if (pay.paymentStatus === 'failed') status = 'failed';

    return {
      transactionId: pay.id.toString(),
      merchantTransactionId: pay.paymentReference || '',
      userId: pay.userId ? pay.userId.toString() : '',
      amount: Number(pay.amount) || 0,
      baseAmount: Number(pay.amount) || 0,
      gstAmount: 0,
      withGst: false,
      status: status,
      createdAt: pay.created_at ? new Date(pay.created_at) : new Date(),
      providerRefId: pay.signature || '',
      couponBonus: Number(pay.cashback_amount) || 0,
      reason: 'recharge'
    };
  }).filter(p => p.userId);

  if (mongoPayments.length > 0) {
    const batchSize = 500;
    for (let i = 0; i < mongoPayments.length; i += batchSize) {
      const batch = mongoPayments.slice(i, i + batchSize);
      await Payment.insertMany(batch);
    }
  }
  console.log(`✅ Payments imported: ${mongoPayments.length}`);

  // 4. Process & Insert Withdrawals
  console.log('\n💸 Importing Withdrawals...');
  const mongoWithdrawals = withdrawals.map(w => {
    let status = 'pending';
    if (w.status === 'success' || w.status === 'completed' || w.status === 'approved') status = 'approved';
    else if (w.status === 'rejected') status = 'rejected';

    return {
      astroId: w.astrologerId ? w.astrologerId.toString() : '',
      amount: Number(w.withdrawAmount) || 0,
      status: status,
      requestedAt: w.created_at ? new Date(w.created_at) : new Date(),
      processedAt: w.updated_at ? new Date(w.updated_at) : null
    };
  }).filter(w => w.astroId);

  if (mongoWithdrawals.length > 0) {
    await Withdrawal.insertMany(mongoWithdrawals);
  }
  console.log(`✅ Withdrawals imported: ${mongoWithdrawals.length}`);

  // 5. Process & Insert Banners
  console.log('\n🖼️ Importing Banners...');
  const mongoBanners = banners.map(b => {
    return {
      imageUrl: b.bannerImage || '',
      title: b.title || 'Special Promotion',
      subtitle: b.description || '',
      ctaText: 'Learn More',
      isActive: b.isActive === 1,
      createdAt: b.created_at ? new Date(b.created_at) : new Date()
    };
  }).filter(b => b.imageUrl);

  if (mongoBanners.length > 0) {
    await Banner.insertMany(mongoBanners);
  }
  console.log(`✅ Banners imported: ${mongoBanners.length}`);

  // 6. Process & Insert Feedbacks (Reviews)
  console.log('\n💬 Importing Feedbacks...');
  const mongoFeedbacks = feedbacks.map(f => {
    const user = usersMap.get(f.userId);
    const astro = usersMap.get(f.astrologerId); // astrologer userId maps to user.id
    
    return {
      userId: f.userId ? f.userId.toString() : '',
      userName: user ? (user.name || 'User') : 'User',
      astrologerId: f.astrologerId ? f.astrologerId.toString() : '',
      astrologerName: astro ? (astro.name || 'Astrologer') : 'Astrologer',
      rating: Number(f.rating) || 5,
      comment: f.review || '',
      sessionType: f.astromallProductId ? 'product' : 'chat',
      createdAt: f.created_at ? new Date(f.created_at) : new Date()
    };
  }).filter(f => f.userId && f.comment);

  if (mongoFeedbacks.length > 0) {
    const batchSize = 500;
    for (let i = 0; i < mongoFeedbacks.length; i += batchSize) {
      const batch = mongoFeedbacks.slice(i, i + batchSize);
      await Feedback.insertMany(batch);
    }
  }
  console.log(`✅ Feedbacks imported: ${mongoFeedbacks.length}`);

  // Close MongoDB connection
  await mongoose.connection.close();
  console.log('\n🎉 MIGRATION COMPLETED SUCCESSFULLY!');
}

migrate().catch(err => {
  console.error('\n❌ MIGRATION FAILED:', err);
  mongoose.connection.close();
});
