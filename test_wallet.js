const mongoose = require('mongoose');
require('dotenv').config();
const MONGODB_URI = "mongodb+srv://pdhanalakshmi357_db_user:UC0grZ88PKkrYmGr@cluster0.rs39etx.mongodb.net/?appName=Cluster0";

mongoose.connect(MONGODB_URI).then(async () => {
    const db = mongoose.connection.db;
    const users = db.collection('users');
    await users.updateMany(
        { phone: { $in: ['9000000000', '919000000000', '+919000000000'] } },
        { $set: { walletBalance: 100000 } }
    );
    console.log('Updated wallet balance remotely!');
    process.exit(0);
}).catch(console.error);
